package com.csg.prm.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 解析 Qwen(大瓦特模拟)返回的 JSON 内容为四项能力的结构化结果。
 * 纯解析逻辑,可离线单测;解析失败抛出 {@link IllegalArgumentException} 由网关回退本地桩。
 */
final class QwenResponseParser {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final String HOLD = "持有权";
    private static final String USE = "使用权";
    private static final String OPERATE = "经营权";

    private QwenResponseParser() {
    }

    static DawatAiGateway.OcrOwnership ownership(String content) {
        JsonNode n = parse(content);
        return new DawatAiGateway.OcrOwnership(
                text(n, "assetName", "未识别资产"),
                text(n, "rightHolder", "中国南方电网有限责任公司"),
                normalizeRight(text(n, "rightType", HOLD)),
                text(n, "respDept", "数字化部"),
                confidence(n),
                text(n, "rawText", "[Qwen] 已抽取权属要素"));
    }

    static DawatAiGateway.ConflictResult conflict(String content) {
        JsonNode n = parse(content);
        return new DawatAiGateway.ConflictResult(
                n.path("hasConflict").asBoolean(false),
                text(n, "riskLevel", "低"),
                stringList(n.path("conflicts")),
                text(n, "suggestion", ""));
    }

    static DawatAiGateway.AuthIntent intent(String content) {
        JsonNode n = parse(content);
        return new DawatAiGateway.AuthIntent(
                text(n, "granteeOrg", "待明确被授权方"),
                normalizeRight(text(n, "rightType", USE)),
                text(n, "scenario", "数据应用"),
                text(n, "scope", "约定字段"),
                text(n, "mode", "一事一议"),
                text(n, "suggestion", ""),
                confidence(n));
    }

    static DawatAiGateway.RagAnswer rag(String content) {
        JsonNode n = parse(content);
        return new DawatAiGateway.RagAnswer(
                text(n, "answer", ""),
                stringList(n.path("citations")),
                confidence(n));
    }

    /** 提取首个 { 到末个 } 之间的 JSON 主体(容忍 ```json 代码块/前后缀文本) */
    private static JsonNode parse(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Qwen 返回为空");
        }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("Qwen 返回非 JSON: " + content);
        }
        try {
            return OM.readTree(content.substring(start, end + 1));
        } catch (Exception e) {
            throw new IllegalArgumentException("Qwen JSON 解析失败", e);
        }
    }

    private static String normalizeRight(String rt) {
        if (rt == null) {
            return HOLD;
        }
        if (rt.contains("经营")) {
            return OPERATE;
        }
        if (rt.contains("加工") || rt.contains("使用")) {
            return USE;
        }
        if (rt.contains("持有")) {
            return HOLD;
        }
        return rt;
    }

    private static String text(JsonNode n, String field, String dft) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? dft : v.asText();
    }

    private static double confidence(JsonNode n) {
        double c = n.path("confidence").asDouble(0.8);
        if (c < 0) {
            return 0;
        }
        return c > 1 ? 1 : c;
    }

    private static List<String> stringList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(e -> out.add(e.asText()));
        }
        return out;
    }
}
