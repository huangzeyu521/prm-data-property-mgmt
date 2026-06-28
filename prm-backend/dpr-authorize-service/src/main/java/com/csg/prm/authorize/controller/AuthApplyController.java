package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.dto.BatchResult;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthFlowLog;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthFlowLogService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Validated
@RequestMapping("/api/dpr/auth/apply")
public class AuthApplyController {

    private final AuthApplyService service;
    private final AuthFlowLogService flowLogService;

    public AuthApplyController(AuthApplyService service, AuthFlowLogService flowLogService) {
        this.service = service;
        this.flowLogService = flowLogService;
    }

    @PostMapping("/draft")
    public Result<String> saveDraft(@Valid @RequestBody AuthApply apply) {
        return Result.success(service.saveDraft(apply));
    }

    /** 删除授权申请(仅草稿态,如批量清单加错/重复的明细项)。 */
    @DeleteMapping("/{applyId}")
    public Result<Void> delete(@PathVariable String applyId) {
        service.deleteApply(applyId);
        return Result.success();
    }

    @PostMapping("/{applyId}/submit")
    public Result<Void> submit(@PathVariable String applyId) {
        service.submit(applyId);
        return Result.success();
    }

    @com.csg.prm.common.auth.RequiresRole({"review", "business", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/{applyId}/approve")
    public Result<String> approve(@PathVariable String applyId, @RequestParam(required = false) String opinion) {
        return Result.success(service.approve(applyId, opinion));
    }

    @com.csg.prm.common.auth.RequiresRole({"review", "business", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/{applyId}/reject")
    public Result<Void> reject(@PathVariable String applyId, @RequestParam(required = false) String reason) {
        service.reject(applyId, reason);
        return Result.success();
    }

    /** 批量审批通过(带审核意见,逐条)。与单条 approve 一致,申报人不得自批。 */
    @com.csg.prm.common.auth.RequiresRole({"review", "business", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/batch-approve")
    public Result<BatchResult> batchApprove(@RequestBody List<String> applyIds) {
        return Result.success(service.batchApprove(applyIds));
    }

    /** 批量驳回(统一原因)。与单条 reject 一致,申报人不得自驳。 */
    @com.csg.prm.common.auth.RequiresRole({"review", "business", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/batch-reject")
    public Result<BatchResult> batchReject(@RequestBody List<String> applyIds,
                                      @RequestParam(required = false) String reason) {
        return Result.success(service.batchReject(applyIds, reason));
    }

    @GetMapping("/{applyId}")
    public Result<AuthApply> detail(@PathVariable String applyId) {
        return Result.success(service.getById(applyId));
    }

    /** 审批处理记录(轨迹:各节点责任人/意见/时间)。 */
    @GetMapping("/{applyId}/flow-log")
    public Result<List<AuthFlowLog>> flowLog(@PathVariable String applyId) {
        return Result.success(flowLogService.listByApply(applyId));
    }

    /** 批量清单明细(表6):查清单下所有授权项 */
    @GetMapping("/by-batch/{batchListId}")
    public Result<java.util.List<AuthApply>> byBatch(@PathVariable String batchListId) {
        return Result.success(service.byBatch(batchListId));
    }

    @PostMapping("/page")
    public Result<PageResult<AuthApply>> page(@Valid @RequestBody AuthApplyQuery query) {
        return Result.success(service.page(query));
    }
}
