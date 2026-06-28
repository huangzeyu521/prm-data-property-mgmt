package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.confirm.dto.ConfirmApplyQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmFlowLog;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmFlowLogService;

import jakarta.validation.Valid;
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
import org.springframework.validation.annotation.Validated;

/**
 * 数据确权申请与审批接口(确权申请 IM-DAM-DPR-02-001-001 / 确权审核 -002)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/apply")
public class ConfirmApplyController {

    private final ConfirmApplyService service;
    private final ConfirmFlowLogService flowLogService;
    private final com.csg.prm.confirm.service.ConfirmAiSnapshotService aiSnapshotService;

    public ConfirmApplyController(ConfirmApplyService service, ConfirmFlowLogService flowLogService,
                                  com.csg.prm.confirm.service.ConfirmAiSnapshotService aiSnapshotService) {
        this.service = service;
        this.flowLogService = flowLogService;
        this.aiSnapshotService = aiSnapshotService;
    }

    @PostMapping("/draft")
    public Result<String> saveDraft(@Valid @RequestBody ConfirmApply apply) {
        return Result.success(service.saveDraft(apply));
    }

    /** 元数据自动填充(含质量评分):用于确权表单预填 + 提交前质量门禁提示 */
    @GetMapping("/autofill")
    public Result<com.csg.prm.confirm.gateway.MetadataGateway.MetadataInfo> autofill(@RequestParam String assetId) {
        return Result.success(service.autofill(assetId));
    }

    /** 权益事实(确权信息带出):按资产返回第三方来源/隐私商密事实,供授权侧只读带出。 */
    @GetMapping("/rights-facts")
    public Result<com.csg.prm.confirm.dto.RightsFactsVO> rightsFacts(@RequestParam String assetId) {
        return Result.success(service.rightsFacts(assetId));
    }

    /**
     * 派生重确权工单(草稿)。供权益动态监测识别数据新增/来源变更/到期时联动调用(附录F 3.3.2)。
     */
    @PostMapping("/re-confirm")
    public Result<String> reConfirm(@RequestParam String assetId,
                               @RequestParam(required = false) String assetName,
                               @RequestParam(required = false) String rightType,
                               @RequestParam(required = false) String reason,
                               @RequestParam(required = false) String sourceRef,
                               @RequestParam(required = false) String changeTrigger) {
        return Result.success(service.createReConfirm(assetId, assetName, rightType, reason, sourceRef, changeTrigger));
    }

    /** 固化提交前 AI 校验结果快照(JSON):服务端计 SM3 + 上链存证 + 关联逐次留痕,防篡改、可审计。提交前调用。 */
    @PostMapping("/{applyId}/ai-snapshot")
    public Result<Void> saveAiSnapshot(@PathVariable String applyId, @RequestBody String snapshotJson) {
        aiSnapshotService.save(applyId, snapshotJson);
        return Result.success();
    }

    /** 校验已固化 AI 校验快照的完整性(重算 SM3 比对存证),供人工预审/审计验真防篡改。 */
    @GetMapping("/{applyId}/ai-snapshot/verify")
    public Result<java.util.Map<String, Object>> verifyAiSnapshot(@PathVariable String applyId) {
        return Result.success(aiSnapshotService.verify(applyId));
    }

    @PostMapping("/{applyId}/submit")
    public Result<Void> submit(@PathVariable String applyId) {
        service.submit(applyId);
        return Result.success();
    }

    /** 批量提交草稿至审核。 */
    @PostMapping("/batch-submit")
    public Result<com.csg.prm.confirm.dto.BatchResult> batchSubmit(@RequestBody List<String> applyIds) {
        return Result.success(service.batchSubmit(applyIds));
    }

    /** 批量审批通过。 */
    @com.csg.prm.common.auth.RequiresRole({"precheck", "review", "manager", "director", "admin"})
    @PostMapping("/batch-approve")
    public Result<com.csg.prm.confirm.dto.BatchResult> batchApprove(@RequestBody List<String> applyIds) {
        return Result.success(service.batchApprove(applyIds));
    }

    /** 批量驳回(统一原因)。 */
    @com.csg.prm.common.auth.RequiresRole({"precheck", "review", "manager", "director", "admin"})
    @PostMapping("/batch-reject")
    public Result<com.csg.prm.confirm.dto.BatchResult> batchReject(@RequestBody List<String> applyIds,
                                                              @RequestParam(required = false) String reason) {
        return Result.success(service.batchReject(applyIds, reason));
    }

    // 控制器层=粗门禁(是否审批人即可);精确"仅本节点角色"由 service 层 assertNodeRole 判定。
    // 须含 precheck/manager/director,否则人工预审/主管复核/经理终审节点的单角色账号会被控制器 403 挡死(逐节点门禁失效)。
    @com.csg.prm.common.auth.RequiresRole({"precheck", "review", "manager", "director", "admin"})
    @PostMapping("/{applyId}/approve")
    public Result<String> approve(@PathVariable String applyId) {
        return Result.success(service.approve(applyId));
    }

    @com.csg.prm.common.auth.RequiresRole({"precheck", "review", "manager", "director", "admin"})
    @PostMapping("/{applyId}/reject")
    public Result<Void> reject(@PathVariable String applyId, @RequestParam(required = false) String reason) {
        service.reject(applyId, reason);
        return Result.success();
    }

    /** 申请人主动撤回(审批中 -> 已撤回);仅申请人本人,撤回后可重新编辑提交。 */
    @com.csg.prm.common.auth.RequiresRole({"apply", "admin"})
    @PostMapping("/{applyId}/withdraw")
    public Result<Void> withdraw(@PathVariable String applyId, @RequestParam(required = false) String reason) {
        service.withdraw(applyId, reason);
        return Result.success();
    }

    /** 删除草稿确权申请(仅草稿状态;已提交/审批中会被拒绝)。 */
    @DeleteMapping("/{applyId}")
    public Result<Void> delete(@PathVariable String applyId) {
        service.delete(applyId);
        return Result.success();
    }

    @GetMapping("/{applyId}")
    public Result<ConfirmApply> detail(@PathVariable String applyId) {
        return Result.success(service.getById(applyId));
    }

    /** 进度跟踪:申请流转历史(各节点 责任人/时间/意见 + 进度通知)。 */
    @GetMapping("/{applyId}/flow-log")
    public Result<List<ConfirmFlowLog>> flowLog(@PathVariable String applyId) {
        return Result.success(flowLogService.listByApply(applyId));
    }

    @PostMapping("/page")
    public Result<PageResult<ConfirmApply>> page(@Valid @RequestBody ConfirmApplyQuery query) {
        return Result.success(service.page(query));
    }

    /** 确权申请概览统计(总/在途/已完成/已驳回/初始/变更),按过滤条件聚合(忽略 status),供查询页概览条。 */
    @PostMapping("/stats")
    public Result<com.csg.prm.confirm.dto.ConfirmApplyStats> stats(@RequestBody ConfirmApplyQuery query) {
        return Result.success(service.stats(query));
    }

    /** 导出确权申请历史记录(CSV,按多维过滤:数据集/权属类型/状态/人员/时间范围)。 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String assetName,
                                         @RequestParam(required = false) String rightType,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String rightHolder,
                                         @RequestParam(required = false) String createTimeStart,
                                         @RequestParam(required = false) String createTimeEnd,
                                         @RequestParam(required = false) String registerType,
                                         @RequestParam(required = false) String changeTrigger) {
        ConfirmApplyQuery query = new ConfirmApplyQuery();
        query.setAssetName(assetName);
        query.setRightType(rightType);
        query.setStatus(status);
        query.setRightHolder(rightHolder);
        query.setCreateTimeStart(createTimeStart);
        query.setCreateTimeEnd(createTimeEnd);
        query.setRegisterType(registerType);
        query.setChangeTrigger(changeTrigger);
        byte[] data = service.exportHistory(query);
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("confirm-history.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
