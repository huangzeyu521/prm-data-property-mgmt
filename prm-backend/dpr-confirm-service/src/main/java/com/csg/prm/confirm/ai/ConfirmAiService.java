package com.csg.prm.confirm.ai;

import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.common.aitrace.AiRunLog;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import com.csg.prm.confirm.entity.ConfirmMaterialRule;
import com.csg.prm.common.aitrace.AiRunLogService;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmMaterialRuleService;
import com.csg.prm.confirm.service.ConfirmMaterialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 确权内生 AI 能力(智能解析 / 决策研判 / 权属冲突识别)。
 * 统一走 prm-common 共享网关 {@link DawatAiGateway}(qwen3-max 真调 / Local 桩),
 * 与独立"智能确权辅助工具"共享同一 AI 内核但不依赖其包、不跨部署调用——PRM 自包含。
 */
@Service
public class ConfirmAiService {

    /** 轻量敏感判定关键词(深度版面/印章判定由独立工具承接) */
    private static final List<String> SENSITIVE_KW = List.of(
            "身份证", "手机号", "电话", "住址", "隐私", "商业秘密", "密级", "核心商密", "敏感信息");

    /** 单份材料解析要素 */
    public record MaterialElement(String materialName, String assetName, String rightHolder,
                                  String rightType, double confidence, boolean sensitiveHit,
                                  String contentHash, String duplicateOf) {
    }

    /** 智能解析结果 */
    public record ParseResult(List<MaterialElement> items, int total, int duplicates,
                              int sensitive, String summary) {
    }

    /** 决策研判结果(预测/需补材料/待处理冲突/评分) */
    public record DecisionResult(String prediction, int score, String aiPrediction,
                                 List<String> supplementMaterials, List<String> pendingConflicts,
                                 String basis) {
    }

    /** 单条应交项的校验逻辑可视化(规则明细 + 判定依据) */
    public record CheckLogicItem(String code, String materialName, String triggerType, String triggerLabel,
                                 String ruleDetail, String required, boolean materialPresent,
                                 String aiVerdict, String aiIssues) {
    }

    /** 校验逻辑可视化结果:逐应交项的规则明细 + AI 判定依据(供人工预审透明复核) */
    public record CheckLogic(List<CheckLogicItem> items, String aiModel, String summary) {
    }

    private final DawatAiGateway ai;
    private final ConfirmApplyService applyService;
    private final ConfirmMaterialService materialService;
    private final AiRunLogService runLogService;
    private final ConfirmMaterialRuleService ruleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfirmAiService(DawatAiGateway ai, ConfirmApplyService applyService,
                            ConfirmMaterialService materialService,
                            AiRunLogService runLogService,
                            ConfirmMaterialRuleService ruleService) {
        this.ai = ai;
        this.applyService = applyService;
        this.materialService = materialService;
        this.runLogService = runLogService;
        this.ruleService = ruleService;
    }

    /**
     * 校验规则可视化(南网§1):逐应交项展示「校验逻辑(触发规则)+ 规则明细 + 判定依据(AI 结论/问题)」。
     * 规则取可配置应交规则单一真源(IM_CONFIRM_MATERIAL_RULE);判定依据取该申请最近一次材料 AI 校验留痕。
     */
    public CheckLogic checkLogic(String applyId) {
        ConfirmApply a = mustApply(applyId);
        List<ConfirmMaterialRule> rules = ruleService.requiredRules(a);
        Set<String> present = new LinkedHashSet<>();
        for (ConfirmMaterial m : materialService.listByApply(applyId)) {
            present.add(m.getMaterialName());
        }
        Map<String, String[]> aiByName = latestMaterialCheckVerdicts(applyId); // name -> [verdict, issues]

        List<CheckLogicItem> items = new ArrayList<>();
        for (ConfirmMaterialRule r : rules) {
            String[] verdict = aiByName.get(r.getMaterialName());
            String code = StringUtils.hasText(r.getTriggerCode()) ? r.getTriggerCode()
                    : (ConfirmMaterialRule.T_TABLE2.equals(r.getTriggerType()) ? "表2" : "核心");
            items.add(new CheckLogicItem(code, r.getMaterialName(), r.getTriggerType(),
                    triggerLabelOf(r), r.getDetail(), r.getRequired(),
                    present.contains(r.getMaterialName()),
                    verdict == null ? null : verdict[0], verdict == null ? null : verdict[1]));
        }
        long present0 = items.stream().filter(CheckLogicItem::materialPresent).count();
        String summary = "应交 " + items.size() + " 项,已交 " + present0
                + ",其中含 AI 判定 " + aiByName.size() + " 项;规则源自可配置应交清单(单一真源),判定依据取最近一次材料 AI 校验留痕。";
        return new CheckLogic(items, ai.modelName(), summary);
    }

