package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageQuery;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 表6 数据批量授权清单接口(IM-DAM-DPR-03-001-001-005)。草案->申报稿->批准。 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/batch-list")
public class BatchAuthListController {

    private final BatchAuthListService service;
    private final com.csg.prm.common.ai.DawatAiGateway ai;
    private final com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper;
    private final com.csg.prm.common.aitrace.AiRunLogService aiRunLogService;

    public BatchAuthListController(BatchAuthListService service,
                                   com.csg.prm.common.ai.DawatAiGateway ai,
                                   com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper,
                                   com.csg.prm.common.aitrace.AiRunLogService aiRunLogService) {
        this.ai = ai;
        this.applyMapper = applyMapper;
        this.service = service;
        this.aiRunLogService = aiRunLogService;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody BatchAuthList list) {
        return Result.success(service.create(list));
    }

    /** AI 批量填单:自然语言→共享字段+多条明细(qwen3-max,stub 回退) */
    @PostMapping("/ai-intent")
    public Result<String> aiIntent(@RequestParam String text) {
        long t0 = System.currentTimeMillis();
        String result = ai.parseBatchIntent(text);
        aiRunLogService.record(com.csg.prm.common.aitrace.AiRunLog.BIZ_AUTHORIZE, "批量意图解析",
                com.csg.prm.common.aitrace.AiRunLog.CAP_AUTH_BATCH_INTENT, ai.modelName(),
                text == null ? "" : (text.length() > 200 ? text.substring(0, 200) : text),
                result, System.currentTimeMillis() - t0);
        return Result.success(result);
    }

    /** AI 清单预审:整单明细交大模型审查批量适用条件(非门禁) */
    @PostMapping("/{batchListId}/pre-review")
    public Result<String> preReview(@PathVariable String batchListId) {
        java.util.List<com.csg.prm.authorize.entity.AuthApply> items = applyMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.csg.prm.authorize.entity.AuthApply>()
                        .eq(com.csg.prm.authorize.entity.AuthApply::getBatchListId, batchListId));
        StringBuilder ctx = new StringBuilder("【批量清单预审】明细数:").append(items.size()).append('\n');
        java.util.Set<String> grantees = new java.util.LinkedHashSet<>();
        java.util.Set<String> rights = new java.util.LinkedHashSet<>();
        for (com.csg.prm.authorize.entity.AuthApply a : items) {
            grantees.add(a.getGranteeOrg());
            rights.add(a.getRightType());
            ctx.append("明细:").append(a.getAssetName()).append('/').append(a.getRightType())
               .append('/').append(a.getScenario())
               .append(";隐私:").append(a.getSensitiveType() == null ? "无" : a.getSensitiveType()).append('\n');
        }
        ctx.append("被授权方一致性:").append(grantees.size() <= 1 ? "一致" : "不符(存在多个被授权方:" + grantees + ")")
           .append(";权益类型一致性:").append(rights.size() <= 1 ? "一致" : "不符(混合权益类型,建议拆分或转一事一议)");
        long t0 = System.currentTimeMillis();
        String opinion = ai.preReviewAuth(ctx.toString());
        aiRunLogService.record(com.csg.prm.common.aitrace.AiRunLog.BIZ_AUTHORIZE, batchListId,
                com.csg.prm.common.aitrace.AiRunLog.CAP_AUTH_PRECHECK, ai.modelName(),
                "批量清单预审 明细数:" + items.size(), opinion, System.currentTimeMillis() - t0);
        return Result.success(opinion == null ? "AI 预审暂不可用" : opinion);
    }

    /** 只读合规校验(试跑):整单是否可提交 + 逐项被拦原因,供前端"通过才放行提交"闭环 */
    @PostMapping("/{batchListId}/compliance-check")
    public Result<com.csg.prm.authorize.dto.BatchComplianceResult> complianceCheck(@PathVariable String batchListId) {
        return Result.success(service.complianceCheck(batchListId));
    }

    @PostMapping("/{batchListId}/submit")
    public Result<Void> submit(@PathVariable String batchListId) {
        service.submit(batchListId);
        return Result.success();
    }

    /** 批准批量清单(逐节点推进至生效)。申报人只能 submit,不得自批。 */
    @com.csg.prm.common.auth.RequiresRole({"review", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/{batchListId}/approve")
    public Result<Void> approve(@PathVariable String batchListId) {
        service.approve(batchListId);
        return Result.success();
    }

    @GetMapping("/{batchListId}")
    public Result<BatchAuthList> detail(@PathVariable String batchListId) {
        return Result.success(service.getById(batchListId));
    }

    @GetMapping("/page")
    public Result<PageResult<BatchAuthList>> page(@Valid PageQuery page,
                                             @RequestParam(required = false) String listYear,
                                             @RequestParam(required = false) String listStatus) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), listYear, listStatus));
    }
}
