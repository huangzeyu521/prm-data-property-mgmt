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
import java.util.ArrayList;
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
        MaterialStat matStat = materialStat(materials);
        double matScore = matStat.score();

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

        // RAG 智能建议(#18 简化检索增强:历史案例+法规条款+申请事实 → 大模型生成 建议+预测结论)
        String ragContext = buildRagContext(apply, materials.size(), matStat.parsed(), conflicts,
                (int) high, (int) med, claims);
        String ragAdvice;
        String ragCitations;
        String aiPrediction;
        try {
            DawatAiGateway.RagAnswer ans = ai.ask(ragContext);
            ragAdvice = ans.answer();
            ragCitations = String.join(";", ans.citations());
            aiPrediction = extractPrediction(ragAdvice);
        } catch (RuntimeException e) {
            ragAdvice = "[RAG] 模型暂不可用,依据《数据二十条》与南网制度按三权分置与先确后授原则研判。";
            ragCitations = "《数据二十条》;南网数据确权授权业务指导书";
            aiPrediction = null;
        }

        String evidence = "0x" + Sm3Util.hashHex(
                applyId + "|" + composite + "|" + prediction + "|" + aiPrediction + "|" + factorsJson);

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
        d.setAiPrediction(aiPrediction);
        d.setRagCitations(ragCitations);
        d.setEvidenceChain(evidence);

        decisionMapper.delete(new LambdaQueryWrapper<AitDecision>().eq(AitDecision::getApplyId, applyId));
        decisionMapper.insert(d);
        return d;
    }

    /** 材料完整性统计:已解析数/印章缺失/完整性评分 */
    private record MaterialStat(int parsed, boolean sealMissing, double score) {
    }

    private MaterialStat materialStat(List<AitMaterial> materials) {
        if (materials.isEmpty()) {
            return new MaterialStat(0, false, 0d);
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
        return new MaterialStat(parsed, sealMissing, sealMissing ? Math.min(base, 70d) : base);
    }

    /**
     * #18 简化 RAG:组装结构化检索上下文(申请事实 + 材料 + 冲突 + 历史案例 + 法规条款),
     * 交大模型生成预测结论(建议通过/驳回/补充材料)与建议;未建向量库,检索为结构化/关键词方式。
     */
    private String buildRagContext(ConfirmApply apply, int materialTotal, int parsedCount,
                                   List<AitConflict> conflicts, int high, int med, List<AitKgClaim> claims) {
        String conflictTypes = conflicts.isEmpty() ? "无"
                : conflicts.stream().map(AitConflict::getConflictType).distinct().collect(Collectors.joining("、"));
        return "【任务】确权结果预测:请基于下方检索内容研判,预测结论限定为 建议通过/建议驳回/建议补充材料 三者之一,"
                + "用【】标注在回答开头,并给出理由与法规引用。\n"
                + "【申请事实】资产:" + apply.getAssetName() + "(" + apply.getAssetId() + ");申请权利类型:"
                + apply.getRightType() + ";申请主体:" + apply.getRightHolder() + "\n"
                + "【材料情况】已上传材料数:" + materialTotal + ";已解析材料数:" + parsedCount + "\n"
                + "【冲突情况】未处置冲突数:" + conflicts.size() + ";高风险冲突数:" + high + ";中风险冲突数:" + med
                + ";冲突类型:" + conflictTypes + "\n"
                + "【历史案例】" + historyCases(apply, claims) + "\n"
                + "【法规条款】" + String.join(" / ", regulationSnippets(apply.getRightType(), !conflicts.isEmpty()));
    }

    /** 历史案例结构化检索:同资产历史确权主张 + 同资产既往智能研判记录 */
    private String historyCases(ConfirmApply apply, List<AitKgClaim> claims) {
        List<String> cases = new ArrayList<>();
        claims.stream().filter(c -> AitKgClaim.SRC_HISTORY.equals(c.getSourceType()))
                .forEach(c -> cases.add("历史确权:" + c.getSubject() + "(" + c.getRightType()
                        + (c.getValidDate() != null ? ",有效期至" + c.getValidDate().toLocalDate() : "") + ")"));
        decisionMapper.selectList(new LambdaQueryWrapper<AitDecision>()
                        .eq(AitDecision::getAssetId, apply.getAssetId()).ne(AitDecision::getApplyId, apply.getApplyId()))
                .forEach(d -> cases.add("既往研判:" + d.getPrediction() + "(评分" + d.getScore() + ")"));
        return cases.isEmpty() ? "无同资产历史确权案例" : String.join(";", cases);
    }

    /** 现行法规条款库(《数据二十条》/南网制度)按权利类型与冲突情形关键词检索 */
    private List<String> regulationSnippets(String rightType, boolean hasConflict) {
        List<String> snippets = new ArrayList<>();
        snippets.add("《数据二十条》:建立数据资源持有权、数据加工使用权、数据产品经营权等分置的产权运行机制");
        String norm = norm(rightType);
        if ("持有".equals(norm)) {
            snippets.add("《数据二十条》:推进实施公共数据确权授权机制,保障数据持有主体权益");
        } else if ("使用".equals(norm)) {
            snippets.add("《数据二十条》:保障数据加工使用权,促进数据使用价值复用");
        } else if ("经营".equals(norm)) {
            snippets.add("南网制度(附录F 3.4.3):数据产品经营权对外授权范围仅限对外开放目录");
        }
        if (hasConflict) {
            snippets.add("南网制度(附录F 3.2):先确后授,权属冲突须经合规管控小组裁定后方可确权");
        }
        return snippets;
    }

    /** 从模型回答中抽取预测结论(未命中返回 null,由前端提示人工复核) */
    private String extractPrediction(String answer) {
        if (answer == null) {
            return null;
        }
        if (answer.contains(AitDecision.PRED_REJECT)) {
            return AitDecision.PRED_REJECT;
        }
        if (answer.contains(AitDecision.PRED_SUPPLEMENT) || answer.contains("补充材料")) {
            return AitDecision.PRED_SUPPLEMENT;
        }
        if (answer.contains(AitDecision.PRED_PASS)) {
            return AitDecision.PRED_PASS;
        }
        return null;
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
