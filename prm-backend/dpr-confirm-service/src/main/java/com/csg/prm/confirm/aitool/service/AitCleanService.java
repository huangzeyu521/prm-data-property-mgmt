package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.aitool.dto.AitCleanRequest;
import com.csg.prm.confirm.aitool.entity.AitAuditBase;
import com.csg.prm.confirm.aitool.entity.AitCleanLog;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseConfig;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.entity.AitTplCompare;
import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import com.csg.prm.confirm.aitool.mapper.AitAuditBaseMapper;
import com.csg.prm.confirm.aitool.mapper.AitCleanLogMapper;
import com.csg.prm.confirm.aitool.mapper.AitMaterialMapper;
import com.csg.prm.confirm.aitool.mapper.AitParseResultMapper;
import com.csg.prm.confirm.aitool.mapper.AitTplCompareMapper;
import com.csg.prm.confirm.aitool.term.AitTermLibrary;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 数据清洗与标准化服务(可研 1.2):对表名/字段名/字段说明/系统名/来源描述及确权枚举字段做
 * 去噪·归一·标准化(#1/#2);规则映射+术语库+模型语义归一 混合清洗(#3);抽取↔统一审核底表映射(#4);
 * 识别 缺失/冲突/异常/重复 + 待补正清单(#5);全程清洗日志可追溯(#6)。
 */
@Service
public class AitCleanService {

    enum Kind { TEXT, NAME, ENUM, BOOL }

    /** 统一模板字段(结构化审核底表口径)。 */
    record FieldSpec(String key, String label, Kind kind, String termField) {
    }

    private static final List<FieldSpec> TEMPLATE = List.of(
            new FieldSpec("tableName", "表名", Kind.NAME, null),
            new FieldSpec("fieldName", "字段名", Kind.NAME, null),
            new FieldSpec("fieldDesc", "字段说明", Kind.TEXT, null),
            new FieldSpec("systemName", "系统名称", Kind.TEXT, null),
            new FieldSpec("sourceDesc", "来源描述", Kind.TEXT, null),
            new FieldSpec("rightType", "权利类型", Kind.ENUM, AitTermLibrary.F_RIGHT_TYPE),
            new FieldSpec("dataSource", "数据来源", Kind.ENUM, AitTermLibrary.F_DATA_SOURCE),
            new FieldSpec("sensitiveType", "敏感类型", Kind.ENUM, AitTermLibrary.F_SENSITIVE),
            new FieldSpec("authScope", "授权范围", Kind.ENUM, AitTermLibrary.F_AUTH_SCOPE),
            new FieldSpec("secretLevel", "密级", Kind.ENUM, AitTermLibrary.F_SECRET_LEVEL),
            new FieldSpec("isPersonalInfo", "是否个人信息", Kind.BOOL, null),
            new FieldSpec("isShared", "是否共享", Kind.BOOL, null));

    /** 原始字段名(归一化后)→ 模板字段键(字段对齐 #3)。 */
    private static final Map<String, String> ALIAS = buildAlias();

    private static final Set<String> MISSING_TOKENS = Set.of(
            "-", "—", "n/a", "na", "null", "空", "/", "\\", "暂无", "待定", "待补充", "未知", "nil", "none");
    private static final Set<String> BOOL_TRUE = Set.of("是", "y", "yes", "true", "1", "有", "t", "√", "✓", "对", "真");
    private static final Set<String> BOOL_FALSE = Set.of("否", "n", "no", "false", "0", "无", "f", "×", "错", "假");

    private final AitMaterialMapper materialMapper;
    private final AitParseResultMapper parseMapper;
    private final AitCleanLogMapper logMapper;
    private final AitAuditBaseMapper auditMapper;
    private final AiToolParseGateway gateway;
    private final AitParseConfigService configService;
    private final AitTplCompareMapper tplCmpMapper;

    public AitCleanService(AitMaterialMapper materialMapper, AitParseResultMapper parseMapper,
                           AitCleanLogMapper logMapper, AitAuditBaseMapper auditMapper,
                           AiToolParseGateway gateway, AitParseConfigService configService,
                           AitTplCompareMapper tplCmpMapper) {
        this.materialMapper = materialMapper;
        this.parseMapper = parseMapper;
        this.logMapper = logMapper;
        this.auditMapper = auditMapper;
        this.gateway = gateway;
        this.configService = configService;
        this.tplCmpMapper = tplCmpMapper;
    }

    /** 1.4#4 管理员配置的额外字段映射(原始字段名 → 模板字段键),并入字段对齐。 */
    private Map<String, String> configAlias() {
        Map<String, String> extra = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, String> e : configService.fieldMapping(AitParseConfig.DEFAULT_SCENE).entrySet()) {
                extra.put(normKey(e.getKey()), e.getValue());
            }
        } catch (RuntimeException ignore) {
            // 配置不可用时仅用内置映射
        }
        return extra;
    }

    public record CleanStats(int rows, int fields, int ok, int missing, int conflict, int abnormal, int duplicate) {
    }

    public record CleanResult(String batchNo, CleanStats stats,
                              List<AitAuditBase> auditBase, List<AitCleanLog> logs, List<AitAuditBase> pending) {
    }

    @Transactional
    public CleanResult clean(String materialId, AitCleanRequest req) {
        AitMaterial m = materialMapper.selectById(materialId);
        if (m == null) {
            throw new BizException("材料不存在");
        }
        boolean useModel = req == null || req.isUseModel();
        List<Map<String, String>> rows = (req == null || req.getRows() == null || req.getRows().isEmpty())
                ? List.of(deriveRow(materialId, m)) : req.getRows();
        String batchNo = "CLEAN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String dtr = m.getDataTableRef();

        // 覆盖式:清掉该材料历史清洗结果
        logMapper.delete(new LambdaQueryWrapper<AitCleanLog>().eq(AitCleanLog::getMaterialId, materialId));
        auditMapper.delete(new LambdaQueryWrapper<AitAuditBase>().eq(AitAuditBase::getMaterialId, materialId));

        List<AitAuditBase> auditAll = new ArrayList<>();
        List<AitCleanLog> logAll = new ArrayList<>();
        Set<String> rowSigs = new HashSet<>();
        Map<String, String> extraAlias = configAlias();
        int rowNo = 0;
        for (Map<String, String> row : rows) {
            rowNo++;
            Map<String, Aligned> aligned = align(row, extraAlias);
            Map<String, String> rowClean = new LinkedHashMap<>();
            for (FieldSpec spec : TEMPLATE) {
                Aligned a = aligned.get(spec.key());
                if (a == null) {
                    auditAll.add(persistAudit(materialId, batchNo, rowNo, spec, null, "",
                            AitAuditBase.ST_MISSING, "字段缺失", "补充「" + spec.label() + "」", dtr));
                    continue;
                }
                if (a.conflict) {
                    auditAll.add(persistAudit(materialId, batchNo, rowNo, spec, a.rawValue, "",
                            AitAuditBase.ST_CONFLICT, "字段对齐冲突:" + a.conflictDesc, "核对并保留正确值", dtr));
                    logAll.add(persistLog(materialId, batchNo, rowNo, spec.key(), a.rawKey, a.rawValue,
                            "字段对齐(冲突)", "", AitCleanLog.METHOD_RULE));
                    continue;
                }
                FieldOutcome fo = cleanField(spec, a.rawValue, useModel);
                rowClean.put(spec.key(), fo.cleaned);
                auditAll.add(persistAudit(materialId, batchNo, rowNo, spec, a.rawValue, fo.cleaned,
                        fo.status, fo.issue, fo.suggestion, dtr));
                logAll.add(persistLog(materialId, batchNo, rowNo, spec.key(), a.rawKey, a.rawValue,
                        fo.rule, fo.cleaned, fo.method));
            }
            // 重复记录识别(#2):按清洗后核心字段签名
            String sig = norm(rowClean.get("tableName")) + "|" + norm(rowClean.get("fieldName"))
                    + "|" + norm(rowClean.get("systemName"));
            if (rowSigs.contains(sig) && StringUtils.hasText(sig.replace("|", ""))) {
                auditAll.add(persistAudit(materialId, batchNo, rowNo,
                        new FieldSpec("_record", "整行记录", Kind.TEXT, null), sig, "",
                        AitAuditBase.ST_DUPLICATE, "与已清洗记录重复", "删除重复记录", dtr));
            } else {
                rowSigs.add(sig);
            }
        }
        List<AitAuditBase> pending = auditAll.stream()
                .filter(a -> !AitAuditBase.ST_OK.equals(a.getStatus())).toList();
        CleanStats stats = stats(rows.size(), auditAll);
        return new CleanResult(batchNo, stats, auditAll, logAll, pending);
    }

    public List<AitAuditBase> auditBase(String materialId) {
        return auditMapper.selectList(new LambdaQueryWrapper<AitAuditBase>()
                .eq(AitAuditBase::getMaterialId, materialId)
                .orderByAsc(AitAuditBase::getRowNo));
    }

    public List<AitAuditBase> pending(String materialId) {
        return auditMapper.selectList(new LambdaQueryWrapper<AitAuditBase>()
                .eq(AitAuditBase::getMaterialId, materialId)
                .ne(AitAuditBase::getStatus, AitAuditBase.ST_OK)
                .orderByAsc(AitAuditBase::getRowNo));
    }

    public List<AitCleanLog> cleanLog(String materialId) {
        return logMapper.selectList(new LambdaQueryWrapper<AitCleanLog>()
                .eq(AitCleanLog::getMaterialId, materialId)
                .orderByAsc(AitCleanLog::getRowNo));
    }

    // ---- 清洗核心 ----

    record FieldOutcome(String cleaned, String rule, String method, String status, String issue, String suggestion) {
    }

    private FieldOutcome cleanField(FieldSpec spec, String rawValue, boolean useModel) {
        List<String> rules = new ArrayList<>();
        boolean ruleUsed = false;
        boolean modelUsed = false;
        String v = rawValue == null ? "" : rawValue;

        if (spec.kind() == Kind.BOOL) {
            String b = detectBool(v);
            if (b == null) {
                if (isMissing(v)) {
                    return new FieldOutcome("", "空值识别", AitCleanLog.METHOD_RULE,
                            AitAuditBase.ST_MISSING, "布尔项缺失", "补充「" + spec.label() + "」(是/否)");
                }
                return new FieldOutcome("", "布尔项识别", AitCleanLog.METHOD_RULE,
                        AitAuditBase.ST_ABNORMAL, "布尔值无法识别:" + v, "请填 是/否");
            }
            return new FieldOutcome(b, "布尔项识别", AitCleanLog.METHOD_RULE, AitAuditBase.ST_OK, null, null);
        }

        String cleaned = denoise(v);
        if (!cleaned.equals(v)) {
            rules.add("去噪/全半角/空白归一");
            ruleUsed = true;
        }
        v = cleaned;
        if (spec.kind() == Kind.NAME) {
            String n = normalizeName(v);
            if (!n.equals(v)) {
                rules.add("命名规范化");
                ruleUsed = true;
            }
            v = n;
        }
        if (isMissing(v)) {
            return new FieldOutcome("", joinRules(rules, "空值识别"), AitCleanLog.METHOD_RULE,
                    AitAuditBase.ST_MISSING, "值缺失", "补充「" + spec.label() + "」");
        }

        if (spec.kind() == Kind.ENUM) {
            AitTermLibrary.Match mt = AitTermLibrary.match(spec.termField(), v);
            if (mt.standard()) {
                return new FieldOutcome(mt.standardTerm(), joinRules(rules, "枚举命中标准"),
                        AitCleanLog.METHOD_RULE, AitAuditBase.ST_OK, null, null);
            }
            if (!"(待人工确认)".equals(mt.standardTerm())) {
                return new FieldOutcome(mt.standardTerm(), joinRules(rules, "术语归一(规则)"),
                        AitCleanLog.METHOD_RULE, AitAuditBase.ST_OK, null, null);
            }
            List<String> candidates = new ArrayList<>(AitTermLibrary.standardTerms(spec.termField()));
            if (useModel) {
                String mv = safeNormalize(spec.termField(), v, candidates);
                if (StringUtils.hasText(mv) && candidates.contains(mv)) {
                    rules.add("语义归一(模型)");
                    modelUsed = true;
                    return new FieldOutcome(mv, joinRules(rules), method(ruleUsed, modelUsed),
                            AitAuditBase.ST_OK, null, null);
                }
            }
            return new FieldOutcome(v, joinRules(rules, "枚举校验"), method(ruleUsed, modelUsed),
                    AitAuditBase.ST_ABNORMAL, "枚举越界,无法归一:" + v, "确认取值,标准值域:" + candidates);
        }

        // TEXT
        return new FieldOutcome(v, rules.isEmpty() ? "无需清洗" : joinRules(rules),
                AitCleanLog.METHOD_RULE, AitAuditBase.ST_OK, null, null);
    }

    private String safeNormalize(String field, String value, List<String> candidates) {
        try {
            return gateway.normalizeEnum(field, value, candidates);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---- 字段对齐 ----

    static final class Aligned {
        final String rawKey;
        final String rawValue;
        boolean conflict;
        String conflictDesc;

        Aligned(String rawKey, String rawValue) {
            this.rawKey = rawKey;
            this.rawValue = rawValue;
        }
    }

    private Map<String, Aligned> align(Map<String, String> row, Map<String, String> extraAlias) {
        Map<String, Aligned> out = new LinkedHashMap<>();
        if (row == null) {
            return out;
        }
        for (Map.Entry<String, String> e : row.entrySet()) {
            String nk = normKey(e.getKey());
            String tkey = ALIAS.get(nk);
            if (tkey == null && extraAlias != null) {
                tkey = extraAlias.get(nk); // 1.4#4 管理员配置的字段映射
            }
            if (tkey == null) {
                continue; // 未识别的原始字段不入模板
            }
            Aligned exist = out.get(tkey);
            if (exist == null) {
                out.put(tkey, new Aligned(e.getKey(), e.getValue()));
            } else if (!norm(exist.rawValue).equals(norm(e.getValue()))) {
                exist.conflict = true;
                exist.conflictDesc = exist.rawKey + "=" + exist.rawValue + " vs " + e.getKey() + "=" + e.getValue();
            }
        }
        return out;
    }

    /** rows 为空时,从材料解析结果派生一行待清洗记录。 */
    private Map<String, String> deriveRow(String materialId, AitMaterial m) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("表名", m.getDataTableRef() == null ? m.getFileName() : m.getDataTableRef());
        AitParseResult r = parseMapper.selectOne(
                new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, materialId));
        if (r != null) {
            row.put("字段名", r.getRightObject());
            row.put("权利类型", r.getRightType());
            row.put("数据来源", r.getDataSource());
            row.put("敏感类型", r.getSensitiveType());
            row.put("授权范围", r.getAuthScope());
            row.put("来源描述", r.getDataSource());
        }
        return row;
    }

    // ---- 持久化 ----

    private AitAuditBase persistAudit(String materialId, String batchNo, int rowNo, FieldSpec spec,
                                      String raw, String clean, String status, String issue,
                                      String suggestion, String dtr) {
        AitAuditBase a = new AitAuditBase();
        a.setMaterialId(materialId);
        a.setBatchNo(batchNo);
        a.setRowNo(rowNo);
        a.setTemplateField(spec.key());
        a.setFieldLabel(spec.label());
        a.setRawValue(raw);
        a.setCleanValue(clean);
        a.setStatus(status);
        a.setIssue(issue);
        a.setSuggestion(suggestion);
        a.setDataTableRef(dtr);
        auditMapper.insert(a);
        return a;
    }

    private AitCleanLog persistLog(String materialId, String batchNo, int rowNo, String field, String rawKey,
                                   String original, String rule, String cleaned, String method) {
        AitCleanLog log = new AitCleanLog();
        log.setMaterialId(materialId);
        log.setBatchNo(batchNo);
        log.setRowNo(rowNo);
        log.setField(field);
        log.setRawKey(rawKey);
        log.setOriginalValue(original);
        log.setRule(rule);
        log.setCleanedValue(cleaned);
        log.setMethod(method);
        logMapper.insert(log);
        return log;
    }

    private CleanStats stats(int rows, List<AitAuditBase> audit) {
        int ok = 0;
        int missing = 0;
        int conflict = 0;
        int abnormal = 0;
        int duplicate = 0;
        for (AitAuditBase a : audit) {
            switch (a.getStatus()) {
                case AitAuditBase.ST_OK -> ok++;
                case AitAuditBase.ST_MISSING -> missing++;
                case AitAuditBase.ST_CONFLICT -> conflict++;
                case AitAuditBase.ST_ABNORMAL -> abnormal++;
                case AitAuditBase.ST_DUPLICATE -> duplicate++;
                default -> { }
            }
        }
        return new CleanStats(rows, audit.size(), ok, missing, conflict, abnormal, duplicate);
    }

    // ---- 字符级清洗工具 ----

    /** 去噪:全角→半角、全角空格、零宽/控制字符剔除、空白折叠、首尾引号去除。 */
    static String denoise(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '　') {
                sb.append(' ');
            } else if (c >= 0xFF01 && c <= 0xFF5E) {
                sb.append((char) (c - 0xFEE0)); // 全角 ASCII → 半角
            } else if (c == '​' || c == '‌' || c == '‍' || c == '﻿') {
                // 零宽字符剔除
            } else if (c < 0x20 && c != '\n' && c != '\t') {
                // 控制字符剔除
            } else {
                sb.append(c);
            }
        }
        String r = sb.toString().replaceAll("[\\t\\n]+", " ").replaceAll(" {2,}", " ").trim();
        r = r.replaceAll("^[\"'「『]+", "").replaceAll("[\"'」』]+$", "").trim();
        return r;
    }

    /** 命名规范化:空白→下划线,去中文标点与括号。 */
    static String normalizeName(String v) {
        if (v == null) {
            return "";
        }
        return v.replaceAll("\\s+", "_").replaceAll("[，。、；：（）()【】\\[\\]]", "").trim();
    }

    private static String detectBool(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim().toLowerCase();
        if (BOOL_TRUE.contains(t)) {
            return "是";
        }
        if (BOOL_FALSE.contains(t)) {
            return "否";
        }
        return null;
    }

    private static boolean isMissing(String v) {
        if (!StringUtils.hasText(v)) {
            return true;
        }
        return MISSING_TOKENS.contains(v.trim().toLowerCase());
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normKey(String k) {
        return k == null ? "" : k.trim().toLowerCase().replaceAll("[\\s_\\-]", "");
    }

    private static String joinRules(List<String> rules, String... extra) {
        List<String> all = new ArrayList<>(rules);
        for (String e : extra) {
            all.add(e);
        }
        return all.isEmpty() ? "无需清洗" : String.join(" + ", all);
    }

    private static String method(boolean ruleUsed, boolean modelUsed) {
        if (ruleUsed && modelUsed) {
            return AitCleanLog.METHOD_HYBRID;
        }
        return modelUsed ? AitCleanLog.METHOD_MODEL : AitCleanLog.METHOD_RULE;
    }

    private static Map<String, String> buildAlias() {
        Map<String, String> m = new LinkedHashMap<>();
        put(m, "tableName", "表名", "数据表", "数据表名", "表名称", "table", "tablename", "表");
        put(m, "fieldName", "字段名", "列名", "字段", "字段名称", "field", "fieldname", "column");
        put(m, "fieldDesc", "字段说明", "字段描述", "注释", "说明", "comment", "字段注释", "描述");
        put(m, "systemName", "系统名称", "系统", "来源系统", "system", "systemname", "业务系统");
        put(m, "sourceDesc", "来源描述", "数据来源说明", "来源说明", "来源", "source", "sourcedesc");
        put(m, "rightType", "权利类型", "权属类型", "righttype");
        put(m, "dataSource", "数据来源", "datasource", "来源类型");
        put(m, "sensitiveType", "敏感类型", "敏感级别", "sensitive", "敏感");
        put(m, "authScope", "授权范围", "范围", "authscope");
        put(m, "secretLevel", "密级", "保密级别", "secret", "secretlevel");
        put(m, "isPersonalInfo", "是否个人信息", "个人信息", "personalinfo", "含个人信息");
        put(m, "isShared", "是否共享", "共享", "shared", "是否对外共享");
        return m;
    }

    private static void put(Map<String, String> m, String tkey, String... aliases) {
        for (String a : aliases) {
            m.put(normKey(a), tkey);
        }
    }

    // ============ 1.1.1.1#4 结构化模板上传 → 与材料抽取内容自动关联审核 → 对比日志 ============

    public record TplCompareResult(String batchNo, String templateName, int matched, int mismatched, int missing,
                                   List<AitTplCompare> rows) {
    }

    /**
     * 上传结构化模板与材料抽取内容自动关联审核,生成对比日志(支持多模板累积)。
     * 模板解析:Excel(字段|值 两列或 表头+数据行) / Word(字段:值 行) / 文本(字段:值)。
     */
    @Transactional
    public TplCompareResult templateCompare(String materialId, String templateName, byte[] data) {
        AitMaterial m = materialMapper.selectById(materialId);
        if (m == null) {
            throw new BizException("材料不存在");
        }
        if (data == null || data.length == 0) {
            throw new BizException("模板文件为空");
        }
        Map<String, String> tpl = parseTemplate(templateName, data);
        if (tpl.isEmpty()) {
            throw new BizException("未能从模板解析出“字段:值”,请检查模板格式(Excel两列/Word或文本“字段:值”)");
        }
        Map<String, String> matVals = materialFieldValues(materialId, m);
        String content = m.getContent() == null ? "" : m.getContent();
        String batchNo = "TPL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        int rowNo = 0;
        int matched = 0;
        int mismatched = 0;
        int missing = 0;
        List<AitTplCompare> rows = new ArrayList<>();
        for (Map.Entry<String, String> e : tpl.entrySet()) {
            rowNo++;
            String tplField = e.getKey();
            String tplValue = e.getValue();
            String matVal = matchMaterialValue(tplField, matVals);
            String consistency;
            if (!StringUtils.hasText(matVal)) {
                consistency = AitTplCompare.C_MISSING;
                missing++;
            } else if (norm(matVal).contains(norm(tplValue)) || norm(tplValue).contains(norm(matVal))) {
                consistency = AitTplCompare.C_MATCH;
                matched++;
            } else {
                consistency = AitTplCompare.C_MISMATCH;
                mismatched++;
            }
            AitTplCompare c = new AitTplCompare();
            c.setMaterialId(materialId);
            c.setBatchNo(batchNo);
            c.setTemplateName(templateName);
            c.setRowNo(rowNo);
            c.setTplField(tplField);
            c.setTplValue(tplValue);
            c.setMaterialValue(matVal);
            c.setConsistency(consistency);
            c.setSourceLocation(locateInContent(content, StringUtils.hasText(matVal) ? matVal : tplValue));
            tplCmpMapper.insert(c);
            rows.add(c);
        }
        return new TplCompareResult(batchNo, templateName, matched, mismatched, missing, rows);
    }

    public List<AitTplCompare> templateCompareLog(String materialId) {
        return tplCmpMapper.selectList(new LambdaQueryWrapper<AitTplCompare>()
                .eq(AitTplCompare::getMaterialId, materialId)
                .orderByAsc(AitTplCompare::getCreateTime).orderByAsc(AitTplCompare::getRowNo));
    }

    /** 对比结果下载(CSV,UTF-8 BOM,Excel 友好)。 */
    public byte[] exportTemplateCompare(String materialId) {
        List<AitTplCompare> list = templateCompareLog(materialId);
        StringBuilder sb = new StringBuilder("﻿模板名称,模板字段,模板值,材料抽取值,一致性,材料中所在位置\n");
        for (AitTplCompare c : list) {
            sb.append(csv(c.getTemplateName())).append(',').append(csv(c.getTplField())).append(',')
                    .append(csv(c.getTplValue())).append(',').append(csv(c.getMaterialValue())).append(',')
                    .append(csv(c.getConsistency())).append(',').append(csv(c.getSourceLocation())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        return (v.contains(",") || v.contains("\"") || v.contains("\n")) ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    /** 材料抽取的字段→值(来自解析结果 + 清洗审核底表),键统一归一化便于匹配。 */
    private Map<String, String> materialFieldValues(String materialId, AitMaterial m) {
        Map<String, String> v = new LinkedHashMap<>();
        AitParseResult r = parseMapper.selectOne(
                new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, materialId));
        if (r != null) {
            v.put(normKey("权利主体"), r.getRightSubject());
            v.put(normKey("权利客体"), r.getRightObject());
            v.put(normKey("权利类型"), r.getRightType());
            v.put(normKey("权利期限"), r.getRightTerm());
            v.put(normKey("授权范围"), r.getAuthScope());
            v.put(normKey("数据来源"), r.getDataSource());
            v.put(normKey("敏感类型"), r.getSensitiveType());
            v.put(normKey("印章真伪"), r.getSealValid());
        }
        v.put(normKey("表名"), m.getDataTableRef() == null ? m.getFileName() : m.getDataTableRef());
        // 叠加清洗审核底表的字段级清洗后值
        for (AitAuditBase a : auditMapper.selectList(new LambdaQueryWrapper<AitAuditBase>()
                .eq(AitAuditBase::getMaterialId, materialId))) {
            if (StringUtils.hasText(a.getCleanValue())) {
                v.putIfAbsent(normKey(a.getFieldLabel()), a.getCleanValue());
            }
        }
        v.values().removeIf(x -> !StringUtils.hasText(x));
        return v;
    }

    /** 模板字段名 → 材料值:先走清洗别名表归一,再按归一键匹配。 */
    private String matchMaterialValue(String tplField, Map<String, String> matVals) {
        String nk = normKey(tplField);
        if (matVals.containsKey(nk)) {
            return matVals.get(nk);
        }
        String alias = ALIAS.get(nk);  // 别名→模板键(tableName/rightType/dataSource...)
        if (alias != null) {
            // 别名键再映回中文标准字段名探测
            for (Map.Entry<String, String> e : matVals.entrySet()) {
                if (e.getKey().equals(nk)) {
                    return e.getValue();
                }
            }
        }
        // 模糊:键互相包含
        for (Map.Entry<String, String> e : matVals.entrySet()) {
            if (e.getKey().contains(nk) || nk.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String locateInContent(String content, String value) {
        if (!StringUtils.hasText(content) || !StringUtils.hasText(value)) {
            return "未在材料正文中定位";
        }
        int idx = content.indexOf(value.trim());
        if (idx < 0) {
            return "未在材料正文中定位";
        }
        int from = Math.max(0, idx - 10);
        int to = Math.min(content.length(), idx + value.trim().length() + 10);
        return "「" + (from > 0 ? "…" : "") + content.substring(from, to).replaceAll("\\s+", " ")
                + (to < content.length() ? "…" : "") + "」(偏移 " + idx + ")";
    }

    /** 解析结构化模板为 字段→值。 */
    private Map<String, String> parseTemplate(String fileName, byte[] data) {
        String ext = fileName == null ? "" : fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        Map<String, String> out = new LinkedHashMap<>();
        try {
            if ("xls".equals(ext) || "xlsx".equals(ext)) {
                DataFormatter fmt = new DataFormatter();
                try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data))) {
                    for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                        Sheet sheet = wb.getSheetAt(si);
                        for (Row row : sheet) {
                            String k = null;
                            String val = null;
                            int ci = 0;
                            for (Cell cell : row) {
                                String cv = fmt.formatCellValue(cell).trim();
                                if (ci == 0) {
                                    k = cv;
                                } else if (ci == 1) {
                                    val = cv;
                                }
                                ci++;
                            }
                            if (StringUtils.hasText(k) && StringUtils.hasText(val) && !"字段".equals(k)) {
                                out.put(k, val);
                            }
                        }
                    }
                }
            } else if ("docx".equals(ext)) {
                try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data))) {
                    for (XWPFParagraph p : doc.getParagraphs()) {
                        addKv(out, p.getText());
                    }
                    for (XWPFTable t : doc.getTables()) {
                        for (XWPFTableRow row : t.getRows()) {
                            var cells = row.getTableCells();
                            if (cells.size() >= 2) {
                                String k = cells.get(0).getText().trim();
                                String val = cells.get(1).getText().trim();
                                if (StringUtils.hasText(k) && StringUtils.hasText(val) && !"字段".equals(k)) {
                                    out.put(k, val);
                                }
                            }
                        }
                    }
                }
            } else {
                String text = new String(data, StandardCharsets.UTF_8);
                for (String line : text.split("\\r?\\n")) {
                    addKv(out, line);
                }
            }
        } catch (Exception e) {
            // 解析失败返回已得到的键值(可能为空 → 上层提示)
        }
        return out;
    }

    private static void addKv(Map<String, String> out, String line) {
        if (!StringUtils.hasText(line)) {
            return;
        }
        java.util.regex.Matcher mt = java.util.regex.Pattern.compile("^\\s*([^:：]{1,30})[:：]\\s*(.+?)\\s*$").matcher(line);
        if (mt.find()) {
            out.put(mt.group(1).trim(), mt.group(2).trim());
        }
    }
}
