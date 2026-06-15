package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.aitool.entity.AitConstraint;
import com.csg.prm.confirm.aitool.entity.AitDocSegment;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.entity.AitProfile;
import com.csg.prm.confirm.aitool.entity.AitProfileSubject;
import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import com.csg.prm.confirm.aitool.mapper.AitConstraintMapper;
import com.csg.prm.confirm.aitool.mapper.AitDocSegmentMapper;
import com.csg.prm.confirm.aitool.mapper.AitMaterialMapper;
import com.csg.prm.confirm.aitool.mapper.AitParseResultMapper;
import com.csg.prm.confirm.aitool.mapper.AitProfileMapper;
import com.csg.prm.confirm.aitool.mapper.AitProfileSubjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 确权要素识别与特征抽取(可研 1.3):
 * 来源方式(#1)+ 数据特征(#3)+ 五类主体(#2)+ 五类约束(#5),关键词规则 + 模型语义混合(#4),
 * 产出结构化确权画像(#6,表级/附件级),供分类分级/法律校验/授权判断输入。
 */
@Service
public class AitElementService {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final String BY_RULE = "规则";
    private static final String BY_MODEL = "模型";

    /** 主体角色 → 规则标签同义词。 */
    private static final Map<String, String[]> SUBJECT_LABELS = new LinkedHashMap<>();
    /** 约束类型 → 规则标签同义词。 */
    private static final Map<String, String[]> CONSTRAINT_LABELS = new LinkedHashMap<>();
    /** 来源方式关键词 → 标准方式。 */
    private static final Map<String, String> SOURCE_KW = new LinkedHashMap<>();
    /** 数据特征关键词 → 标准特征。 */
    private static final Map<String, String> FEATURE_KW = new LinkedHashMap<>();

    static {
        SUBJECT_LABELS.put(AitProfileSubject.R_SOURCE, new String[]{"来源主体", "数据来源方", "来源方", "数据提供方", "提供方"});
        SUBJECT_LABELS.put(AitProfileSubject.R_AUTHORIZER, new String[]{"授权主体", "授权方", "授权人"});
        SUBJECT_LABELS.put(AitProfileSubject.R_USER, new String[]{"使用主体", "数据使用方", "使用方", "使用单位"});
        SUBJECT_LABELS.put(AitProfileSubject.R_PROCESSOR, new String[]{"加工主体", "数据加工方", "加工方", "处理方"});
        SUBJECT_LABELS.put(AitProfileSubject.R_SHARED, new String[]{"共享对象", "数据共享对象", "共享方", "共享给"});

        CONSTRAINT_LABELS.put(AitConstraint.T_AUTH_SCOPE, new String[]{"授权范围"});
        CONSTRAINT_LABELS.put(AitConstraint.T_USE_BOUNDARY, new String[]{"使用边界", "使用范围限制", "使用限制", "使用范围"});
        CONSTRAINT_LABELS.put(AitConstraint.T_SHARE_LIMIT, new String[]{"共享限制", "共享范围", "共享约束", "禁止共享"});
        CONSTRAINT_LABELS.put(AitConstraint.T_RETENTION, new String[]{"保留期限", "保存期限", "留存期限", "数据保留"});
        CONSTRAINT_LABELS.put(AitConstraint.T_DESENSITIZE, new String[]{"脱敏要求", "数据脱敏", "脱敏", "去标识"});

        SOURCE_KW.put("自行生产", "自行生产");
        SOURCE_KW.put("自产", "自行生产");
        SOURCE_KW.put("自有", "自行生产");
        SOURCE_KW.put("公开采集", "公开采集");
        SOURCE_KW.put("网络采集", "公开采集");
        SOURCE_KW.put("爬取", "公开采集");
        SOURCE_KW.put("公共数据授权", "公共数据授权");
        SOURCE_KW.put("政务数据", "公共数据授权");
        SOURCE_KW.put("共同生产", "共同生产");
        SOURCE_KW.put("合作生产", "共同生产");
        SOURCE_KW.put("联合生产", "共同生产");
        SOURCE_KW.put("交易采购", "交易采购");
        SOURCE_KW.put("采购", "交易采购");
        SOURCE_KW.put("交易", "交易采购");
        SOURCE_KW.put("购买", "交易采购");

        FEATURE_KW.put("敏感个人信息", "敏感个人信息");
        FEATURE_KW.put("个人信息", "个人信息");
        FEATURE_KW.put("隐私", "个人信息");
        FEATURE_KW.put("商业秘密", "商业秘密");
        FEATURE_KW.put("商密", "商业秘密");
        FEATURE_KW.put("监管数据", "监管数据");
        FEATURE_KW.put("监管", "监管数据");
        FEATURE_KW.put("电网生产数据", "电网生产数据");
        FEATURE_KW.put("生产数据", "电网生产数据");
        FEATURE_KW.put("内部运营数据", "内部运营数据");
        FEATURE_KW.put("运营数据", "内部运营数据");
    }

    private final AitMaterialMapper materialMapper;
    private final AitParseResultMapper parseMapper;
    private final AitDocSegmentMapper segmentMapper;
    private final AitProfileMapper profileMapper;
    private final AitProfileSubjectMapper subjectMapper;
    private final AitConstraintMapper constraintMapper;
    private final AiToolParseGateway gateway;

    public AitElementService(AitMaterialMapper materialMapper, AitParseResultMapper parseMapper,
                             AitDocSegmentMapper segmentMapper, AitProfileMapper profileMapper,
                             AitProfileSubjectMapper subjectMapper, AitConstraintMapper constraintMapper,
                             AiToolParseGateway gateway) {
        this.materialMapper = materialMapper;
        this.parseMapper = parseMapper;
        this.segmentMapper = segmentMapper;
        this.profileMapper = profileMapper;
        this.subjectMapper = subjectMapper;
        this.constraintMapper = constraintMapper;
        this.gateway = gateway;
    }

    public record ProfileDTO(AitProfile profile, List<AitProfileSubject> subjects, List<AitConstraint> constraints) {
    }

    @Transactional
    public ProfileDTO extract(String materialId, boolean useModel) {
        AitMaterial m = materialMapper.selectById(materialId);
        if (m == null) {
            throw new BizException("材料不存在");
        }
        AitParseResult parse = parseMapper.selectOne(
                new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, materialId));
        String corpus = buildCorpus(m, parse);

        // 规则抽取(确定性,离线)
        Map<String, String> ruleSubjects = ruleSubjects(corpus);
        Map<String, String> ruleConstraints = ruleConstraints(corpus, parse);
        String sourceMethod = ruleSourceMethod(corpus, parse);
        Set<String> features = ruleFeatures(corpus, parse);
        String sourceBy = BY_RULE;
        double confidence = 0.9;

        // 模型语义层(规则+模型混合 #4):规则优先,模型补缺
        AiToolParseGateway.ElementSet model = null;
        if (useModel) {
            try {
                model = gateway.extractElements(corpus);
            } catch (RuntimeException ignore) {
                model = null;
            }
        }
        Map<String, String> subjectBy = new LinkedHashMap<>();
        Map<String, String> constraintBy = new LinkedHashMap<>();
        for (String r : SUBJECT_LABELS.keySet()) {
            if (ruleSubjects.containsKey(r)) {
                subjectBy.put(r, BY_RULE);
            }
        }
        for (String t : CONSTRAINT_LABELS.keySet()) {
            if (ruleConstraints.containsKey(t)) {
                constraintBy.put(t, BY_RULE);
            }
        }
        if (model != null) {
            if (model.subjects() != null) {
                for (Map.Entry<String, String> e : model.subjects().entrySet()) {
                    if (!ruleSubjects.containsKey(e.getKey()) && StringUtils.hasText(e.getValue())) {
                        ruleSubjects.put(e.getKey(), e.getValue());
                        subjectBy.put(e.getKey(), BY_MODEL);
                    }
                }
            }
            if (model.constraints() != null) {
                for (Map.Entry<String, String> e : model.constraints().entrySet()) {
                    if (!ruleConstraints.containsKey(e.getKey()) && StringUtils.hasText(e.getValue())) {
                        ruleConstraints.put(e.getKey(), e.getValue());
                        constraintBy.put(e.getKey(), BY_MODEL);
                    }
                }
            }
            if (!StringUtils.hasText(sourceMethod) && StringUtils.hasText(model.sourceMethod())) {
                sourceMethod = model.sourceMethod();
                sourceBy = BY_MODEL;
            }
            if (model.dataFeatures() != null) {
                features.addAll(model.dataFeatures());
            }
            confidence = Math.max(confidence, model.confidence());
        }
        if (!StringUtils.hasText(sourceMethod)) {
            sourceMethod = "其他";
        }

        // 覆盖式落库
        clearOld(materialId);
        String level = inferLevel(m);
        AitProfile profile = new AitProfile();
        profile.setMaterialId(materialId);
        profile.setDataTableRef(m.getDataTableRef());
        profile.setLevel(level);
        profile.setSourceMethod(sourceMethod);
        profile.setSourceMethodBy(sourceBy);
        profile.setDataFeatures(String.join("、", features));
        profile.setConfidence(confidence);
        profile.setElementsJson(buildElementsJson(m, level, sourceMethod, sourceBy, features,
                ruleSubjects, subjectBy, ruleConstraints, constraintBy));
        profileMapper.insert(profile);

        List<AitProfileSubject> subjects = new ArrayList<>();
        for (Map.Entry<String, String> e : ruleSubjects.entrySet()) {
            AitProfileSubject s = new AitProfileSubject();
            s.setProfileId(profile.getProfileId());
            s.setMaterialId(materialId);
            s.setSubjectRole(e.getKey());
            s.setSubjectName(cap(e.getValue(), 500));
            s.setMethod(subjectBy.getOrDefault(e.getKey(), BY_RULE));
            subjectMapper.insert(s);
            subjects.add(s);
        }
        List<AitConstraint> constraints = new ArrayList<>();
        for (Map.Entry<String, String> e : ruleConstraints.entrySet()) {
            AitConstraint c = new AitConstraint();
            c.setProfileId(profile.getProfileId());
            c.setMaterialId(materialId);
            c.setConstraintType(e.getKey());
            c.setConstraintValue(cap(e.getValue(), 1000));
            c.setMethod(constraintBy.getOrDefault(e.getKey(), BY_RULE));
            constraintMapper.insert(c);
            constraints.add(c);
        }
        return new ProfileDTO(profile, subjects, constraints);
    }

    public ProfileDTO profile(String materialId) {
        AitProfile p = profileMapper.selectOne(
                new LambdaQueryWrapper<AitProfile>().eq(AitProfile::getMaterialId, materialId)
                        .orderByDesc(AitProfile::getCreateTime).last("LIMIT 1"));
        if (p == null) {
            throw new BizException("尚未生成确权画像,请先抽取");
        }
        List<AitProfileSubject> subjects = subjectMapper.selectList(
                new LambdaQueryWrapper<AitProfileSubject>().eq(AitProfileSubject::getProfileId, p.getProfileId()));
        List<AitConstraint> constraints = constraintMapper.selectList(
                new LambdaQueryWrapper<AitConstraint>().eq(AitConstraint::getProfileId, p.getProfileId()));
        return new ProfileDTO(p, subjects, constraints);
    }

    /** #6 表级 + 附件级 确权特征视图:按数据表/申请聚合各材料画像。 */
    public Map<String, Object> view(String dataTableRef, String applyId) {
        List<AitProfile> profiles;
        if (StringUtils.hasText(dataTableRef)) {
            // 多表共附件:画像的 dataTableRef 可含多个表(T1;T2),按单表查视图用 LIKE 命中(与 1.2 归集口径一致)
            profiles = profileMapper.selectList(new LambdaQueryWrapper<AitProfile>()
                    .like(AitProfile::getDataTableRef, dataTableRef));
        } else if (StringUtils.hasText(applyId)) {
            List<String> ids = materialMapper.selectList(new LambdaQueryWrapper<AitMaterial>()
                            .eq(AitMaterial::getApplyId, applyId))
                    .stream().map(AitMaterial::getMaterialId).toList();
            profiles = ids.isEmpty() ? List.of() : profileMapper.selectList(
                    new LambdaQueryWrapper<AitProfile>().in(AitProfile::getMaterialId, ids));
        } else {
            profiles = List.of();
        }
        List<Map<String, Object>> tableLevel = new ArrayList<>();
        List<Map<String, Object>> attachmentLevel = new ArrayList<>();
        Set<String> mergedFeatures = new LinkedHashSet<>();
        for (AitProfile p : profiles) {
            Map<String, Object> row = profileBrief(p);
            if (AitProfile.LEVEL_TABLE.equals(p.getLevel())) {
                tableLevel.add(row);
            } else {
                attachmentLevel.add(row);
            }
            if (StringUtils.hasText(p.getDataFeatures())) {
                for (String f : p.getDataFeatures().split("、")) {
                    if (StringUtils.hasText(f)) {
                        mergedFeatures.add(f);
                    }
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dataTableRef", dataTableRef);
        out.put("tableLevel", tableLevel);
        out.put("attachmentLevel", attachmentLevel);
        out.put("mergedFeatures", new ArrayList<>(mergedFeatures));
        out.put("profileCount", profiles.size());
        return out;
    }

    // ---- 规则抽取 ----

    private Map<String, String> ruleSubjects(String corpus) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : SUBJECT_LABELS.entrySet()) {
            String v = labeledValue(corpus, e.getValue());
            if (StringUtils.hasText(v)) {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }

    private Map<String, String> ruleConstraints(String corpus, AitParseResult parse) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : CONSTRAINT_LABELS.entrySet()) {
            String v = labeledValue(corpus, e.getValue());
            if (StringUtils.hasText(v)) {
                out.put(e.getKey(), v);
            }
        }
        // 种子:解析结果的授权范围/权利期限
        if (parse != null) {
            if (!out.containsKey(AitConstraint.T_AUTH_SCOPE) && StringUtils.hasText(parse.getAuthScope())) {
                out.put(AitConstraint.T_AUTH_SCOPE, parse.getAuthScope());
            }
            if (!out.containsKey(AitConstraint.T_RETENTION) && StringUtils.hasText(parse.getRightTerm())) {
                out.put(AitConstraint.T_RETENTION, parse.getRightTerm());
            }
        }
        return out;
    }

    private String ruleSourceMethod(String corpus, AitParseResult parse) {
        if (parse != null && StringUtils.hasText(parse.getDataSource())) {
            return parse.getDataSource();
        }
        for (Map.Entry<String, String> e : SOURCE_KW.entrySet()) {
            if (corpus.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return "";
    }

    private Set<String> ruleFeatures(String corpus, AitParseResult parse) {
        Set<String> out = new LinkedHashSet<>();
        if (parse != null && StringUtils.hasText(parse.getSensitiveType())) {
            out.add(parse.getSensitiveType());
        }
        for (Map.Entry<String, String> e : FEATURE_KW.entrySet()) {
            if (corpus.contains(e.getKey())) {
                out.add(e.getValue());
            }
        }
        return out;
    }

    /** 取标签后的值:命中标签 → 跳过分隔符 → 截取到下一个分隔符。 */
    private static String labeledValue(String corpus, String[] labels) {
        if (!StringUtils.hasText(corpus)) {
            return null;
        }
        for (String label : labels) {
            int idx = corpus.indexOf(label);
            if (idx < 0) {
                continue;
            }
            String rest = corpus.substring(idx + label.length());
            rest = rest.replaceFirst("^[\\s:：=为是,，]+", "");
            int end = rest.length();
            for (String d : new String[]{"\n", "，", "。", ";", "；", "、", "|", "\t"}) {
                int p = rest.indexOf(d);
                if (p >= 0 && p < end) {
                    end = p;
                }
            }
            String v = rest.substring(0, end).trim();
            if (StringUtils.hasText(v)) {
                return v.length() > 200 ? v.substring(0, 200) : v;
            }
        }
        return null;
    }

    /** 语料 = 正文 + 多粒度片段(字段名/字段说明/表注释等),覆盖多源抽取(#4)。 */
    private String buildCorpus(AitMaterial m, AitParseResult parse) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(m.getContent())) {
            sb.append(m.getContent()).append('\n');
        }
        List<AitDocSegment> segs = segmentMapper.selectList(new LambdaQueryWrapper<AitDocSegment>()
                .eq(AitDocSegment::getMaterialId, m.getMaterialId()));
        for (AitDocSegment s : segs) {
            if (StringUtils.hasText(s.getContent())) {
                sb.append(s.getContent()).append('\n');
            }
        }
        if (parse != null && StringUtils.hasText(parse.getRightSubject())) {
            sb.append("权利主体:").append(parse.getRightSubject()).append('\n');
        }
        return sb.toString();
    }

    private String inferLevel(AitMaterial m) {
        String c = m.getCategory();
        if ("元数据".equals(c) || "确权证明".equals(c) || c == null) {
            return AitProfile.LEVEL_TABLE;
        }
        return AitProfile.LEVEL_ATTACHMENT;
    }

    private void clearOld(String materialId) {
        List<AitProfile> old = profileMapper.selectList(
                new LambdaQueryWrapper<AitProfile>().eq(AitProfile::getMaterialId, materialId));
        for (AitProfile p : old) {
            subjectMapper.delete(new LambdaQueryWrapper<AitProfileSubject>()
                    .eq(AitProfileSubject::getProfileId, p.getProfileId()));
            constraintMapper.delete(new LambdaQueryWrapper<AitConstraint>()
                    .eq(AitConstraint::getProfileId, p.getProfileId()));
        }
        profileMapper.delete(new LambdaQueryWrapper<AitProfile>().eq(AitProfile::getMaterialId, materialId));
    }

    private String buildElementsJson(AitMaterial m, String level, String sourceMethod, String sourceBy,
                                     Set<String> features, Map<String, String> subjects, Map<String, String> subjectBy,
                                     Map<String, String> constraints, Map<String, String> constraintBy) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("materialId", m.getMaterialId());
        root.put("dataTableRef", m.getDataTableRef());
        root.put("level", level);
        root.put("category", m.getCategory());
        root.put("sourceScope", List.of("文档正文", "字段名/字段说明/表注释(多粒度片段)"));
        Map<String, Object> sm = new LinkedHashMap<>();
        sm.put("value", sourceMethod);
        sm.put("by", sourceBy);
        root.put("sourceMethod", sm);
        root.put("dataFeatures", new ArrayList<>(features));
        List<Map<String, String>> subjList = new ArrayList<>();
        for (Map.Entry<String, String> e : subjects.entrySet()) {
            Map<String, String> sj = new LinkedHashMap<>();
            sj.put("role", e.getKey());
            sj.put("name", e.getValue());
            sj.put("by", subjectBy.getOrDefault(e.getKey(), BY_RULE));
            subjList.add(sj);
        }
        root.put("subjects", subjList);
        List<Map<String, String>> consList = new ArrayList<>();
        for (Map.Entry<String, String> e : constraints.entrySet()) {
            Map<String, String> cs = new LinkedHashMap<>();
            cs.put("type", e.getKey());
            cs.put("value", e.getValue());
            cs.put("by", constraintBy.getOrDefault(e.getKey(), BY_RULE));
            consList.add(cs);
        }
        root.put("constraints", consList);
        root.put("downstream", downstream(features, sourceMethod, constraints, subjects));
        try {
            return OM.writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** #6 为分类分级/法律校验/授权判断提供输入。 */
    private Map<String, Object> downstream(Set<String> features, String sourceMethod,
                                           Map<String, String> constraints, Map<String, String> subjects) {
        Map<String, Object> d = new LinkedHashMap<>();
        String grade;
        if (features.contains("敏感个人信息")) {
            grade = "高(含敏感个人信息)";
        } else if (features.contains("个人信息") || features.contains("商业秘密") || features.contains("监管数据")) {
            grade = "较高";
        } else {
            grade = "一般";
        }
        d.put("classificationGrade", grade);
        List<String> legal = new ArrayList<>();
        if (features.contains("个人信息") || features.contains("敏感个人信息")) {
            legal.add("个人信息保护合规(告知同意/最小必要)");
        }
        if (features.contains("商业秘密")) {
            legal.add("商业秘密保护审查");
        }
        if (features.contains("监管数据")) {
            legal.add("监管数据合规审查");
        }
        if (subjects.containsKey(AitProfileSubject.R_SHARED)) {
            legal.add("对外共享合规审查");
        }
        if (legal.isEmpty()) {
            legal.add("常规合规审查");
        }
        d.put("legalCheckPoints", legal);
        Map<String, Object> auth = new LinkedHashMap<>();
        auth.put("sourceMethod", sourceMethod);
        auth.put("authScope", constraints.get(AitConstraint.T_AUTH_SCOPE));
        auth.put("shareLimit", constraints.get(AitConstraint.T_SHARE_LIMIT));
        d.put("authBasis", auth);
        return d;
    }

    private Map<String, Object> profileBrief(AitProfile p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("materialId", p.getMaterialId());
        row.put("level", p.getLevel());
        row.put("sourceMethod", p.getSourceMethod());
        row.put("dataFeatures", p.getDataFeatures());
        row.put("confidence", p.getConfidence());
        return row;
    }

    private static String cap(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
