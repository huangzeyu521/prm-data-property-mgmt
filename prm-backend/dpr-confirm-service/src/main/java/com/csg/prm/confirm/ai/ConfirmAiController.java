package com.csg.prm.confirm.ai;

import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 确权内生 AI 能力接口(智能解析 / 决策研判 / 权属冲突识别)。
 * 命名空间在 PRM 自有 /dpr/confirm/ai/*(非独立工具 /aitool),前端在确权向导内嵌调用,不跳转独立工具。
 */
@RestController
@RequestMapping("/api/dpr/confirm/ai")
public class ConfirmAiController {

    private final ConfirmAiService service;

    public ConfirmAiController(ConfirmAiService service) {
        this.service = service;
    }

    /** 智能解析材料:要素抽取 + 敏感判定 + 内容指纹查重 */
    @PostMapping("/parse")
    public R<ConfirmAiService.ParseResult> parse(@RequestParam String applyId) {
        return R.ok(service.parse(applyId));
    }

    /** AI 决策研判:预测/需补材料/待处理冲突/评分 */
    @PostMapping("/decision")
    public R<ConfirmAiService.DecisionResult> decision(@RequestParam String applyId) {
        return R.ok(service.decision(applyId));
    }

    /** 权属冲突识别 */
    @PostMapping("/conflict")
    public R<DawatAiGateway.ConflictResult> conflict(@RequestParam String applyId) {
        return R.ok(service.conflict(applyId));
    }
}
