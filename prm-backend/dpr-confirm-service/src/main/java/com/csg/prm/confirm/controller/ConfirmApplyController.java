package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.confirm.dto.ConfirmApplyQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmFlowLog;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmFlowLogService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据确权申请与审批接口(确权申请 IM-DAM-DPR-02-001-001 / 确权审核 -002)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/apply")
public class ConfirmApplyController {

    private final ConfirmApplyService service;
    private final ConfirmFlowLogService flowLogService;

    public ConfirmApplyController(ConfirmApplyService service, ConfirmFlowLogService flowLogService) {
        this.service = service;
        this.flowLogService = flowLogService;
    }

    @PostMapping("/draft")
    public R<String> saveDraft(@RequestBody ConfirmApply apply) {
        return R.ok(service.saveDraft(apply));
    }

    /** 元数据自动填充(含质量评分):用于确权表单预填 + 提交前质量门禁提示 */
    @GetMapping("/autofill")
    public R<com.csg.prm.confirm.gateway.MetadataGateway.MetadataInfo> autofill(@RequestParam String assetId) {
        return R.ok(service.autofill(assetId));
    }

    /**
     * 派生重确权工单(草稿)。供权益动态监测识别数据新增/来源变更/到期时联动调用(附录F 3.3.2)。
     */
    @PostMapping("/re-confirm")
    public R<String> reConfirm(@RequestParam String assetId,
                               @RequestParam(required = false) String assetName,
                               @RequestParam(required = false) String rightType,
                               @RequestParam(required = false) String reason,
                               @RequestParam(required = false) String sourceRef,
                               @RequestParam(required = false) String changeTrigger) {
        return R.ok(service.createReConfirm(assetId, assetName, rightType, reason, sourceRef, changeTrigger));
    }

    /** 固化提交前 AI 校验结果快照(JSON),供人工预审完整复核·可追溯。提交前调用。 */
    @PostMapping("/{applyId}/ai-snapshot")
    public R<Void> saveAiSnapshot(@PathVariable String applyId, @RequestBody String snapshotJson) {
        service.saveAiSnapshot(applyId, snapshotJson);
        return R.ok();
    }

    @PostMapping("/{applyId}/submit")
    public R<Void> submit(@PathVariable String applyId) {
        service.submit(applyId);
        return R.ok();
    }

    /** 批量提交草稿至审核。 */
    @PostMapping("/batch-submit")
    public R<com.csg.prm.confirm.dto.BatchResult> batchSubmit(@RequestBody List<String> applyIds) {
        return R.ok(service.batchSubmit(applyIds));
    }

    /** 批量审批通过。 */
    @PostMapping("/batch-approve")
    public R<com.csg.prm.confirm.dto.BatchResult> batchApprove(@RequestBody List<String> applyIds) {
        return R.ok(service.batchApprove(applyIds));
    }

    /** 批量驳回(统一原因)。 */
    @PostMapping("/batch-reject")
    public R<com.csg.prm.confirm.dto.BatchResult> batchReject(@RequestBody List<String> applyIds,
                                                              @RequestParam(required = false) String reason) {
        return R.ok(service.batchReject(applyIds, reason));
    }

    @com.csg.prm.common.auth.RequiresRole({"review", "admin"})
    @PostMapping("/{applyId}/approve")
    public R<String> approve(@PathVariable String applyId) {
        return R.ok(service.approve(applyId));
    }

    @com.csg.prm.common.auth.RequiresRole({"review", "admin"})
    @PostMapping("/{applyId}/reject")
    public R<Void> reject(@PathVariable String applyId, @RequestParam(required = false) String reason) {
        service.reject(applyId, reason);
        return R.ok();
    }

    /** 删除草稿确权申请(仅草稿状态;已提交/审批中会被拒绝)。 */
    @DeleteMapping("/{applyId}")
    public R<Void> delete(@PathVariable String applyId) {
        service.delete(applyId);
        return R.ok();
    }

    @GetMapping("/{applyId}")
    public R<ConfirmApply> detail(@PathVariable String applyId) {
        return R.ok(service.getById(applyId));
    }

    /** 进度跟踪:申请流转历史(各节点 责任人/时间/意见 + 进度通知)。 */
    @GetMapping("/{applyId}/flow-log")
    public R<List<ConfirmFlowLog>> flowLog(@PathVariable String applyId) {
        return R.ok(flowLogService.listByApply(applyId));
    }

    @PostMapping("/page")
    public R<PageResult<ConfirmApply>> page(@RequestBody ConfirmApplyQuery query) {
        return R.ok(service.page(query));
    }

    /** 导出确权申请历史记录(CSV,按多维过滤:数据集/权属类型/状态/人员/时间范围)。 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String assetName,
                                         @RequestParam(required = false) String rightType,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String rightHolder,
                                         @RequestParam(required = false) String createTimeStart,
                                         @RequestParam(required = false) String createTimeEnd) {
        ConfirmApplyQuery query = new ConfirmApplyQuery();
        query.setAssetName(assetName);
        query.setRightType(rightType);
        query.setStatus(status);
        query.setRightHolder(rightHolder);
        query.setCreateTimeStart(createTimeStart);
        query.setCreateTimeEnd(createTimeEnd);
        byte[] data = service.exportHistory(query);
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("confirm-history.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
