package com.csg.prm.confirm.ai;

import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final DawatAiGateway ai;
    private final ConfirmApplyService applyService;
    private final ConfirmMaterialService materialService;

    public ConfirmAiService(DawatAiGateway ai, ConfirmApplyService applyService,
                            ConfirmMaterialService materialService) {
        this.ai = ai;
        this.applyService = applyService;
        this.materialService = materialService;
    }

    /** 智能解析:对申请已上传材料逐份抽权属要素(OCR权属)+ 敏感判定 + 内容指纹(SM3)查重。 */
    public ParseResult parse(String applyId) {
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
        return new ParseResult(items, mats.size(), dup, sens, summary);
    }

    /** 权属冲突识别:对申请权属主张做冲突/重复确权排查。 */
    public DawatAiGateway.ConflictResult conflict(String applyId) {
        ConfirmApply a = mustApply(applyId);
        return ai.detectConflict(a.getAssetId(), a.getRightHolder(), a.getRightType());
    }

    /** AI 决策研判:综合 冲突检测 + 法规 RAG + 材料齐全度 → 预测/需补材料/待处理冲突/评分。 */
    public DecisionResult decision(String applyId) {
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
        return new DecisionResult(prediction, score, aiPrediction, supplement, conflicts, basis);
    }

    private ConfirmApply mustApply(String applyId) {
        ConfirmApply a = applyService.getById(applyId);
        if (a == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "确权申请不存在");
        }
        return a;
    }
}
