package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.dto.AuthComplianceReport;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCompliance;
import com.csg.prm.authorize.entity.AuthMaterial;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.mapper.AuthComplianceMapper;
import com.csg.prm.authorize.mapper.AuthMaterialMapper;
import com.csg.prm.authorize.service.AuthComplianceService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthComplianceServiceImpl implements AuthComplianceService {

    private final AuthComplianceMapper mapper;
    private final AuthApplyMapper applyMapper;
    private final AuthMaterialMapper materialMapper;
    private final com.csg.prm.authorize.service.AuthApplyService applyService;

    private final com.csg.prm.common.ai.DawatAiGateway ai;
    private final com.csg.prm.common.aitrace.AiRunLogService aiRunLogService;

    public AuthComplianceServiceImpl(AuthComplianceMapper mapper, AuthApplyMapper applyMapper,
                                     AuthMaterialMapper materialMapper,
                                     com.csg.prm.authorize.service.AuthApplyService applyService,
                                     com.csg.prm.common.ai.DawatAiGateway ai,
                                     com.csg.prm.common.aitrace.AiRunLogService aiRunLogService) {
        this.mapper = mapper;
        this.applyMapper = applyMapper;
        this.materialMapper = materialMapper;
        this.applyService = applyService;
        this.ai = ai;
        this.aiRunLogService = aiRunLogService;
    }

    /** 合规 AI 预审:先跑规则三维校验,再交大模型生成补充预审意见(qwen3-max,stub 回退) */
    @Override
    public String preReview(String applyId) {
        AuthComplianceReport report = runCheck(applyId);
        com.csg.prm.authorize.entity.AuthApply apply = applyMapper.selectById(applyId);
        StringBuilder ctx = new StringBuilder("【申请】资产:").append(apply.getAssetName())
                .append(";被授权方:").append(apply.getGranteeOrg()).append(";权益:").append(apply.getRightType())
                .append(";场景:").append(apply.getScenario())
                .append(";隐私/商密:").append(apply.getSensitiveType() == null ? "无" : apply.getSensitiveType())
                .append("\n【规则校验】结果:").append(report.getCheckResult())
                .append(";风险:").append(report.getRiskLevel()).append('\n');
        for (AuthComplianceReport.Item it : report.getItems()) {
            ctx.append(it.getDimension()).append('/').append(it.getItem()).append(':')
                    .append(it.isPass() ? "通过" : "不符").append('(').append(it.getMessage()).append(")\n");
        }
        long t0 = System.currentTimeMillis();
        String opinion = ai.preReviewAuth(ctx.toString());
        // 逐次留痕(南网全流程留痕追溯):合规 AI 预审 模型/输入摘要/输出/耗时/SM3/触发人
        aiRunLogService.record(com.csg.prm.common.aitrace.AiRunLog.BIZ_AUTHORIZE, applyId,
                com.csg.prm.common.aitrace.AiRunLog.CAP_AUTH_PRECHECK, ai.modelName(),
                "资产:" + apply.getAssetName() + ";规则结果:" + report.getCheckResult() + ";风险:" + report.getRiskLevel(),
                opinion, System.currentTimeMillis() - t0);
        return opinion == null ? "AI 预审暂不可用" : opinion;
    }

    @Override
    @Transactional
    public AuthComplianceReport runCheck(String applyId) {
        if (!StringUtils.hasText(applyId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "申请ID不能为空");
        }
        AuthApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "授权申请不存在");
        }
        long matCount = materialMapper.selectCount(new LambdaQueryWrapper<AuthMaterial>()
                .eq(AuthMaterial::getApplyId, applyId));

        AuthComplianceReport report = new AuthComplianceReport();
        report.setApplyId(applyId);
        boolean hasFail = false;
        boolean hasWarn = false;

        // ① 材料完整性
        boolean matOk = matCount >= 1;
        report.add("材料完整性", "申请材料", matOk, matOk ? "已上传 " + matCount + " 份材料" : "未上传任何申请材料");
        hasFail |= !matOk;

        // ② 权限合理性
        boolean cardOk = StringUtils.hasText(apply.getEquityCardId());
        report.add("权限合理性", "先确后授(引用权益卡片)", cardOk, cardOk ? "已引用权益卡片" : "未引用权益卡片,违反先确后授");
        hasFail |= !cardOk;

        boolean rtOk = StringUtils.hasText(apply.getRightType());
        report.add("权限合理性", "授权权益类型", rtOk, rtOk ? apply.getRightType() : "未指定授权权益类型");
        hasFail |= !rtOk;

        boolean scopeOk = StringUtils.hasText(apply.getScope());
        report.add("权限合理性", "授权范围", scopeOk, scopeOk ? "已填写" : "授权范围未填写,建议明确");
        hasWarn |= !scopeOk;

        // 提交硬门禁(与 submit 同源):先确后授 isUsable / 经营权仅限开放目录 / 授权范围·期限 ⊆ 确权边界
        // —— 这些过去只在 submit 时查,合规校验若不含会出现"合规过了却提交被拒"的死路,故并入合规报告
        String blockReason = applyService.submitBlockReason(applyId);
        boolean submitOk = blockReason == null;
        report.add("权限合理性", "确权边界与先确后授(提交硬门禁)", submitOk,
                submitOk ? "符合先确后授·经营权目录·范围/期限⊆确权边界" : blockReason);
        hasFail |= !submitOk;

        // ③ 合规性
        if (StringUtils.hasText(apply.getThirdPartySource())) {
            boolean lic = StringUtils.hasText(apply.getThirdPartyLicense());
            report.add("合规性", "第三方许可凭证", lic,
                    lic ? "已提供" : "涉第三方来源(" + apply.getThirdPartySource() + ")未提供许可凭证");
            hasFail |= !lic;
        } else {
            report.add("合规性", "第三方许可凭证", true, "不涉及第三方");
        }

        boolean sensitive = StringUtils.hasText(apply.getSensitiveType()) && !"无".equals(apply.getSensitiveType());
        if (sensitive) {
            boolean conf = Boolean.TRUE.equals(apply.getNeedConfidentiality())
                    || StringUtils.hasText(apply.getConfidentialityFile())
                    || StringUtils.hasText(apply.getInfoAuthAgreement());
            report.add("合规性", "敏感数据保护", conf,
                    conf ? "已含保密承诺/信息授权协议" : "涉敏感(" + apply.getSensitiveType() + ")建议补充保密承诺函/信息授权协议");
            hasWarn |= !conf;
        } else {
            report.add("合规性", "敏感数据保护", true, "不涉及敏感数据");
        }

        if (Boolean.TRUE.equals(apply.getCrossRegion())) {
            report.add("合规性", "跨区域授权", false, "跨区域/跨域授权需额外合规审查与备案");
            hasWarn = true;
        } else {
            report.add("合规性", "跨区域授权", true, "非跨域");
        }

        String level = hasFail ? AuthCompliance.LEVEL_RED : hasWarn ? AuthCompliance.LEVEL_YELLOW : AuthCompliance.LEVEL_GREEN;
        String result = hasFail ? "不通过" : hasWarn ? "警告" : "通过";
        String problems = report.getItems().stream().filter(i -> !i.isPass())
                .map(i -> i.getItem() + ":" + i.getMessage()).collect(Collectors.joining("; "));

        AuthCompliance c = new AuthCompliance();
        c.setApplyId(applyId);
        c.setRiskLevel(level);
        c.setCheckResult(result);
        c.setProblemDesc(StringUtils.hasText(problems) ? problems : "无");
        c.setCheckReport(toJson(report));
        c.setCheckTime(LocalDateTime.now());
        mapper.insert(c);

        report.setCheckId(c.getCheckId());
        report.setRiskLevel(level);
        report.setCheckResult(result);
        report.setProblemDesc(c.getProblemDesc());
        return report;
    }

    @Override
    public byte[] exportRecords(String applyId, String riskLevel) {
        List<AuthCompliance> list = mapper.selectList(buildWrapper(applyId, riskLevel)
                .orderByDesc(AuthCompliance::getCheckTime));
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("申请ID,风险等级,校验结果,问题描述,校验时间\n");
        for (AuthCompliance c : list) {
            sb.append(csv(c.getApplyId())).append(',')
                    .append(csv(c.getRiskLevel())).append(',')
                    .append(csv(c.getCheckResult())).append(',')
                    .append(csv(c.getProblemDesc())).append(',')
                    .append(csv(String.valueOf(c.getCheckTime()))).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public PageResult<AuthCompliance> page(long current, long size, String applyId, String riskLevel) {
        IPage<AuthCompliance> p = mapper.selectPage(
                new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size),
                buildWrapper(applyId, riskLevel).orderByDesc(AuthCompliance::getCheckTime));
        return PageResult.of(p);
    }

    private LambdaQueryWrapper<AuthCompliance> buildWrapper(String applyId, String riskLevel) {
        LambdaQueryWrapper<AuthCompliance> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(applyId), AuthCompliance::getApplyId, applyId)
                .eq(StringUtils.hasText(riskLevel), AuthCompliance::getRiskLevel, riskLevel);
        return w;
    }

    private String toJson(AuthComplianceReport r) {
        return "[" + r.getItems().stream().map(i -> "{\"dimension\":\"" + esc(i.getDimension())
                + "\",\"item\":\"" + esc(i.getItem())
                + "\",\"pass\":" + i.isPass()
                + ",\"message\":\"" + esc(i.getMessage()) + "\"}").collect(Collectors.joining(",")) + "]";
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String csv(String s) {
        if (s == null || "null".equals(s)) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
