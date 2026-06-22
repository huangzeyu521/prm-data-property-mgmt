package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.dto.MaterialCheckReport;
import com.csg.prm.confirm.dto.MaterialSyncReport;
import com.csg.prm.confirm.entity.ConfirmMaterialRule;
import com.csg.prm.confirm.service.ConfirmMaterialRuleService;
import com.csg.prm.confirm.service.ConfirmMaterialService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 确权申请材料 + 材料校验接口(IM-DAM-DPR-02-001-001-004/005)。 */
@RestController
@RequestMapping("/api/dpr/confirm/material")
public class ConfirmMaterialController {

    private final ConfirmMaterialService service;
    private final ConfirmMaterialRuleService ruleService;

    public ConfirmMaterialController(ConfirmMaterialService service, ConfirmMaterialRuleService ruleService) {
        this.service = service;
        this.ruleService = ruleService;
    }

    /** 应交材料清单规则(单一真源):前端据此构建 A–J 应交清单,不再前端硬编码。 */
    @GetMapping("/rule")
    public R<List<ConfirmMaterialRule>> rules(
            @RequestParam(defaultValue = "确权") String scene) {
        return R.ok(ruleService.listEnabled(scene));
    }

    @PostMapping
    public R<String> upload(@RequestBody ConfirmMaterial m) {
        return R.ok(service.upload(m));
    }

    /** 真实文件上传(multipart:file + applyId/材料名称/类型/所有者),含格式验证。 */
    @PostMapping("/upload-file")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam String applyId,
                                @RequestParam(required = false) String materialName,
                                @RequestParam(required = false) String materialType,
                                @RequestParam(required = false) String owner) {
        if (file == null || file.isEmpty()) {
            throw new BizException("未选择文件");
        }
        ConfirmMaterial meta = new ConfirmMaterial();
        meta.setApplyId(applyId);
        meta.setMaterialName(materialName);
        meta.setMaterialType(materialType);
        meta.setOwner(owner);
        try {
            return R.ok(service.uploadFile(meta, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException e) {
            throw new BizException("读取上传文件失败:" + e.getMessage());
        }
    }

    /** 在线预览/下载原件。 */
    @GetMapping("/{materialId}/file")
    public ResponseEntity<byte[]> file(@PathVariable String materialId) {
        byte[] data = service.download(materialId);
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.inline().filename(materialId, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{materialId}")
    public R<Void> delete(@PathVariable String materialId) {
        service.delete(materialId);
        return R.ok();
    }

    @GetMapping("/by-apply/{applyId}")
    public R<List<ConfirmMaterial>> listByApply(@PathVariable String applyId) {
        return R.ok(service.listByApply(applyId));
    }

    @PostMapping("/{materialId}/check")
    public R<Void> check(@PathVariable String materialId,
                         @RequestParam boolean pass,
                         @RequestParam(required = false) String abnormalDesc) {
        service.check(materialId, pass, abnormalDesc);
        return R.ok();
    }

    /** 规则化材料校验:自动识别缺失项/不合规项,返回校验报告。 */
    @PostMapping("/check-run")
    public R<MaterialCheckReport> checkRun(@RequestParam String applyId) {
        return R.ok(service.runCheck(applyId));
    }

    /**
     * 先从数据资产管理平台元数据(AU_TABLE_META_DATA)同步该申请已上传材料:命中应交项的平台附件
     * 自动登记为"平台同步"免上传,返回同步明细 + 仍待用户补全清单。幂等,可重复调用。
     */
    @PostMapping("/sync-platform")
    public R<MaterialSyncReport> syncPlatform(@RequestParam String applyId) {
        return R.ok(service.syncFromPlatform(applyId));
    }

    /** 材料 AI 校验:qwen3-max 逐份校验完整性/合规性/与表单一致性(stub 回退),返回严格 JSON */
    @PostMapping("/ai-check")
    public R<String> aiCheck(@RequestParam String applyId) {
        return R.ok(service.aiCheck(applyId));
    }

    /** 推送审核(后端门禁):校验全通过才提交审核。 */
    @PostMapping("/push-review")
    public R<Void> pushReview(@RequestParam String applyId) {
        service.pushReview(applyId);
        return R.ok();
    }

    /** 导出材料校验结果(CSV)。 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String applyId) {
        byte[] data = service.exportCheck(applyId);
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("material-check-" + applyId + ".csv", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    @GetMapping("/page")
    public R<PageResult<ConfirmMaterial>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String applyId,
                                               @RequestParam(required = false) String checkResult) {
        return R.ok(service.page(current, size, applyId, checkResult));
    }
}
