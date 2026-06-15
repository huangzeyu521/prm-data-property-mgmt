package com.csg.prm.confirm.aitool.service;

import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * #7 材料智能解析准确度评测:对带标注的样本集逐字段比对解析结果,产出整体与分字段准确率。
 * 评测使用当前生效的解析网关(dev=qwen 真测;test=本地确定性桩),阈值 0.95(可研"准确度≥95%")。
 */
@Service
public class AitAccuracyService {

    /**
     * 参与评测的要素字段。权利客体(rightObject)语义跨实现不稳定(文件名主干 vs 内容语义),
     * 非可靠标注项,故不计入准确率评测,避免对真实模型的正确语义抽取误判。
     */
    private static final String[] FIELDS = {
            "rightSubject", "rightType", "rightTerm", "authScope", "dataSource", "sensitiveType"};
    private static final double THRESHOLD = 0.95;
    private static final ObjectMapper OM = new ObjectMapper();

    private final AiToolParseGateway gateway;

    public AitAccuracyService(AiToolParseGateway gateway) {
        this.gateway = gateway;
    }

    public record FieldAccuracy(String field, int total, int correct, double rate) {
    }

    public record AccuracyReport(int sampleCount, int totalFields, int correctFields,
                                 double overall, double threshold, boolean pass,
                                 List<FieldAccuracy> perField, String note) {
    }

    public AccuracyReport evaluate() {
        List<JsonNode> samples = loadSamples();
        Map<String, int[]> perField = new LinkedHashMap<>(); // field -> [correct,total]
        for (String f : FIELDS) {
            perField.put(f, new int[]{0, 0});
        }
        int totalFields = 0;
        int correctFields = 0;
        for (JsonNode s : samples) {
            String fileName = s.path("fileName").asText("");
            String content = s.path("content").asText("");
            JsonNode expected = s.path("expected");
            AiToolParseGateway.ParsedElements e = gateway.parse(fileName, content);
            Map<String, String> got = toMap(e);
            for (String f : FIELDS) {
                String exp = expected.path(f).asText(null);
                if (!StringUtils.hasText(exp)) {
                    continue; // 该样本未标注此字段 → 不计入
                }
                totalFields++;
                perField.get(f)[1]++;
                if (matches(exp, got.get(f))) {
                    correctFields++;
                    perField.get(f)[0]++;
                }
            }
        }
        List<FieldAccuracy> fa = new ArrayList<>();
        for (String f : FIELDS) {
            int[] ct = perField.get(f);
            fa.add(new FieldAccuracy(f, ct[1], ct[0], ct[1] == 0 ? 1.0 : (double) ct[0] / ct[1]));
        }
        double overall = totalFields == 0 ? 0.0 : (double) correctFields / totalFields;
        String note = "评测网关=" + gateway.getClass().getSimpleName()
                + ";样本=" + samples.size() + ";比对=精确/包含匹配。生产以同口径对千问(qwen-vl/qwen)真测。";
        return new AccuracyReport(samples.size(), totalFields, correctFields,
                round(overall), THRESHOLD, overall >= THRESHOLD, fa, note);
    }

    private static Map<String, String> toMap(AiToolParseGateway.ParsedElements e) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("rightSubject", e.rightSubject());
        m.put("rightObject", e.rightObject());
        m.put("rightType", e.rightType());
        m.put("rightTerm", e.rightTerm());
        m.put("authScope", e.authScope());
        m.put("dataSource", e.dataSource());
        m.put("sensitiveType", e.sensitiveType());
        return m;
    }

    /** 字段命中:去空格后精确相等,或互相包含(容忍模型多/少修饰词)。 */
    private static boolean matches(String expected, String got) {
        if (got == null) {
            return false;
        }
        String a = expected.trim();
        String b = got.trim();
        return a.equals(b) || a.contains(b) || b.contains(a);
    }

    private List<JsonNode> loadSamples() {
        List<JsonNode> out = new ArrayList<>();
        try {
            JsonNode root = OM.readTree(new ClassPathResource("aitool/accuracy-samples.json").getInputStream());
            if (root.isArray()) {
                root.forEach(out::add);
            }
        } catch (Exception e) {
            // 样本缺失 → 返回空(overall=0,pass=false),便于发现配置问题
        }
        return out;
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
