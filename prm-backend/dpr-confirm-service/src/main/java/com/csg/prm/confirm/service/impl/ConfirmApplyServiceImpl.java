package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.workflow.FlowDefinitions;
import com.csg.prm.common.workflow.FlowTransition;
import com.csg.prm.common.workflow.ProcessFlowEngine;
import com.csg.prm.confirm.dto.ConfirmApplyQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.gateway.MetadataGateway;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmFlowLogService;
import com.csg.prm.confirm.service.ConfirmSummaryService;
import com.csg.prm.confirm.service.EquityCardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConfirmApplyServiceImpl implements ConfirmApplyService {

    /** 元数据质量门禁阈值:<80 自动驳回确权(需求§5.5 接口③) */
    private static final int QUALITY_THRESHOLD = 80;

    /** 逐节点角色门禁:每个审批节点仅对应角色(及 all/admin)可处理(审批/驳回)。 */
    private static final Map<String, String> NODE_ROLE = Map.of(
            ConfirmApply.STATUS_PRECHECK, "precheck",   // 40 人工预审 -> 人工预审员
            ConfirmApply.STATUS_COMPLIANCE, "review",   // 50 合规审核 -> 合规管控小组
            ConfirmApply.STATUS_MANAGER, "manager",     // 60 主管复核 -> 数字化部主管
            ConfirmApply.STATUS_DIRECTOR, "director");  // 70 经理终审 -> 经理/高级经理
    private static final Map<String, String> ROLE_LABEL = Map.of(
            "precheck", "人工预审员", "review", "合规管控小组", "manager", "数字化部主管", "director", "经理/高级经理");

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfirmApplyServiceImpl.class);

    private final ConfirmApplyMapper mapper;
    private final EquityCardService equityCardService;
    private final ConfirmSummaryService summaryService;
    private final MetadataGateway metadataGateway;
    private final ProcessFlowEngine flowEngine;
    private final ConfirmFlowLogService flowLogService;
    private final com.csg.prm.confirm.integration.AssetCardWritebackService cardWriteback;
    private final com.csg.prm.confirm.integration.AssetCardArchiveService cardArchive;

    public ConfirmApplyServiceImpl(ConfirmApplyMapper mapper, EquityCardService equityCardService,
                                   ConfirmSummaryService summaryService, MetadataGateway metadataGateway,
                                   ProcessFlowEngine flowEngine, ConfirmFlowLogService flowLogService,
                                   com.csg.prm.confirm.integration.AssetCardWritebackService cardWriteback,
                                   com.csg.prm.confirm.integration.AssetCardArchiveService cardArchive) {
        this.mapper = mapper;
        this.equityCardService = equityCardService;
        this.summaryService = summaryService;
        this.metadataGateway = metadataGateway;
        this.flowEngine = flowEngine;
        this.flowLogService = flowLogService;
        this.cardWriteback = cardWriteback;
        this.cardArchive = cardArchive;
    }

    /** 各审批状态对应的责任人(节点处理人/角色)。 */
    /**
     * 逐节点角色门禁:校验当前登录用户是否有权处理该节点(审批/驳回)。
     * 无用户上下文(内部调用/未启用认证/单测)或 all/admin 角色:放行;否则节点角色须匹配。
     */
    private void assertNodeRole(String status) {
        UserContext ctx = UserContextHolder.get();
        Set<String> roles = ctx == null ? null : ctx.getRoles();
        if (roles == null || roles.isEmpty() || roles.contains("all") || roles.contains("admin")) {
            return;
        }
        String need = NODE_ROLE.get(status);
        if (need != null && !roles.contains(need)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(),
                    "无权限:【" + status + "】节点须由「" + ROLE_LABEL.getOrDefault(need, need) + "」角色处理");
        }
    }

    private String responderOf(String status) {
        if (ConfirmApply.STATUS_PRECHECK.equals(status)) {
            return "人工预审员";
        }
        if (ConfirmApply.STATUS_COMPLIANCE.equals(status)) {
            return "合规管控小组";
        }
        if (ConfirmApply.STATUS_MANAGER.equals(status)) {
            return "数字化部主管";
        }
        if (ConfirmApply.STATUS_DIRECTOR.equals(status)) {
            return "确权经理";
        }
        return "申请人";
    }

    @Override
    public MetadataGateway.MetadataInfo autofill(String assetId) {
        return metadataGateway.autofill(assetId);
    }

    @Override
    public com.csg.prm.confirm.dto.RightsFactsVO rightsFacts(String assetId) {
        com.csg.prm.confirm.dto.RightsFactsVO vo = new com.csg.prm.confirm.dto.RightsFactsVO(assetId);
        if (!StringUtils.hasText(assetId)) {
            return vo;
        }
        // 取该资产最新「已完成」确权(制卡归集态),作为权益事实真源
        List<ConfirmApply> list = mapper.selectList(new LambdaQueryWrapper<ConfirmApply>()
                .eq(ConfirmApply::getAssetId, assetId)
                .eq(ConfirmApply::getStatus, ConfirmApply.STATUS_DONE)
                .orderByDesc(ConfirmApply::getCreateTime));
        if (list.isEmpty()) {
            return vo;
        }
        ConfirmApply a = list.get(0);
        vo.setConfirmed(true);
        // null 安全:复用既有 containsAny(不做 null 校验),入参先兜底为空串
        String source = a.getSourceIdentification() == null ? "" : a.getSourceIdentification();
        String relation = a.getRelationIdentification() == null ? "" : a.getRelationIdentification();
        // 第三方来源(表2 涉第三方,或表1 来源识别 B/C/D/E 非自行生产)→ 带出来源主体/说明
        boolean thirdParty = Boolean.TRUE.equals(a.getInvolvesThirdParty())
                || containsAny(source, "B", "C", "D", "E");
        if (thirdParty) {
            vo.setThirdPartySource(firstNonBlank(a.getSourceSubject(), a.getThirdPartyInfo(), "涉及第三方来源"));
        }
        // 隐私/商密(表1 信息关联识别 H个人隐私 / I第三方商密)
        StringBuilder sb = new StringBuilder();
        if (containsAny(relation, "H") || StringUtils.hasText(a.getPrivacyInfo())) {
            sb.append("个人隐私");
        }
        if (containsAny(relation, "I")) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append("商业秘密");
        }
        vo.setSensitiveType(sb.length() > 0 ? sb.toString() : "无");
        return vo;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (StringUtils.hasText(v)) {
                return v;
            }
        }
        return "";
    }

    @Override
    @Transactional
    public String saveDraft(ConfirmApply apply) {
        validate(apply);
        if (StringUtils.hasText(apply.getApplyId())) {
            ConfirmApply exist = require(apply.getApplyId());
            if (!ConfirmApply.STATUS_DRAFT.equals(exist.getStatus())) {
                throw new BizException("仅草稿状态可编辑");
            }
            mapper.updateById(apply);
            return apply.getApplyId();
        }
        apply.setStatus(ConfirmApply.STATUS_DRAFT);
        if (!StringUtils.hasText(apply.getApplyNo())) {
            apply.setApplyNo(generateApplyNo());
        }
        mapper.insert(apply);
        return apply.getApplyId();
    }

    @Override
    @Transactional
    public void delete(String applyId) {
        ConfirmApply apply = require(applyId);
        if (!ConfirmApply.STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BizException("仅草稿状态的确权申请可删除;已提交/审批中请走撤回或驳回");
        }
        mapper.deleteById(applyId);
    }

    @Override
    @Transactional
    public String createReConfirm(String assetId, String assetName, String rightType,
                                  String reason, String sourceRef, String changeTrigger) {
        if (!StringUtils.hasText(assetId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "关联资产ID不能为空");
        }
        ConfirmApply apply = new ConfirmApply();
        apply.setAssetId(assetId);
        apply.setAssetName(StringUtils.hasText(assetName) ? assetName : assetId);
        apply.setRightType(StringUtils.hasText(rightType) ? rightType : "数据持有权");
        apply.setPurpose(StringUtils.hasText(reason) ? reason : "监测联动派生重确权");
        apply.setReConfirm(Boolean.TRUE);
        // 重确权即"确权变更"登记类型(附录F 表1),并归类变更触发动因
        apply.setRegisterType("确权变更");
        apply.setChangeTrigger(StringUtils.hasText(changeTrigger) ? changeTrigger : "权益到期");
        apply.setSourceRef(sourceRef);
        // 基于现状预填:从前序当前有效权益卡片带出权属主体/权利类型(变更是对现状的修订,不空表重填)
        EquityCard prior = equityCardService.findCurrentValid(assetId, apply.getRightType());
        if (prior != null) {
            apply.setRightHolder(prior.getRightOwner());
            if (StringUtils.hasText(prior.getRightType())) {
                apply.setRightType(prior.getRightType());
            }
        }
        apply.setStatus(ConfirmApply.STATUS_DRAFT);
        apply.setApplyNo(generateApplyNo());
        mapper.insert(apply);
        return apply.getApplyId();
    }

    @Override
    @Transactional
    public void saveAiSnapshot(String applyId, String snapshotJson) {
        ConfirmApply apply = require(applyId);
        ConfirmApply upd = new ConfirmApply();
        upd.setApplyId(applyId);
        upd.setAiSnapshot(snapshotJson);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void submit(String applyId) {
        ConfirmApply apply = require(applyId);
        if (!ConfirmApply.STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BizException("仅草稿状态可提交");
        }
        validate(apply);
        // 引用完整性:关联资产ID 必须能解析到平台真实数据资产卡片(平台未接入则不阻断)。杜绝幽灵资产。
        if (!cardArchive.assetCardResolvable(apply.getAssetId())) {
            throw new BizException("关联的数据资产卡片在平台不存在,请重新搜索并选择有效卡片");
        }
        // 资料完整性归集审查(节点40,对齐南网补录工单 M01/M02 填报口径:来源方式/来源主体/G–J 逐维说明)
        validateRegistration(apply);
        // 元数据质量门禁:质量评分<80 自动驳回确权(需求§5.5 接口③)。
        // 注意:此处"持久化驳回"而非抛异常——抛异常会令本事务回滚,驳回状态无法落库。
        int score = metadataGateway.qualityScore(apply.getAssetId());
        if (score < QUALITY_THRESHOLD) {
            String reason = "元数据质量评分 " + score + " 低于 " + QUALITY_THRESHOLD + ",自动驳回,请先治理元数据质量后重新提交";
            updateNode(applyId, ConfirmApply.STATUS_REJECTED, null, reason, null);
            flowLogService.record(apply, ConfirmApply.STATUS_DRAFT, ConfirmApply.STATUS_REJECTED, "元数据质量门禁", reason);
            return;
        }
        // 归集审查通过 -> 由流程引擎启动确权审批链(首状态:人工预审中,对AI校验结果人工复核后再进合规)
        String first = flowEngine.start(FlowDefinitions.DPR_CONFIRM, applyId);
        updateNode(applyId, first, nodeOf(first), null, null);
        flowLogService.record(apply, ConfirmApply.STATUS_DRAFT, first, "申请人", null);
    }

    /**
     * 审批推进(对齐附录F 八节点):
     * 合规审核(50,合规管控小组:生成表3/表4+认定意见) -> 主管复核(60) -> 经理终审(70) -> 制卡(80)。
     * @return 终审通过(节点80)时返回生成的权益卡片ID,否则 null
     */
    @Override
    @Transactional
    public String approve(String applyId) {
        ConfirmApply apply = require(applyId);
        String from = apply.getStatus();
        assertNodeRole(from);
        if (!flowEngine.canAdvance(FlowDefinitions.DPR_CONFIRM, from)) {
            throw new BizException("当前状态不可审批:" + from);
        }
        // 节点50合规审核通过的随状态副作用:生成表3/表4与认定意见
        String opinion = null;
        if (ConfirmApply.STATUS_COMPLIANCE.equals(from)) {
            if (!StringUtils.hasText(apply.getRecognitionOpinion())) {
                apply.setRecognitionOpinion("合规管控小组审核通过,权属认定符合三权分置要求");
            }
            opinion = apply.getRecognitionOpinion();
            summaryService.generate(apply);
        }
        // 由流程引擎推进(去除硬编码 switch):合规->主管->经理->制卡完成
        FlowTransition t = flowEngine.advance(FlowDefinitions.DPR_CONFIRM, applyId, from);
        updateNode(applyId, t.nextState(), nodeOf(t.nextState()), null, opinion);
        // 流转留痕 + 进度通知(责任人=本节点审批人)
        flowLogService.record(apply, from, t.nextState(), responderOf(from), opinion);
        // 终态(节点80制卡):生成权益卡片并回写台账(EquityCardService 内完成)
        if (!t.terminal()) {
            return null;
        }
        String cardId = equityCardService.generateFromApply(apply);
        // 确权完成 → 把产权信息/权益基本信息写回平台资产卡片(单向 PRM→平台;非致命,失败不回滚确权)
        try {
            cardWriteback.writeback(apply.getAssetId());
        } catch (RuntimeException e) {
            log.warn("[资产卡片写回] 确权完成写回失败(不影响确权): assetId={}, 原因={}", apply.getAssetId(), e.getMessage());
        }
        return cardId;
    }

    /** 确权状态 -> 附录F 节点编号(纯映射,非流程逻辑) */
    private Integer nodeOf(String status) {
        if (ConfirmApply.STATUS_PRECHECK.equals(status)) {
            return ConfirmApply.NODE_PRECHECK;
        }
        if (ConfirmApply.STATUS_MANAGER.equals(status)) {
            return ConfirmApply.NODE_MANAGER;
        }
        if (ConfirmApply.STATUS_DIRECTOR.equals(status)) {
            return ConfirmApply.NODE_DIRECTOR;
        }
        if (ConfirmApply.STATUS_DONE.equals(status)) {
            return ConfirmApply.NODE_DONE;
        }
        return ConfirmApply.NODE_COMPLIANCE;
    }

    @Override
    @Transactional
    public void reject(String applyId, String reason) {
        ConfirmApply apply = require(applyId);
        assertNodeRole(apply.getStatus());
        // 仅审批链中(可推进)的状态可驳回——与引擎判定一致
        if (!flowEngine.canAdvance(FlowDefinitions.DPR_CONFIRM, apply.getStatus())) {
            throw new BizException("当前状态不可驳回:" + apply.getStatus());
        }
        String from = apply.getStatus();
        updateNode(applyId, ConfirmApply.STATUS_REJECTED, null, reason, null);
        flowLogService.record(apply, from, ConfirmApply.STATUS_REJECTED, responderOf(from), reason);
    }

    @Override
    @Transactional
    public void withdraw(String applyId, String reason) {
        ConfirmApply apply = require(applyId);
        // 仅审批链活动态(可推进)可撤回;已完成(已制卡)/已驳回/草稿 均不可撤回
        if (!flowEngine.canAdvance(FlowDefinitions.DPR_CONFIRM, apply.getStatus())) {
            throw new BizException("当前状态不可撤回:" + apply.getStatus() + ";草稿请删除,已完成/已驳回不可撤回");
        }
        // 撤回是申请人本人的动作(非节点审批角色门禁):校验调用者为原申请人
        assertApplicant(apply);
        String from = apply.getStatus();
        String why = StringUtils.hasText(reason) ? reason : "申请人主动撤回";
        updateNode(applyId, ConfirmApply.STATUS_WITHDRAWN, null, why, null);
        flowLogService.record(apply, from, ConfirmApply.STATUS_WITHDRAWN, "申请人", why);
    }

    /**
     * 撤回门禁:仅申请人本人可撤回(与节点审批角色相反)。
     * 无用户上下文(内部/未启用认证/单测)、admin/all 角色、或原单无 creatorId:放行;否则须 creatorId 匹配当前用户。
     */
    private void assertApplicant(ConfirmApply apply) {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || !StringUtils.hasText(ctx.getUserId())) {
            return;
        }
        Set<String> roles = ctx.getRoles();
        if (roles != null && (roles.contains("all") || roles.contains("admin"))) {
            return;
        }
        String creator = apply.getCreatorId();
        if (StringUtils.hasText(creator) && !creator.equals(ctx.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "仅申请人本人可撤回该确权申请");
        }
    }

    @Override
    public ConfirmApply getById(String applyId) {
        return require(applyId);
    }

    @Override
    public PageResult<ConfirmApply> page(ConfirmApplyQuery query) {
        IPage<ConfirmApply> page = mapper.selectPage(query.toPage(), historyWrapper(query));
        return PageResult.of(page);
    }

    /** 历史记录多维过滤:数据集(资产名)/权属类型/状态/人员(权属人)/申请时间范围。 */
    private LambdaQueryWrapper<ConfirmApply> historyWrapper(ConfirmApplyQuery query) {
        LambdaQueryWrapper<ConfirmApply> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(query.getAssetName()), ConfirmApply::getAssetName, query.getAssetName())
                .eq(StringUtils.hasText(query.getRightType()), ConfirmApply::getRightType, query.getRightType())
                .eq(StringUtils.hasText(query.getStatus()), ConfirmApply::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getRightHolder()), ConfirmApply::getRightHolder, query.getRightHolder())
                .ge(StringUtils.hasText(query.getCreateTimeStart()), ConfirmApply::getCreateTime, query.getCreateTimeStart())
                .le(StringUtils.hasText(query.getCreateTimeEnd()), ConfirmApply::getCreateTime, query.getCreateTimeEnd())
                .orderByDesc(ConfirmApply::getCreateTime);
        return w;
    }

    @Override
    public byte[] exportHistory(ConfirmApplyQuery query) {
        List<ConfirmApply> list = mapper.selectList(historyWrapper(query));
        StringBuilder sb = new StringBuilder("﻿"); // UTF-8 BOM
        sb.append("申请编号,资产名称,权属类型,权属人,状态,驳回原因,申请时间,处理时效\n");
        for (ConfirmApply a : list) {
            sb.append(csv(a.getApplyNo())).append(',')
                    .append(csv(a.getAssetName())).append(',')
                    .append(csv(a.getRightType())).append(',')
                    .append(csv(a.getRightHolder())).append(',')
                    .append(csv(a.getStatus())).append(',')
                    .append(csv(a.getRejectReason())).append(',')
                    .append(csv(String.valueOf(a.getCreateTime()))).append(',')
                    .append(csv(durationOf(a))).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** 处理时效:终态(已完成/已驳回/已撤回)用 updateTime-createTime,在途用 now-createTime。 */
    private String durationOf(ConfirmApply a) {
        if (a.getCreateTime() == null) {
            return "";
        }
        boolean terminal = ConfirmApply.STATUS_DONE.equals(a.getStatus())
                || ConfirmApply.STATUS_REJECTED.equals(a.getStatus())
                || ConfirmApply.STATUS_WITHDRAWN.equals(a.getStatus());
        java.time.LocalDateTime end = terminal && a.getUpdateTime() != null
                ? a.getUpdateTime() : java.time.LocalDateTime.now();
        long mins = java.time.Duration.between(a.getCreateTime(), end).toMinutes();
        if (mins < 0) {
            mins = 0;
        }
        long days = mins / 1440;
        long hours = (mins % 1440) / 60;
        return days + "天" + hours + "小时" + (terminal ? "" : "(在途)");
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

    @Override
    public com.csg.prm.confirm.dto.BatchResult batchSubmit(List<String> applyIds) {
        com.csg.prm.confirm.dto.BatchResult r = new com.csg.prm.confirm.dto.BatchResult();
        if (applyIds == null) {
            return r;
        }
        for (String id : applyIds) {
            try {
                submit(id);
                r.ok();
            } catch (RuntimeException e) {
                r.fail(id, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public com.csg.prm.confirm.dto.BatchResult batchApprove(List<String> applyIds) {
        com.csg.prm.confirm.dto.BatchResult r = new com.csg.prm.confirm.dto.BatchResult();
        if (applyIds == null) {
            return r;
        }
        for (String id : applyIds) {
            try {
                approve(id);
                r.ok();
            } catch (RuntimeException e) {
                r.fail(id, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public com.csg.prm.confirm.dto.BatchResult batchReject(List<String> applyIds, String reason) {
        com.csg.prm.confirm.dto.BatchResult r = new com.csg.prm.confirm.dto.BatchResult();
        if (applyIds == null) {
            return r;
        }
        for (String id : applyIds) {
            try {
                reject(id, reason);
                r.ok();
            } catch (RuntimeException e) {
                r.fail(id, e.getMessage());
            }
        }
        return r;
    }

    private void updateNode(String applyId, String status, Integer node, String rejectReason, String opinion) {
        ConfirmApply upd = new ConfirmApply();
        upd.setApplyId(applyId);
        upd.setStatus(status);
        upd.setCurrentNode(node);
        upd.setRejectReason(rejectReason);
        upd.setRecognitionOpinion(opinion);
        mapper.updateById(upd);
    }

    private ConfirmApply require(String applyId) {
        if (!StringUtils.hasText(applyId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "申请ID不能为空");
        }
        ConfirmApply apply = mapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "确权申请不存在");
        }
        return apply;
    }

    /**
     * 确权登记完整性校验(对齐南网补录工单 M01/M02 填报规则):
     * ① 来源方式 A–F 至少选一("ABCDEF中至少填一个");
     * ② 来源含 B–F(非纯自行生产)须填来源主体名称;
     * ③ 关联识别 G/H/I/J 涉及则对应主体说明逐维必填("填是则后两列必填")。
     */
    private void validateRegistration(ConfirmApply apply) {
        // 确权变更(附录F §3.3.2 重新确权):须填写变更触发类型(数据新增/来源变更/管理要求变更/权益到期/其他)
        if ("确权变更".equals(apply.getRegisterType()) && !StringUtils.hasText(apply.getChangeTrigger())) {
            throw new BizException("确权变更须选择变更触发类型(数据新增/数据来源变更/管理要求变更/权益到期/其他)");
        }
        String src = apply.getSourceIdentification() == null ? "" : apply.getSourceIdentification().toUpperCase();
        String rel = apply.getRelationIdentification() == null ? "" : apply.getRelationIdentification().toUpperCase();
        if (!containsAny(src, "A", "B", "C", "D", "E", "F")) {
            throw new BizException("请至少选择一种数据来源方式(A自行生产/B公开采集/C公共授权/D公共生产/E交易采购/F其他)");
        }
        if (containsAny(src, "B", "C", "D", "E", "F") && !StringUtils.hasText(apply.getSourceSubject())) {
            throw new BizException("数据来源涉及公开采集/受让/委托/交易等(B–F),须填写来源主体名称");
        }
        if (rel.contains("G") && !StringUtils.hasText(apply.getRegulated())) {
            throw new BizException("涉及行政监管要求(G),须填写监管要求/关联主体说明");
        }
        if (rel.contains("H") && !StringUtils.hasText(apply.getPrivacyInfo())) {
            throw new BizException("涉及用户个人/家庭隐私(H),须填写隐私关联主体说明");
        }
        if ((rel.contains("I") || Boolean.TRUE.equals(apply.getInvolvesThirdParty()))
                && !StringUtils.hasText(apply.getThirdPartyInfo())) {
            throw new BizException("涉及第三方商业机密(I),须填写第三方权益信息");
        }
        if (rel.contains("J") && !StringUtils.hasText(apply.getRelationSubject())) {
            throw new BizException("存在其他数据权益约束协议(J),须填写关联主体说明");
        }
    }

    private static boolean containsAny(String s, String... codes) {
        for (String c : codes) {
            if (s.contains(c)) {
                return true;
            }
        }
        return false;
    }

    private void validate(ConfirmApply apply) {
        if (apply == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(apply.getAssetId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "关联资产ID不能为空");
        }
        if (!StringUtils.hasText(apply.getAssetName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产名称不能为空");
        }
        if (!StringUtils.hasText(apply.getRightType())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "权属类型不能为空");
        }
    }

    /** 申请编号对齐南网补录工单规范:MDAU-00-日期-5位序号(如 MDAU-00-20250725-00026) */
    private String generateApplyNo() {
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        int seq = (int) (Math.abs(java.util.UUID.randomUUID().getLeastSignificantBits()) % 100000);
        return String.format("MDAU-00-%s-%05d", date, seq);
    }
}
