package com.csg.prm.confirm.ai;

import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.api.R;
import com.csg.prm.confirm.entity.ConfirmAiRunLog;
import com.csg.prm.confirm.service.ConfirmAiRunLogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 确权内生 AI 能力接口(智能解析 / 决策研判 / 权属冲突识别 / 校验规则可视化 / 校验过程回放)。
 * 命名空间在 PRM 自有 /dpr/confirm/ai/*(非独立工具 /aitool),前端在确权向导内嵌调用,不跳转独立工具。
 */
@RestController
@RequestMapping("/api/dpr/confirm/ai")
public class ConfirmAiController {

    private final ConfirmAiService service;
    private final ConfirmAiRunLogService runLogService;

    public ConfirmAiController(ConfirmAiService service, ConfirmAiRunLogService runLogService) {
        this.service = service;
        this.runLogService = runLogService;
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

    /** 校验规则可视化(§1):逐应交项的校验逻辑 + 规则明细 + AI 判定依据,供人工预审透明复核。 */
    @GetMapping("/check-logic")
    public R<ConfirmAiService.CheckLogic> checkLogic(@RequestParam String applyId) {
        return R.ok(service.checkLogic(applyId));
    }

    /** 校验过程回放(§2):该申请全部大模型操作留痕时间线(能力/模型/耗时/SM3/触发人),供复盘审计。 */
    @GetMapping("/runlog")
    public R<List<ConfirmAiRunLog>> runlog(@RequestParam String applyId) {
        return R.ok(runLogService.listByApply(applyId));
    }
}
