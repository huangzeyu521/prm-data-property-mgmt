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
 * 大瓦特 AI 智能辅助接口(⑨):OCR 权属识别 / 权属冲突检测 / 授权意图识别 / RAG 知识问答。
 * 经 {@link DawatAiGateway} 端口对接;provider=stub 本地桩 / provider=qwen 阿里云百炼 qwen3-max。
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

    @PostMapping("/ocr-ownership")
    public R<DawatAiGateway.OcrOwnership> ocrOwnership(@RequestParam String fileUrl) {
        return R.ok(ai.recognizeOwnership(fileUrl));
    }

    @PostMapping("/detect-conflict")
    public R<DawatAiGateway.ConflictResult> detectConflict(@RequestParam String assetId,
                                                           @RequestParam(required = false) String rightHolder,
                                                           @RequestParam(required = false) String rightType) {
        return R.ok(ai.detectConflict(assetId, rightHolder, rightType));
    }

    @PostMapping("/auth-intent")
    public R<DawatAiGateway.AuthIntent> authIntent(@RequestParam String text) {
        return R.ok(ai.recognizeAuthIntent(text));
    }

    @GetMapping("/ask")
    public R<DawatAiGateway.RagAnswer> ask(@RequestParam String question) {
        return R.ok(ai.ask(question));
    }
}
