package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 表6 数据批量授权清单接口(IM-DAM-DPR-03-001-001-005)。草案->申报稿->批准。 */
@RestController
@RequestMapping("/api/dpr/auth/batch-list")
public class BatchAuthListController {

    private final BatchAuthListService service;
    private final com.csg.prm.common.ai.DawatAiGateway ai;
    private final com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper;

    public BatchAuthListController(BatchAuthListService service,
                                   com.csg.prm.common.ai.DawatAiGateway ai,
                                   com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper) {
        this.ai = ai;
        this.applyMapper = applyMapper;
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody BatchAuthList list) {
        return R.ok(service.create(list));
    }

    /** AI 批量填单:自然语言→共享字段+多条明细(qwen3-max,stub 回退) */
    @PostMapping("/ai-intent")
    public R<String> aiIntent(@RequestParam String text) {
        return R.ok(ai.parseBatchIntent(text));
    }

    /** AI 清单预审:整单明细交大模型审查批量适用条件(非门禁) */
    @PostMapping("/{batchListId}/pre-review")
    public R<String> preReview(@PathVariable String batchListId) {
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
        String opinion = ai.preReviewAuth(ctx.toString());
        return R.ok(opinion == null ? "AI 预审暂不可用" : opinion);
    }

    @PostMapping("/{batchListId}/submit")
    public R<Void> submit(@PathVariable String batchListId) {
        service.submit(batchListId);
        return R.ok();
    }

    @PostMapping("/{batchListId}/approve")
    public R<Void> approve(@PathVariable String batchListId) {
        service.approve(batchListId);
        return R.ok();
    }

    @GetMapping("/{batchListId}")
    public R<BatchAuthList> detail(@PathVariable String batchListId) {
        return R.ok(service.getById(batchListId));
    }

    @GetMapping("/page")
    public R<PageResult<BatchAuthList>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String listYear,
                                             @RequestParam(required = false) String listStatus) {
        return R.ok(service.page(current, size, listYear, listStatus));
    }
}
