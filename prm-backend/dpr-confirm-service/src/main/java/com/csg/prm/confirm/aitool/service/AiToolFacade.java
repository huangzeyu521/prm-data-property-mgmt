package com.csg.prm.confirm.aitool.service;

import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一工具适配层(3.3#2/#3):把 OCR / Embedding / 向量检索 / 规则校验 / 模型调用 / 文档解析 统一封装,
 * 底层 Local↔Qwen 网关可换(@Primary),降低组件替换成本;暴露能力清单供 MCP/ACP/CLI/REST 编排接入;
 * 兼容 OpenAI 风格接口(base-url 可配,内网切换)。
 */
@Service
public class AiToolFacade {

    private final AiToolParseGateway parseGateway;
    private final AitKbService kbService;
    private final DawatAiGateway ai;
    private final AitRunLogService runLog;

    private final String provider;
    private final String baseUrl;
    private final String model;
    private final String visionModel;
    private final String embedModel;

    public AiToolFacade(AiToolParseGateway parseGateway, AitKbService kbService, DawatAiGateway ai,
                        AitRunLogService runLog,
                        @Value("${prm.ai.provider:stub}") String provider,
                        @Value("${prm.ai.base-url:}") String baseUrl,
                        @Value("${prm.ai.model:}") String model,
                        @Value("${prm.ai.vision-model:qwen-vl-max}") String visionModel,
                        @Value("${prm.ai.embed-model:text-embedding-v3}") String embedModel) {
        this.parseGateway = parseGateway;
        this.kbService = kbService;
        this.ai = ai;
        this.runLog = runLog;
        this.provider = provider;
        this.baseUrl = baseUrl;
        this.model = model;
        this.visionModel = visionModel;
        this.embedModel = embedModel;
    }

    /** 能力清单(供 MCP/ACP 编排 / CLI / 第三方 REST 接入)。 */
    public List<Map<String, Object>> capabilities() {
        List<Map<String, Object>> caps = new ArrayList<>();
        caps.add(cap("文档解析", "parse", "PDF/Word/Excel 正文与要素抽取", model));
        caps.add(cap("OCR", "ocr", "图片/扫描件 OCR+版面分析(经 /material/upload-file 上传后解析)", visionModel));
        caps.add(cap("Embedding", "embedding", "文本向量化", embedModel));
        caps.add(cap("向量检索", "vector-search", "知识库语义/混合检索", embedModel));
        caps.add(cap("规则校验", "rule-check", "清洗/冲突规则校验(见 /clean、/conflict-rule)", "-"));
        caps.add(cap("模型调用", "model-call", "大模型问答/研判(OpenAI 兼容)", model));
        return caps;
    }

    private Map<String, Object> cap(String name, String tool, String desc, String model) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("tool", tool);
        m.put("desc", desc);
        m.put("model", model);
        return m;
    }

    /** 模型/平台配置(OpenAI 兼容,内网可配)。 */
    public Map<String, Object> modelConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider", provider);
        m.put("baseUrl", baseUrl);
        m.put("model", model);
        m.put("visionModel", visionModel);
        m.put("embedModel", embedModel);
        m.put("openaiCompatible", true);
        m.put("intranetConfigurable", "通过环境变量 PRM_AI_BASE_URL/QWEN_MODEL_NAME/DASHSCOPE_API_KEY 指向内网推理服务");
        return m;
    }

    /** 统一工具调用入口:tool ∈ embedding/parse/vector-search/model-call/rule-check/ocr。 */
    public Object invoke(String tool, Map<String, Object> params) {
        Map<String, Object> p = params == null ? Map.of() : params;
        long t0 = System.currentTimeMillis();
        switch (tool == null ? "" : tool) {
            case "embedding" -> {
                float[] v = parseGateway.embed(str(p, "text"));
                runLog.model("AiToolFacade", "embedding", embedModel, System.currentTimeMillis() - t0, "成功");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("dim", v == null ? 0 : v.length);
                out.put("preview", v == null || v.length == 0 ? List.of()
                        : List.of(v[0], v.length > 1 ? v[1] : 0f, v.length > 2 ? v[2] : 0f));
                return out;
            }
            case "parse" -> {
                AiToolParseGateway.ParsedElements e = parseGateway.parse(str(p, "fileName"), str(p, "content"));
                runLog.model("AiToolFacade", "parse", model, System.currentTimeMillis() - t0, "成功");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("rightSubject", e.rightSubject());
                out.put("rightType", e.rightType());
                out.put("dataSource", e.dataSource());
                out.put("sensitiveType", e.sensitiveType());
                out.put("confidence", e.confidence());
                return out;
            }
            case "vector-search" -> {
                List<AitKbService.SearchHit> hits = kbService.search(str(p, "query"),
                        AitKbService.MODE_HYBRID, str(p, "domain"), null, 5);
                runLog.model("AiToolFacade", "vector-search", embedModel, System.currentTimeMillis() - t0, "成功");
                return hits;
            }
            case "model-call" -> {
                DawatAiGateway.RagAnswer a = ai.ask(str(p, "prompt"));
                runLog.model("AiToolFacade", "model-call", model, System.currentTimeMillis() - t0, "成功");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("answer", a.answer());
                out.put("citations", a.citations());
                return out;
            }
            case "rule-check" -> {
                runLog.rule("AiToolFacade", "rule-check", "规则校验请走 /clean(清洗规则)与 /conflict-rule(冲突规则)");
                return Map.of("note", "规则校验由清洗服务与冲突规则引擎承载,见 /clean、/conflict-rule");
            }
            case "ocr" -> {
                return Map.of("note", "OCR 需图片字节,请经 /material/upload-file 上传后调用 /material/{id}/parse");
            }
            default -> throw new BizException("不支持的工具:" + tool + ",可用工具见 /tools/capabilities");
        }
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }

    public boolean modelAvailable() {
        return StringUtils.hasText(baseUrl) && !"stub".equalsIgnoreCase(provider);
    }
}
