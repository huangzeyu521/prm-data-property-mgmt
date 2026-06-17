package com.csg.prm.confirm.controller;

import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.api.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 大瓦特 AI 智能辅助接口(⑨):授权意图识别 + AI 提供方诊断。
 * 对齐说明:OCR 权属识别 / 权属冲突检测 / RAG 知识问答 已统一收敛至智能确权辅助工具(aitool)的完整能力
 * (材料智能解析 / 冲突识别与分析 / 知识库与 RAG),主模块不再维护这三项早期简版端点;
 * 授权意图识别 aitool 暂无等价能力,保留于此。经 {@link DawatAiGateway} 共享端口对接。
 */
@RestController
@RequestMapping("/api/dpr/confirm/ai")
public class AiAssistController {

    private final DawatAiGateway ai;
    private final String provider;
    private final String model;

    public AiAssistController(DawatAiGateway ai,
                             @Value("${prm.ai.provider:stub}") String provider,
                             @Value("${prm.ai.model:qwen3-max}") String model) {
        this.ai = ai;
        this.provider = provider;
        this.model = model;
    }

    @GetMapping("/provider")
    public R<Map<String, String>> provider() {
        return R.ok(Map.of("provider", provider,
                "model", "qwen".equals(provider) ? model : "本地规则桩",
                "impl", ai.getClass().getSimpleName()));
    }

    @PostMapping("/auth-intent")
    public R<DawatAiGateway.AuthIntent> authIntent(@RequestParam String text) {
        return R.ok(ai.recognizeAuthIntent(text));
    }
}
