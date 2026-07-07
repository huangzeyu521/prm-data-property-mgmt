package com.csg.prm.common.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 大瓦特 AI 网关 —— 基于阿里云百炼 qwen3.7-max-2026-06-08(OpenAI 兼容接口)的真实实现,模拟大瓦特 AI 平台。
 * 仅当 prm.ai.provider=qwen 时启用并 @Primary 覆盖本地桩;调用失败/超时自动回退 {@link LocalDawatAiGateway}。
 * 密钥仅从环境变量(prm.ai.api-key -> ${DASHSCOPE_API_KEY})读取,绝不硬编码。
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.ai.provider", havingValue = "qwen")
public class QwenDawatAiGateway implements DawatAiGateway {

    private static final Logger log = LoggerFactory.getLogger(QwenDawatAiGateway.class);

    private final LocalDawatAiGateway fallback;
    private final RestClient client;
    private final String model;
    private final String apiKey;

    @Override
    public String modelName() {
        return StringUtils.hasText(apiKey) ? model : "local-rule-stub(qwen-fallback)";
    }

    public QwenDawatAiGateway(LocalDawatAiGateway fallback,
                              @Value("${prm.ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
                              @Value("${prm.ai.model:qwen3.7-max-2026-06-08}") String model,
                              @Value("${prm.ai.api-key:}") String apiKey) {
        this.fallback = fallback;
        this.model = model;
        this.apiKey = apiKey;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        if (!StringUtils.hasText(apiKey)) {
            log.warn("[大瓦特AI] prm.ai.provider=qwen 但 api-key 为空(未设置 DASHSCOPE_API_KEY),将回退本地桩");
        } else {
            log.info("[大瓦特AI] 已启用 qwen 实现,model={}", model);
        }
    }

    @Override
    public OcrOwnership recognizeOwnership(String fileUrl) {
        try {
            String c = chat("你是数据确权权属识别助手。依据权属证明材料名称/URL 推断权属要素。",
                    "材料:" + fileUrl + "\n仅输出JSON,字段:assetName(资产名称),rightHolder(权属人),"
                            + "rightType(持有权/使用权/经营权),respDept(责任部门),"
                            + "confidence(0-1小数),rawText(简要说明)。不要输出多余文本。");
            return QwenResponseParser.ownership(c);
        } catch (RuntimeException e) {
            log.warn("[大瓦特AI] recognizeOwnership 调用失败,回退本地桩: {}", e.getMessage());
            return fallback.recognizeOwnership(fileUrl);
        }
    }

    @Override
    public ConflictResult detectConflict(String assetId, String rightHolder, String rightType) {
        try {
            String c = chat("你是数据权属冲突检测助手,排查重复确权与权属边界重叠。",
                    "资产ID:" + assetId + ",权属人:" + rightHolder + ",权属类型:" + rightType
                            + "\n仅输出JSON,字段:hasConflict(true/false),riskLevel(低/中/高),"
                            + "conflicts(字符串数组),suggestion(处置建议)。不要输出多余文本。");
            return QwenResponseParser.conflict(c);
        } catch (RuntimeException e) {
            log.warn("[大瓦特AI] detectConflict 调用失败,回退本地桩: {}", e.getMessage());
            return fallback.detectConflict(assetId, rightHolder, rightType);
        }
    }

    @Override
    public AuthIntent recognizeAuthIntent(String text) {
        try {
            String c = chat("你是数据授权意图识别助手,从自然语言申请抽取授权要素。",
                    "申请:" + text + "\n仅输出JSON,字段:granteeOrg(被授权方),"
                            + "rightType(持有权/使用权/经营权),scenario(场景),scope(范围),"
                            + "mode(一事一议/批量),suggestion(建议),confidence(0-1小数)。不要输出多余文本。");
            return QwenResponseParser.intent(c);
        } catch (RuntimeException e) {
            log.warn("[大瓦特AI] recognizeAuthIntent 调用失败,回退本地桩: {}", e.getMessage());
            return fallback.recognizeAuthIntent(text);
        }
    }

    @Override
    public RagAnswer ask(String question) {
        try {
            String c = chat("你是中国南方电网数据确权授权业务助手,依据《数据确权授权业务指导书》(附录F)"
                            + "及三权分置、先确后授等原则作答,务必专业准确。",
                    "问题:" + question + "\n仅输出JSON,字段:answer(回答),"
                            + "citations(引用条目字符串数组,如\"附录F 4.1\"),confidence(0-1小数)。不要输出多余文本。");
            return QwenResponseParser.rag(c);
        } catch (RuntimeException e) {
            log.warn("[大瓦特AI] ask 调用失败,回退本地桩: {}", e.getMessage());
            return fallback.ask(question);
        }
    }

    @Override
    public String reviewAuthMaterials(String context) {
        try {
            return chat("你是中国南方电网数据授权材料合规校验专家,依据《数据确权授权业务指导书》附录D/E,逐份校验授权申请材料:"
                            + "①要素与申请表单一致性;②保密承诺函/信息授权协议是否覆盖授权场景;③盖章与必备表述完整性。"
                            + "仅输出严格JSON:{\"overall\":\"通过|不通过|存疑\",\"overallDesc\":\"...\","
                            + "\"items\":[{\"materialName\":\"...\",\"verdict\":\"通过|不通过|存疑\",\"issues\":\"...\",\"suggestion\":\"...\"}]},不要输出多余文本。",
                    context);
        } catch (RuntimeException e) {
            log.warn("[大瓦特AI] reviewAuthMaterials 调用失败,回退本地桩: {}", e.getMessage());
            return fallback.reviewAuthMaterials(context);
        }
    }

    @Override
    public String preReviewAuth(String context) {
        try {
            return chat("你是数据授权合规预审专家。基于给定的规则校验结果与申请上下文,输出一段简洁的预审意见"
                            + "(是否同意提交、风险提示、先确后授边界与对外开放目录提醒),100字以内,直接输出文本。",
                    context);
        } catch (RuntimeException e) {
            log.warn("[大瓦特AI] preReviewAuth 调用失败,回退本地桩: {}", e.getMessage());
            return fallback.preReviewAuth(context);
        }
    }

    @Override
    public String parseBatchIntent(String text) {
        try {
            return chat("你是批量数据授权意图解析助手。从自然语言中解析共享字段与多条明细。"
                            + "仅输出严格JSON:{\"granteeOrg\":\"被授权方\",\"rightType\":\"持有权/使用权/经营权\","
                            + "\"scenario\":\"场景\",\"items\":[{\"assetName\":\"数据资产名\"}]},不要输出多余文本。",
                    "申请描述:" + text);
        } catch (RuntimeException e) {
            log.warn("[大瓦特AI] parseBatchIntent 调用失败,回退本地桩: {}", e.getMessage());
            return fallback.parseBatchIntent(text);
        }
    }

    private String chat(String system, String user) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("DASHSCOPE_API_KEY 未配置");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)));
        JsonNode resp = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        if (resp == null || !resp.has("choices") || resp.path("choices").isEmpty()) {
            throw new IllegalStateException("Qwen 响应无 choices");
        }
        return resp.path("choices").get(0).path("message").path("content").asText();
    }
}
