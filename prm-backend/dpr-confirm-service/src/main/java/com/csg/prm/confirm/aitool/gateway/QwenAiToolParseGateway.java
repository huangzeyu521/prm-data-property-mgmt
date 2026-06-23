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
 * 文档解析网关 —— 真实调用大瓦特(以阿里云百炼 qwen3.7-max-2026-06-08 模拟,OpenAI 兼容)做确权要素抽取。
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
    private final String visionModel;
    private final String embedModel;
    private final String apiKey;

    public QwenAiToolParseGateway(
            LocalAiToolParseGateway fallback,
            @Value("${prm.ai.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${prm.ai.model:qwen3.7-max-2026-06-08}") String model,
            @Value("${prm.ai.vision-model:qwen-vl-max}") String visionModel,
            @Value("${prm.ai.embed-model:text-embedding-v3}") String embedModel,
            @Value("${prm.ai.api-key:}") String apiKey) {
        this.fallback = fallback;
        this.model = model;
        this.visionModel = visionModel;
        this.embedModel = embedModel;
        this.apiKey = apiKey;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        log.info("[智能确权-千问] 文档解析启用 qwen 实现,model={},vision-model={},embed-model={}", model, visionModel, embedModel);
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
            return AiToolParseGateway.normalize(mapElements(out), content);
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

    @Override
    public OcrLayout ocrAndLayout(byte[] imageBytes, String mime, String hint) {
        if (!StringUtils.hasText(apiKey) || imageBytes == null || imageBytes.length == 0) {
            return fallback.ocrAndLayout(imageBytes, mime, hint);
        }
        try {
            String sys = "你是电力数据确权材料 OCR 与版面分析助手。对图片做 OCR 并分析版面。";
            String usr = "对这张材料图片(" + (hint == null ? "" : hint) + ")做:①OCR 提取全部正文文字;"
                    + "②识别标题;③识别表格(含嵌套表格,文本化为多行);④识别印章区域(区分公章/合同章/骑缝章)与位置;"
                    + "⑤判断是目录页还是正文页;⑥估计分栏数。仅输出JSON,字段:"
                    + "text(正文全文),titles(标题数组),tables(表格文本数组),"
                    + "seals(数组,每项{type:公章|合同章|骑缝章|其他,location:位置描述,desc:说明}),"
                    + "pageType(目录页|正文页),columnCount(整数),confidence(0-1小数)。不要输出多余文本。";
            String out = chatVision(sys, usr, imageBytes, StringUtils.hasText(mime) ? mime : "image/png");
            return mapOcrLayout(out);
        } catch (RuntimeException e) {
            log.warn("[智能确权-千问] qwen-vl OCR/版面分析失败,回退本地桩: {}", e.getMessage());
            return fallback.ocrAndLayout(imageBytes, mime, hint);
        }
    }

    @Override
    public String classifyCategory(String fileName, String content) {
        if (!StringUtils.hasText(apiKey)) {
            return fallback.classifyCategory(fileName, content);
        }
        try {
            String sys = "你是电力数据确权材料归类助手。只回答一个类别词,不要解释。";
            String usr = "材料文件名:" + fileName + "\n材料正文(节选):"
                    + (content == null ? "" : content.substring(0, Math.min(content.length(), 800)))
                    + "\n请从以下类别中选一个最贴切的:元数据、制度附件、授权材料、合同材料、来源说明、确权证明、其他。只输出类别词。";
            String out = chat(sys, usr);
            return normalizeCategory(out);
        } catch (RuntimeException e) {
            return fallback.classifyCategory(fileName, content);
        }
    }

    @Override
    public String normalizeEnum(String field, String value, java.util.List<String> candidates) {
        if (!StringUtils.hasText(apiKey) || candidates == null || candidates.isEmpty()) {
            return fallback.normalizeEnum(field, value, candidates);
        }
        try {
            String sys = "你是数据标准化助手。把给定脏值归一到候选标准值之一,只回答标准值本身,无法判定回答'无'。";
            String usr = "字段:" + field + "\n脏值:" + value + "\n候选标准值:" + String.join("、", candidates)
                    + "\n只输出一个候选标准值,或'无'。";
            String out = chat(sys, usr);
            if (out == null) {
                return "";
            }
            for (String c : candidates) {
                if (out.contains(c)) {
                    return c;
                }
            }
            return "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    @Override
    public float[] embed(String text) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(text)) {
            return fallback.embed(text);
        }
        try {
            Map<String, Object> body = Map.of("model", embedModel, "input", text);
            JsonNode resp = client.post().uri("/embeddings")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body).retrieve().body(JsonNode.class);
            JsonNode arr = resp == null ? null : resp.path("data").path(0).path("embedding");
            if (arr == null || !arr.isArray() || arr.isEmpty()) {
                return fallback.embed(text);
            }
            float[] v = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                v[i] = (float) arr.get(i).asDouble();
            }
            return v;
        } catch (RuntimeException e) {
            log.warn("[智能确权-千问] 向量化失败,回退本地 hash 向量: {}", e.getMessage());
            return fallback.embed(text);
        }
    }

    @Override
    public ElementSet extractElements(String corpus) {
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(corpus)) {
            return fallback.extractElements(corpus);
        }
        try {
            String sys = "你是电力数据确权要素抽取助手。从语料中识别确权关键要素,可结合命名实体识别与语义理解。";
            String usr = "语料:\n" + (corpus.length() > 4000 ? corpus.substring(0, 4000) : corpus)
                    + "\n仅输出JSON,字段:"
                    + "sourceMethod(数据来源方式:自行生产/公开采集/公共数据授权/共同生产/交易采购/其他),"
                    + "dataFeatures(数组,取值:个人信息/敏感个人信息/商业秘密/监管数据/电网生产数据/内部运营数据),"
                    + "subjects(对象,键:来源主体/授权主体/使用主体/加工主体/共享对象,值为名称,无则省略),"
                    + "constraints(对象,键:授权范围/使用边界/共享限制/保留期限/脱敏要求,值为内容,无则省略),"
                    + "confidence(0-1)。不要输出多余文本。";
            String out = chat(sys, usr);
            return mapElementSet(out);
        } catch (RuntimeException e) {
            log.warn("[智能确权-千问] 要素抽取失败,回退本地规则: {}", e.getMessage());
            return fallback.extractElements(corpus);
        }
    }

    static ElementSet mapElementSet(String content) {
        if (content == null) {
            return null;
        }
        int s = content.indexOf('{');
        int e = content.lastIndexOf('}');
        if (s < 0 || e <= s) {
            return null;
        }
        try {
            JsonNode n = OM.readTree(content.substring(s, e + 1));
            java.util.List<String> features = strList(n.get("dataFeatures"));
            java.util.Map<String, String> subjects = strMap(n.get("subjects"));
            java.util.Map<String, String> constraints = strMap(n.get("constraints"));
            return new ElementSet(text(n, "sourceMethod", ""), features, subjects, constraints, conf(n));
        } catch (Exception ex) {
            return null;
        }
    }

    private static java.util.Map<String, String> strMap(JsonNode node) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(en -> {
                if (StringUtils.hasText(en.getValue().asText())) {
                    m.put(en.getKey(), en.getValue().asText());
                }
            });
        }
        return m;
    }

    /** 模型输出的中文类别词收敛为规范资料类型编码 01–07(联调清单 行25 / CEC_DATA_TYPE)。 */
    private static String normalizeCategory(String out) {
        return com.csg.prm.confirm.aitool.enums.MaterialDataType.codeOf(out);
    }

    /** 解析 qwen-vl 返回 JSON 为 OcrLayout(容忍代码块/前后缀)。 */
    public static OcrLayout mapOcrLayout(String content) {
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
            java.util.List<String> titles = strList(n.get("titles"));
            java.util.List<String> tables = strList(n.get("tables"));
            java.util.List<Seal> seals = new java.util.ArrayList<>();
            JsonNode sn = n.get("seals");
            if (sn != null && sn.isArray()) {
                for (JsonNode item : sn) {
                    seals.add(new Seal(text(item, "type", "其他"),
                            text(item, "location", ""), text(item, "desc", "")));
                }
            }
            return new OcrLayout(text(n, "text", ""), titles, tables, seals,
                    text(n, "pageType", "正文页"), n.path("columnCount").asInt(1), conf(n));
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON解析失败", ex);
        }
    }

    private static java.util.List<String> strList(JsonNode arr) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode i : arr) {
                if (StringUtils.hasText(i.asText())) {
                    out.add(i.asText());
                }
            }
        }
        return out;
    }

    /** 多模态对话(qwen-vl):图片以 data URL(base64)内联传入,OpenAI 兼容 content 数组格式。 */
    private String chatVision(String system, String userText, byte[] image, String mime) {
        String dataUrl = "data:" + mime + ";base64," + java.util.Base64.getEncoder().encodeToString(image);
        Map<String, Object> userMsg = Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", userText),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))));
        Map<String, Object> body = Map.of(
                "model", visionModel, "temperature", 0.1,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        userMsg));
        JsonNode resp = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body).retrieve().body(JsonNode.class);
        if (resp == null || !resp.has("choices") || resp.path("choices").isEmpty()) {
            throw new IllegalStateException("qwen-vl 响应无 choices");
        }
        return resp.path("choices").get(0).path("message").path("content").asText();
    }

    @Override
    public String adviseResolution(String context) {
        try {
            String sys = "你是电力数据确权权属冲突处置专家。请基于《数据二十条》数据三权分置、南网数据资产管理规定等法规"
                    + "与处置规则,针对给定的权属冲突给出简洁可执行的解决方案建议(1-3 条,含责任主体与处置步骤)。";
            return chat(sys, context);
        } catch (Exception e) {
            return null; // 失败回退规则建议
        }
    }

    @Override
    public String reviewMaterials(String context) {
        try {
            String sys = "你是中国南方电网数据确权材料合规校验专家,依据《数据确权授权业务指导书》逐份校验申请材料:"
                    + "①完整性(要素是否齐全、是否盖章);②合规性(密级/敏感信息/第三方权益表述是否合规);"
                    + "③与申请表单一致性(权属主体/类型/资产是否一致)。"
                    + "仅输出严格JSON:{\"overall\":\"通过|不通过|存疑\",\"overallDesc\":\"...\","
                    + "\"items\":[{\"materialName\":\"...\",\"verdict\":\"通过|不通过|存疑\",\"issues\":\"...\",\"suggestion\":\"...\"}]},"
                    + "不要输出多余文本。";
            return chat(sys, context);
        } catch (Exception e) {
            return null; // 失败回退规则桩结论
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