    /** 触发规则的人读标签(校验逻辑):ALWAYS 常交 / TABLE2 涉三方 / SOURCE-A–F / RELATION-G–J。 */
    private String triggerLabelOf(ConfirmMaterialRule r) {
        return switch (r.getTriggerType()) {
            case ConfirmMaterialRule.T_ALWAYS -> "始终必交(核心表单/凭证)";
            case ConfirmMaterialRule.T_TABLE2 -> "涉及第三方权益(B–J)时必交";
            case ConfirmMaterialRule.T_SOURCE -> "数据来源识别命中 " + r.getTriggerCode()
                    + (r.getTriggerLabel() == null ? "" : "(" + r.getTriggerLabel() + ")");
            case ConfirmMaterialRule.T_RELATION -> "信息关联识别命中 " + r.getTriggerCode()
                    + (r.getTriggerLabel() == null ? "" : "(" + r.getTriggerLabel() + ")");
            default -> r.getTriggerType();
        };
    }

    /** 取该申请最近一次"材料校验"AI 留痕,解析逐材料的 verdict/issues。无则空。 */
    private Map<String, String[]> latestMaterialCheckVerdicts(String applyId) {
        Map<String, String[]> out = new HashMap<>();
        List<AiRunLog> logs = runLogService.listByBiz(applyId);
        AiRunLog last = null;
        for (AiRunLog l : logs) {
            if (AiRunLog.CAP_MATERIAL_CHECK.equals(l.getCapability())) {
                last = l; // listByBiz 升序,循环结束 last 即最近一次
            }
        }
        if (last == null || !StringUtils.hasText(last.getOutput())) {
            return out;
        }
        try {
            JsonNode root = objectMapper.readTree(last.getOutput());
            JsonNode arr = root.get("items");
            if (arr != null && arr.isArray()) {
                for (JsonNode it : arr) {
                    String name = it.path("materialName").asText(null);
                    if (name != null) {
                        out.put(name, new String[]{
                                it.path("verdict").asText(null),
                                it.path("issues").asText(null)});
                    }
                }
            }
        } catch (Exception ignore) {
            // 输出非预期结构:判定依据留空,不影响规则展示
        }
        return out;
    }

    /** 留痕一次 AI 调用(序列化结果为输出);失败不影响主流程。 */
    private void traceLog(String applyId, String capability, String inputSummary, Object result, long durationMs) {
        String out;
        try {
            out = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            out = String.valueOf(result);
        }
        runLogService.record(AiRunLog.BIZ_CONFIRM, applyId, capability, ai.modelName(), inputSummary, out, durationMs);
    }

