package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.dto.BatchResult;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthFlowLog;
import com.csg.prm.authorize.service.AuthAgreementService;
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
    private final AuthAgreementService agreementService;

    public AuthApplyController(AuthApplyService service, AuthFlowLogService flowLogService,
                              AuthAgreementService agreementService) {
        this.service = service;
        this.flowLogService = flowLogService;
        this.agreementService = agreementService;
    }

    @PostMapping("/draft")
    public Result<String> saveDraft(@Valid @RequestBody AuthApply apply) {
        return Result.success(service.saveDraft(apply));
    }

    /** 删除授权申请(仅草稿态;一事一议仅本人,批量明细走清单级门禁)。 */
    @com.csg.prm.common.auth.RequiresRole({"apply", "admin"})
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

    @com.csg.prm.common.auth.RequiresRole({"unit", "review", "business", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/{applyId}/approve")
    public Result<String> approve(@PathVariable String applyId, @RequestParam(required = false) String opinion) {
        String certId = service.approve(applyId, opinion);        // 事务1:沿链推进(专项终审=批准/批量终审=已生效),提交
        // 专项终审「批准」即自动「形成」《运营授权协议》(35号文 表2 110),独立事务 best-effort
        // (自过滤:仅一事一议+批准/存量已生效+无协议才生成,幂等;批量走清单级协议,内部自判跳过)
        agreementService.autoGenerateForApprovedApply(applyId);
        return Result.success(certId);
    }

    @com.csg.prm.common.auth.RequiresRole({"unit", "review", "business", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/{applyId}/reject")
    public Result<Void> reject(@PathVariable String applyId, @RequestParam(required = false) String reason) {
        service.reject(applyId, reason);
        return Result.success();
    }

    /** 申请人主动撤回(审批中 -> 已撤回);仅申请人本人,撤回后可修改重提。 */
    @com.csg.prm.common.auth.RequiresRole({"apply", "business", "unit", "admin"})
    @PostMapping("/{applyId}/withdraw")
    public Result<Void> withdraw(@PathVariable String applyId, @RequestParam(required = false) String reason) {
        service.withdraw(applyId, reason);
        return Result.success();
    }

    /** 整份一事一议申请单撤回(formNo 下全部审批中明细逐行撤回)。 */
    @com.csg.prm.common.auth.RequiresRole({"apply", "business", "unit", "admin"})
    @PostMapping("/form/{formNo}/withdraw")
    public Result<Void> withdrawForm(@PathVariable String formNo) {
        service.withdrawForm(formNo);
        return Result.success();
    }

    /** 批量审批通过(带审核意见,逐条)。与单条 approve 一致,申报人不得自批。 */
    @com.csg.prm.common.auth.RequiresRole({"unit", "review", "business", "manager", "director", "gm", "leadership", "admin"})
    @PostMapping("/batch-approve")
    public Result<BatchResult> batchApprove(@RequestBody List<String> applyIds) {
        BatchResult r = service.batchApprove(applyIds);
        // 批量审批中到达终审「批准」的一事一议同样自动「形成」协议(autoGenerate 自过滤,幂等)
        if (applyIds != null) {
            for (String id : applyIds) {
                agreementService.autoGenerateForApprovedApply(id);
            }
        }
        return Result.success(r);
    }

    /** 批量驳回(统一原因)。与单条 reject 一致,申报人不得自驳。 */
    @com.csg.prm.common.auth.RequiresRole({"unit", "review", "business", "manager", "director", "gm", "leadership", "admin"})
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

    // ── 一事一议「单场景·多表」申请单(同 formNo 多张数据表,对齐表5 多行) ──

    /** 新建一事一议申请单,返回申请单号 formNo(多张数据表逐张 saveDraft 共用)。 */
    @PostMapping("/form")
    public Result<String> createForm() {
        return Result.success(service.createForm());
    }

    /** 查申请单下全部数据表行(向导回填/历史详情)。 */
    @GetMapping("/by-form/{formNo}")
    public Result<java.util.List<AuthApply>> byForm(@PathVariable String formNo) {
        return Result.success(service.byForm(formNo));
    }

    /** 提交整份申请单(formNo 下全部草稿行逐行进入审批链)。 */
    @PostMapping("/form/{formNo}/submit")
    public Result<Void> submitForm(@PathVariable String formNo) {
        service.submitForm(formNo);
        return Result.success();
    }

    /** 申请单逐表自检(只读试跑,与提交门禁同源)。 */
    @PostMapping("/form/{formNo}/compliance-check")
    public Result<com.csg.prm.authorize.dto.BatchComplianceResult> formComplianceCheck(@PathVariable String formNo) {
        return Result.success(service.checkFormCompliance(formNo));
    }

    @PostMapping("/page")
    public Result<PageResult<AuthApply>> page(@Valid @RequestBody AuthApplyQuery query) {
        return Result.success(service.page(query));
    }
}
