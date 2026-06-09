package com.csg.prm.confirm.aitool.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 文档解析网关 —— 真实调用大瓦特(以阿里云百炼 qwen3-max 模拟,OpenAI 兼容)做确权要素抽取。
 * 仅当 prm.ai.provider=qwen 时启用并 @Primary 覆盖本地桩;调用失败/超时自动回退 {@link LocalAiToolParseGateway}。
 * 密钥仅从环境变量(prm.ai.api-key -> ${DASHSCOPE_API_KEY})读取,绝不硬编码。
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.ai.provider", havingValue = "qwen")
public class QwenAiToolParseGateway implements AiToolParseGateway {

    private static final Logger log = LoggerFactory.getLogger(QwenAiToolParseGateway.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final LocalAiToolParseGateway fallback;
    private final RestClient client;
    private final String model;
    private final String apiKey;

    public QwenAiToolParseGateway(
            LocalAiToolParseGateway fallback,
            @Value("${prm.ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${prm.ai.model:qwen3-max}") String model,
            @Value("${prm.ai.api-key:}") String apiKey) {
        this.fallback = fallback;
        this.model = model;
        this.apiKey = apiKey;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        log.info("[智能确权-大瓦特] 文档解析启用 qwen 实现,model={}", model);
    }

    @Override
    public ParsedElements parse(String fileName, String content) {
        if (!StringUtils.hasText(apiKey)) {
            return fallback.parse(fileName, content);
        }
        try {
            String sys = "你是电力数据确权材料要素抽取助手。从确权证明材料中抽取关键要素。";
            String usr = "材料文件名:" + fileName + "\n材料正文:" + content
                    + "\n仅输出JSON,字段:rightSubject(权利主体,如 电力企业/供电局/供电所/用户单位),"
                    + "rightObject(权利客体,如 电力数据/计量设备/线路资产/数据资源),"
                    + "rightType(数据持有权/数据加工使用权/数据产品经营权/所有权/使用权/授权使用权),"
                    + "rightTerm(权利期限,起止或有效期),"
                    + "authScope(授权范围),dataSource(自行生产/公开采集/公共数据授权/共同生产/交易采购/其他),"
                    + "sensitiveType(个人信息/敏感个人信息/商业秘密/监管数据/电网生产数据/内部运营数据),"
                    + "sealValid(有效/可疑/未检出),sealDesc(印章说明),confidence(0-1小数)。不要输出多余文本。";
            String out = chat(sys, usr);
            return mapElements(out);
        } catch (RuntimeException e) {
            log.warn("[智能确权-大瓦特] qwen 解析失败,回退本地桩: {}", e.getMessage());
            return fallback.parse(fileName, content);
        }
    }

    /** 解析 qwen 返回 JSON 为确权要素(容忍代码块/前后缀);失败抛异常由 parse() 回退 */
    public static ParsedElements mapElements(String content) {
        if (content == null) {
            throw new IllegalArgumentException("空响应");
        }
        int s = content.indexOf('{');
        int e = content.lastIndexOf('}');
        if (s < 0 || e <= s) {
            throw new IllegalArgumentException("非JSON响应");
        }
        try {
            JsonNode n = OM.readTree(content.substring(s, e + 1));
            return new ParsedElements(
                    text(n, "rightSubject", "中国南方电网有限责任公司"),
                    text(n, "rightObject", "数据资源"),
                    text(n, "rightType", "数据持有权"),
                    text(n, "rightTerm", "3年"),
                    text(n, "authScope", "约定字段"),
                    text(n, "dataSource", "自行生产"),
                    text(n, "sensitiveType", "电网生产数据"),
                    text(n, "sealValid", "未检出"),
                    text(n, "sealDesc", ""),
                    conf(n));
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON解析失败", ex);
        }
    }

    private String chat(String system, String user) {
        Map<String, Object> body = Map.of(
                "model", model, "temperature", 0.2,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)));
        JsonNode resp = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body).retrieve().body(JsonNode.class);
        if (resp == null || !resp.has("choices") || resp.path("choices").isEmpty()) {
            throw new IllegalStateException("qwen 响应无 choices");
        }
        return resp.path("choices").get(0).path("message").path("content").asText();
    }

    private static String text(JsonNode n, String f, String dft) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() || !StringUtils.hasText(v.asText()) ? dft : v.asText();
    }

    private static double conf(JsonNode n) {
        double c = n.path("confidence").asDouble(0.9);
        return c < 0 ? 0 : (c > 1 ? 1 : c);
    }
}