    /** 智能解析:对申请已上传材料逐份抽权属要素(OCR权属)+ 敏感判定 + 内容指纹(SM3)查重。 */
    public ParseResult parse(String applyId) {
        long t0 = System.currentTimeMillis();
        ConfirmApply apply = mustApply(applyId);
        List<ConfirmMaterial> mats = materialService.listByApply(applyId);
        if (mats.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "尚未上传材料,无法智能解析");
        }
        List<MaterialElement> items = new ArrayList<>();
        Map<String, String> seen = new HashMap<>();
        int dup = 0, sens = 0;
        for (ConfirmMaterial m : mats) {
            String text = materialService.materialText(m.getMaterialId());
            String ref = StringUtils.hasText(m.getFileUrl()) ? m.getFileUrl() : m.getFileName();
            DawatAiGateway.OcrOwnership o = ai.recognizeOwnership(ref);
            boolean sensitive = StringUtils.hasText(text) && SENSITIVE_KW.stream().anyMatch(text::contains);
            if (sensitive) {
                sens++;
            }
            String hash = StringUtils.hasText(text) ? Sm3Util.hashHex(text) : null;
            String duplicateOf = null;
            if (hash != null) {
                if (seen.containsKey(hash)) {
                    duplicateOf = seen.get(hash);
                    dup++;
                } else {
                    seen.put(hash, m.getMaterialName());
                }
            }
            items.add(new MaterialElement(m.getMaterialName(),
                    o == null ? null : o.assetName(), o == null ? null : o.rightHolder(),
                    o == null ? null : o.rightType(), o == null ? 0d : o.confidence(),
                    sensitive, hash, duplicateOf));
        }
        String summary = "解析 " + mats.size() + " 份材料:识别权属要素 " + items.size()
                + " 项,敏感 " + sens + " 份,疑似重复 " + dup + " 份";
        ParseResult result = new ParseResult(items, mats.size(), dup, sens, summary);
        traceLog(applyId, AiRunLog.CAP_PARSE,
                "资产:" + apply.getAssetName() + ";材料 " + mats.size() + " 份",
                result, System.currentTimeMillis() - t0);
        return result;
    }

    /** 权属冲突识别:对申请权属主张做冲突/重复确权排查。 */
    public DawatAiGateway.ConflictResult conflict(String applyId) {
        long t0 = System.currentTimeMillis();
        ConfirmApply a = mustApply(applyId);
        DawatAiGateway.ConflictResult result = ai.detectConflict(a.getAssetId(), a.getRightHolder(), a.getRightType());
        traceLog(applyId, AiRunLog.CAP_CONFLICT,
                "资产:" + a.getAssetId() + ";主体:" + a.getRightHolder() + ";权类:" + a.getRightType(),
                result, System.currentTimeMillis() - t0);
        return result;
    }

    /** AI 决策研判:综合 冲突检测 + 法规 RAG + 材料齐全度 → 预测/需补材料/待处理冲突/评分。 */
    public DecisionResult decision(String applyId) {
        long t0 = System.currentTimeMillis();
        ConfirmApply a = mustApply(applyId);
        DawatAiGateway.ConflictResult c = ai.detectConflict(a.getAssetId(), a.getRightHolder(), a.getRightType());
        DawatAiGateway.RagAnswer rag = ai.ask("确权" + (a.getRightType() == null ? "" : a.getRightType()) + "的合规要点与应交材料?");
        List<ConfirmMaterial> mats = materialService.listByApply(applyId);

        List<String> supplement = new ArrayList<>();
        if (mats.isEmpty()) {
            supplement.add("尚未上传任何材料");
        }
        List<String> conflicts = (c != null && c.hasConflict() && c.conflicts() != null)
                ? new ArrayList<>(c.conflicts()) : new ArrayList<>();

        int score = 60 + (mats.isEmpty() ? 0 : 20) + (conflicts.isEmpty() ? 20 : 0);
        String prediction = (conflicts.isEmpty() && !mats.isEmpty()) ? "建议通过" : "建议补充";
        String aiPrediction = rag == null ? "未生成" : rag.answer();
        String basis = "冲突检测:" + (c == null ? "无" : c.riskLevel())
                + ";法规RAG置信:" + (rag == null ? "无" : rag.confidence())
                + ";已上传材料:" + mats.size() + " 份";
        DecisionResult result = new DecisionResult(prediction, score, aiPrediction, supplement, conflicts, basis);
        traceLog(applyId, AiRunLog.CAP_DECISION,
                "资产:" + a.getAssetName() + ";权类:" + a.getRightType() + ";材料 " + mats.size() + " 份",
                result, System.currentTimeMillis() - t0);
        return result;
    }

    private ConfirmApply mustApply(String applyId) {
        ConfirmApply a = applyService.getById(applyId);
        if (a == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "确权申请不存在");
        }
        return a;
    }
}
