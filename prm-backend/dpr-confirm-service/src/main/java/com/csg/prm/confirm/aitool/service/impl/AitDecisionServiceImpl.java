package com.csg.prm.confirm.aitool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitDecision;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.mapper.AitConflictMapper;
import com.csg.prm.confirm.aitool.mapper.AitDecisionMapper;
import com.csg.prm.confirm.aitool.mapper.AitKgClaimMapper;
import com.csg.prm.confirm.aitool.mapper.AitMaterialMapper;
import com.csg.prm.confirm.aitool.mapper.AitParseResultMapper;
import com.csg.prm.confirm.aitool.service.AitDecisionService;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AitDecisionServiceImpl implements AitDecisionService {

    private static final Set<String> STD_RIGHT = Set.of("数据持有权", "数据加工使用权", "数据产品经营权");
    // 关键因子权重(SW-007):材料完整性30% / 权属无冲突40% / 合规15% / 历史匹配15%
    private static final double W_MATERIAL = 0.30;
    private static final double W_CONFLICT = 0.40;
    private static final double W_COMPLIANCE = 0.15;
    private static final double W_HISTORY = 0.15;

    private final AitDecisionMapper decisionMapper;
    private final AitMaterialMapper materialMapper;
    private final AitParseResultMapper parseMapper;
    private final AitConflictMapper conflictMapper;
    private final AitKgClaimMapper claimMapper;
    private final ConfirmApplyMapper applyMapper;
    private final DawatAiGateway ai;

    public AitDecisionServiceImpl(AitDecisionMapper decisionMapper, AitMaterialMapper materialMapper,
                                  AitParseResultMapper parseMapper, AitConflictMapper conflictMapper,
                                  AitKgClaimMapper claimMapper, ConfirmApplyMapper applyMapper, DawatAiGateway ai) {
        this.decisionMapper = decisionMapper;
        this.materialMapper = materialMapper;
        this.parseMapper = parseMapper;
        this.conflictMapper = conflictMapper;
        this.claimMapper = claimMapper;
        this.applyMapper = applyMapper;
        this.ai = ai;
    }

    @Override
    @Transactional
    public AitDecision analyze(String applyId) {
        ConfirmApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "确权申请不存在");
        }
        String assetId = apply.getAssetId();

        // 因子1:材料完整性
        List<AitMaterial> materials = materialMapper.selectList(
                new LambdaQueryWrapper<AitMaterial>().eq(AitMaterial::getApplyId, applyId));
        double matScore = materialCompleteness(materials);

        // 因子2:权属无冲突
        List<AitConflict> conflicts = conflictMapper.selectList(new LambdaQueryWrapper<AitConflict>()
                .eq(AitConflict::getAssetId, assetId).eq(AitConflict::getStatus, AitConflict.STATUS_OPEN));
        long high = conflicts.stream().filter(c -> "高".equals(c.getRiskLevel())).count();
        long med = conflicts.stream().filter(c -> "中".equals(c.getRiskLevel())).count();
        double confScore = conflicts.isEmpty() ? 100d : Math.max(0d, 100d - high * 40 - med * 20);

        // 因子3:合规性
        double compScore = STD_RIGHT.contains(apply.getRightType()) ? 90d : 60d;

        // 因子4:历史案例匹配度
        List<AitKgClaim> claims = claimMapper.selectList(
                new LambdaQueryWrapper<AitKgClaim>().eq(AitKgClaim::getAssetId, assetId));
        boolean histMatch = claims.stream().anyMatch(c -> AitKgClaim.SRC_HISTORY.equals(c.getSourceType())
                && normEq(c.getRightType(), apply.getRightType()));
        double histScore = histMatch ? 85d : 72d;

        double composite = round(matScore * W_MATERIAL + confScore * W_CONFLICT
                + compScore * W_COMPLIANCE + histScore * W_HISTORY);

        // 结果预测
        String prediction;
        if (materials.isEmpty() || matScore < 60) {
            prediction = AitDecision.PRED_SUPPLEMENT;
        } else if (composite < 55 || high >= 2) {
            prediction = AitDecision.PRED_REJECT;
        } else if (composite >= 80 && high == 0) {
            prediction = AitDecision.PRED_PASS;
        } else {
            prediction = AitDecision.PRED_SUPPLEMENT;
        }

        String factorsJson = String.format(
                "[{\"name\":\"材料完整性\",\"weight\":0.30,\"score\":%s},{\"name\":\"权属无冲突\",\"weight\":0.40,\"score\":%s},"
              + "{\"name\":\"合规性\",\"weight\":0.15,\"score\":%s},{\"name\":\"历史案例匹配度\",\"weight\":0.15,\"score\":%s}]",
                matScore, confScore, compScore, histScore);
        String strength = factorTag(matScore >= 85, "材料完整性") + factorTag(confScore >= 85, "权属无冲突")
                + factorTag(compScore >= 85, "合规性") + factorTag(histScore >= 85, "历史匹配度");
        String weak = factorTag(matScore < 70, "材料完整性") + factorTag(confScore < 70, "权属无冲突")
                + factorTag(compScore < 70, "合规性") + factorTag(histScore < 70, "历史匹配度");

        // 权益分割方案(多主体)
        Set<String> subjects = claims.stream().map(AitKgClaim::getSubject).filter(StringUtils::hasText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        String split = subjects.size() >= 2
                ? "按业务范围分割:" + subjects.stream().map(s -> s + "(约定范围)").collect(Collectors.joining("、"))
                : "单一权利主体,无需分割";

        String supplement = matScore < 80 ? "需补充权属证明/授权函(印章或要素缺失项)" : "材料齐备";
        String pending = conflicts.isEmpty() ? "无待处置冲突"
                : "待处置冲突 " + conflicts.size() + " 项:"
                + conflicts.stream().map(AitConflict::getConflictType).distinct().collect(Collectors.joining("、"));
        String reason = "综合评分 " + composite + " 分;" + pending + ";材料完整性 " + matScore + ",权属无冲突 " + confScore + "。";

        // RAG 智能建议(大模型/本地桩)
        String ragAdvice;
        try {
            ragAdvice = ai.ask("数据确权决策依据与流程要点(" + apply.getRightType() + ")").answer();
        } catch (RuntimeException e) {
            ragAdvice = "[RAG] 依据《数据二十条》与南网制度,按三权分置与先确后授原则研判。";
        }

        String evidence = "0x" + Sm3Util.hashHex(applyId + "|" + composite + "|" + prediction + "|" + factorsJson);

        AitDecision d = new AitDecision();
        d.setApplyId(applyId);
        d.setAssetId(assetId);
        d.setPrediction(prediction);
        d.setScore(composite);
        d.setFactorsJson(factorsJson);
        d.setStrengthFactors(StringUtils.hasText(strength) ? strength : "无明显优势因子");
        d.setWeakFactors(StringUtils.hasText(weak) ? weak : "无明显短板");
        d.setSplitPlan(split);
        d.setReason(reason);
        d.setSupplementMaterials(supplement);
        d.setPendingConflicts(pending);
        d.setRagAdvice(ragAdvice);
        d.setEvidenceChain(evidence);

        decisionMapper.delete(new LambdaQueryWrapper<AitDecision>().eq(AitDecision::getApplyId, applyId));
        decisionMapper.insert(d);
        return d;
    }

    private double materialCompleteness(List<AitMaterial> materials) {
        if (materials.isEmpty()) {
            return 0d;
        }
        int parsed = 0;
        boolean sealMissing = false;
        for (AitMaterial m : materials) {
            AitParseResult r = parseMapper.selectOne(
                    new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, m.getMaterialId()));
            if (r != null) {
                parsed++;
                if (!"有效".equals(r.getSealValid())) {
                    sealMissing = true;
                }
            }
        }
        double base = round(parsed * 100.0 / materials.size());
        return sealMissing ? Math.min(base, 70d) : base;
    }

    @Override
    public AitDecision getByApply(String applyId) {
        AitDecision d = decisionMapper.selectOne(
                new LambdaQueryWrapper<AitDecision>().eq(AitDecision::getApplyId, applyId));
        if (d == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "尚未生成决策建议");
        }
        return d;
    }

    @Override
    public PageResult<AitDecision> page(PageQuery query, String prediction) {
        LambdaQueryWrapper<AitDecision> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(prediction), AitDecision::getPrediction, prediction)
                .orderByDesc(AitDecision::getCreateTime);
        IPage<AitDecision> p = decisionMapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    private String factorTag(boolean cond, String name) {
        return cond ? (name + " ") : "";
    }

    private boolean normEq(String a, String b) {
        return norm(a).equals(norm(b));
    }

    private String norm(String rt) {
        if (rt == null) {
            return "";
        }
        if (rt.contains("经营")) {
            return "经营";
        }
        if (rt.contains("使用") || rt.contains("加工")) {
            return "使用";
        }
        if (rt.contains("持有") || rt.contains("所有")) {
            return "持有";
        }
        return rt;
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
