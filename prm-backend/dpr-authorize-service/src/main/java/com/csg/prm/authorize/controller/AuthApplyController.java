package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据授权申请与审批接口(授权申请 IM-DAM-DPR-03-001-001 / 授权审核 -002)。
 */
@RestController
@RequestMapping("/api/dpr/auth/apply")
public class AuthApplyController {

    private final AuthApplyService service;

    public AuthApplyController(AuthApplyService service) {
        this.service = service;
    }

    @PostMapping("/draft")
    public R<String> saveDraft(@RequestBody AuthApply apply) {
        return R.ok(service.saveDraft(apply));
    }

    @PostMapping("/{applyId}/submit")
    public R<Void> submit(@PathVariable String applyId) {
        service.submit(applyId);
        return R.ok();
    }

    @PostMapping("/{applyId}/approve")
    public R<String> approve(@PathVariable String applyId) {
        return R.ok(service.approve(applyId));
    }

    @PostMapping("/{applyId}/reject")
    public R<Void> reject(@PathVariable String applyId, @RequestParam(required = false) String reason) {
        service.reject(applyId, reason);
        return R.ok();
    }

    @GetMapping("/{applyId}")
    public R<AuthApply> detail(@PathVariable String applyId) {
        return R.ok(service.getById(applyId));
    }

    /** 批量清单明细(表6):查清单下所有授权项 */
    @GetMapping("/by-batch/{batchListId}")
    public R<java.util.List<AuthApply>> byBatch(@PathVariable String batchListId) {
        return R.ok(service.byBatch(batchListId));
    }

    @PostMapping("/page")
    public R<PageResult<AuthApply>> page(@RequestBody AuthApplyQuery query) {
        return R.ok(service.page(query));
    }
}
