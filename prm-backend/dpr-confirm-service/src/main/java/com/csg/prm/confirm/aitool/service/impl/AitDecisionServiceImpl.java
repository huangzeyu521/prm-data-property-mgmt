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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    /** 按比例方案:全字段主张折算的满额权重 */
    private static final int FULL_SCOPE_WEIGHT = 10;
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

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

        // 每因子附"得分依据"说明(#19),前端进度条悬浮展示
        String matReason = materials.isEmpty() ? "未上传任何权属证明材料"
                : "共" + materials.size() + "份材料,已解析" + matStat.parsed() + "份"
                + (matStat.sealMissing() ? ";印章缺失/可疑,得分封顶70" : ";印章均有效");
        String confReason = conflicts.isEmpty() ? "无未处置权属冲突"
                : "未处置冲突" + conflicts.size() + "项:高风险" + high + "项(每项-40)/中风险" + med + "项(每项-20)";
        String compReason = "权利类型「" + apply.getRightType() + "」"
                + (STD_RIGHT.contains(apply.getRightType()) ? "属三权分置标准权利" : "非三权分置标准权利,需人工复核");
        String histReason = histMatch ? "同资产存在同类权利的历史确权记录,匹配度高"
                : "无同资产同类权利历史确权记录,按基准分计";
        String factorsJson = String.format(
                "[{\"name\":\"材料完整性\",\"weight\":0.30,\"score\":%s,\"reason\":\"%s\"},"
              + "{\"name\":\"权属无冲突\",\"weight\":0.40,\"score\":%s,\"reason\":\"%s\"},"
              + "{\"name\":\"合规性\",\"weight\":0.15,\"score\":%s,\"reason\":\"%s\"},"
              + "{\"name\":\"历史案例匹配度\",\"weight\":0.15,\"score\":%s,\"reason\":\"%s\"}]",
                matScore, matReason, confScore, confReason, compScore, compReason, histScore, histReason);
        String strength = factorTag(matScore >= 85, "材料完整性") + factorTag(confScore >= 85, "权属无冲突")
                + factorTag(compScore >= 85, "合规性") + factorTag(histScore >= 85, "历史匹配度");
        String weak = factorTag(matScore < 70, "材料完整性") + factorTag(confScore < 70, "权属无冲突")
                + factorTag(compScore < 70, "合规性") + factorTag(histScore < 70, "历史匹配度");

        // 权益分割方案(#20):多主体自动生成 按业务范围/按比例 双方案,明确各主体 权利/范围/期限/责任
        Set<String> subjects = claims.stream().map(AitKgClaim::getSubject).filter(StringUtils::hasText)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        String[] splitPlans = buildSplitPlans(subjects, claims);
        String split = splitPlans[0];

        // 需补材料逐项点名(#21):无材料给基础清单,有问题材料按文件名点名
        String supplement = materials.isEmpty()
                ? "未上传材料,需提供:权属证明、授权函/委托书(盖章件)"
                : (matStat.issues().isEmpty() ? "材料齐备" : "需补正:" + String.join(";", matStat.issues()));
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

        // 理由纳入 AI 预测一致性(#21)
        reason = reason + (aiPrediction == null ? "AI 预测未生成,建议人工复核。"
                : prediction.equals(aiPrediction) ? "AI 预测与规则结论一致。"
                : "AI 预测(" + aiPrediction + ")与规则结论不一致,建议人工复核。");

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
        d.setSplitPlansJson(splitPlans[1]);
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

    /** 材料完整性统计:已解析数/印章缺失/完整性评分/逐份问题点名(#21) */
    private record MaterialStat(int parsed, boolean sealMissing, double score, List<String> issues) {
    }

    private MaterialStat materialStat(List<AitMaterial> materials) {
        if (materials.isEmpty()) {
            return new MaterialStat(0, false, 0d, List.of());
        }
        int parsed = 0;
        boolean sealMissing = false;
        List<String> issues = new ArrayList<>();
        for (AitMaterial m : materials) {
            AitParseResult r = parseMapper.selectOne(
                    new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, m.getMaterialId()));
            if (r == null) {
                issues.add("《" + m.getFileName() + "》尚未解析,请先完成智能解析");
                continue;
            }
            parsed++;
            if (!"有效".equals(r.getSealValid())) {
                sealMissing = true;
                String seal = StringUtils.hasText(r.getSealValid()) ? r.getSealValid() : "未检出";
                issues.add("《" + m.getFileName() + "》印章" + seal + ",需补充盖章版权属证明");
            }
        }
        double base = round(parsed * 100.0 / materials.size());
        return new MaterialStat(parsed, sealMissing, sealMissing ? Math.min(base, 70d) : base, issues);
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

    /**
     * #20 权益分割方案生成:多主体时输出 [0]=推荐摘要、[1]=双方案明细 JSON。
     * 方案A 按业务范围(各主体既有主张范围,重叠部分标注协商);方案B 按比例(主张范围字段数折算)。
     */
    private String[] buildSplitPlans(Set<String> subjects, List<AitKgClaim> claims) {
        if (subjects.size() < 2) {
            return new String[]{"单一权利主体,无需分割", "[]"};
        }
        Map<String, List<AitKgClaim>> bySubject = new LinkedHashMap<>();
        claims.stream().filter(c -> StringUtils.hasText(c.getSubject()))
                .forEach(c -> bySubject.computeIfAbsent(c.getSubject(), k -> new ArrayList<>()).add(c));

        List<Map<String, Object>> planA = new ArrayList<>();
        List<Map<String, Object>> planB = new ArrayList<>();
        Map<String, Integer> weights = new LinkedHashMap<>();
        bySubject.forEach((s, cs) -> weights.put(s, cs.stream().mapToInt(c -> scopeWeight(c.getAuthScope())).max().orElse(1)));
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();

        boolean hasOverlap = false;
        int assigned = 0;
        int idx = 0;
        for (Map.Entry<String, List<AitKgClaim>> e : bySubject.entrySet()) {
            String subject = e.getKey();
            List<AitKgClaim> cs = e.getValue();
            String rightType = cs.stream().map(AitKgClaim::getRightType).filter(StringUtils::hasText)
                    .distinct().collect(Collectors.joining("、"));
            String scope = cs.stream().map(AitKgClaim::getAuthScope).filter(StringUtils::hasText)
                    .distinct().collect(Collectors.joining("、"));
            String overlapNote = overlapNote(subject, cs, bySubject);
            hasOverlap = hasOverlap || !overlapNote.isEmpty();
            String term = cs.stream().map(AitKgClaim::getValidDate).filter(java.util.Objects::nonNull)
                    .min(java.util.Comparator.naturalOrder())
                    .map(t -> "至" + t.toLocalDate()).orElse("按申请约定");
            String duty = cs.stream().map(c -> dutyOf(c.getRightType())).distinct().collect(Collectors.joining(";"));

            Map<String, Object> a = new LinkedHashMap<>();
            a.put("subject", subject);
            a.put("rightType", rightType.isEmpty() ? "按主张认定" : rightType);
            a.put("scope", (scope.isEmpty() ? "约定范围" : scope) + overlapNote);
            a.put("term", term);
            a.put("duty", duty);
            planA.add(a);

            idx++;
            int ratio = idx == bySubject.size() ? 100 - assigned
                    : (int) Math.round(weights.get(subject) * 100.0 / totalWeight);
            assigned += ratio;
            Map<String, Object> b = new LinkedHashMap<>(a);
            b.put("scope", "全量范围按比例 " + ratio + "%(主张范围字段数折算)");
            planB.add(b);
        }

        String recommend = hasOverlap ? "按比例" : "按业务范围";
        String summary = "多主体(" + subjects.size() + "方)权益分割:推荐「" + recommend + "」方案"
                + (hasOverlap ? "(主张范围存在重叠)" : "(主张范围互不重叠)")
                + ";涉及主体:" + String.join("、", subjects) + ";双方案明细见下表";
        List<Map<String, Object>> plans = List.of(
                Map.of("plan", "按业务范围", "desc", "按各主体既有业务主张划分权利范围,重叠部分标注协商划分", "items", planA),
                Map.of("plan", "按比例", "desc", "按各主体主张范围字段数折算比例分配权益(全字段=满额)", "items", planB));
        try {
            return new String[]{summary, JSON_MAPPER.writeValueAsString(plans)};
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return new String[]{summary, "[]"};
        }
    }

    /** 主体范围与其他主体的重叠标注(全字段视为包含一切) */
    private String overlapNote(String subject, List<AitKgClaim> own, Map<String, List<AitKgClaim>> bySubject) {
        Set<String> mine = fieldsOf(own);
        List<String> notes = new ArrayList<>();
        for (Map.Entry<String, List<AitKgClaim>> e : bySubject.entrySet()) {
            if (e.getKey().equals(subject)) {
                continue;
            }
            Set<String> theirs = fieldsOf(e.getValue());
            Set<String> shared;
            if (mine == null && theirs == null) {
                shared = Set.of("全字段");
            } else if (mine == null) {
                shared = theirs;
            } else if (theirs == null) {
                shared = mine;
            } else {
                shared = mine.stream().filter(theirs::contains)
                        .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
            }
            if (!shared.isEmpty()) {
                notes.add("与" + e.getKey() + "重叠:" + String.join("、", shared));
            }
        }
        return notes.isEmpty() ? "" : "[" + String.join(";", notes) + ",建议协商或按比例划分]";
    }

    /** 主体全部主张的范围字段集;含全字段主张返回 null(表示不限) */
    private Set<String> fieldsOf(List<AitKgClaim> cs) {
        Set<String> fields = new java.util.LinkedHashSet<>();
        for (AitKgClaim c : cs) {
            String s = c.getAuthScope();
            if (!StringUtils.hasText(s)) {
                continue;
            }
            if (s.contains("全字段") || s.contains("全部") || s.contains("不限")) {
                return null;
            }
            for (String f : s.split("[、,，;；]")) {
                if (StringUtils.hasText(f)) {
                    fields.add(f.trim());
                }
            }
        }
        return fields;
    }

    /** 范围权重:全字段=满额10,否则字段数(至少1),用于按比例方案折算 */
    private int scopeWeight(String scope) {
        if (!StringUtils.hasText(scope) || scope.contains("全字段") || scope.contains("全部") || scope.contains("不限")) {
            return FULL_SCOPE_WEIGHT;
        }
        return Math.max(1, (int) java.util.Arrays.stream(scope.split("[、,，;；]"))
                .filter(StringUtils::hasText).count());
    }

    /** 责任划分:按权利类型映射(三权分置) */
    private String dutyOf(String rightType) {
        return switch (norm(rightType)) {
            case "持有" -> "数据质量与安全主体责任";
            case "使用" -> "加工合规与最小必要脱敏责任";
            case "经营" -> "对外提供合规与开放目录边界责任";
            default -> "按协议约定承担相应合规责任";
        };
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
