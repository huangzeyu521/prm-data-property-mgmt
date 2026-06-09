package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthSealUploadLog;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.exception.BizException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 授权协议接口:生成/签章上传/审核/存档/下载(IM-DAM-DPR-03-001-003)。 */
@RestController
@RequestMapping("/api/dpr/auth/agreement")
public class AuthAgreementController {

    private final AuthAgreementService service;

    public AuthAgreementController(AuthAgreementService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public R<String> generate(@RequestParam String applyId,
                              @RequestParam(required = false) String templateId,
                              @RequestParam(required = false) String granteeOrg) {
        return R.ok(service.generate(applyId, templateId, granteeOrg));
    }

    @PostMapping("/{agreementId}/sign-grantor")
    public R<Void> signByGrantor(@PathVariable String agreementId, @RequestParam(required = false) String fileUrl) {
        service.signByGrantor(agreementId, fileUrl);
        return R.ok();
    }

    @PostMapping("/{agreementId}/sign-grantee")
    public R<Void> signByGrantee(@PathVariable String agreementId, @RequestParam(required = false) String fileUrl) {
        service.signByGrantee(agreementId, fileUrl);
        return R.ok();
    }

    /** 上传签章文件(multipart:file + role=授权方/被授权方):格式校验 + 签章有效性 + 记录。 */
    @PostMapping("/{agreementId}/upload-seal")
    public R<AuthSealUploadLog> uploadSeal(@PathVariable String agreementId,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam String role) {
        if (file == null || file.isEmpty()) {
            throw new BizException("未选择签章文件");
        }
        try {
            return R.ok(service.uploadSeal(agreementId, role, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException e) {
            throw new BizException("读取签章文件失败:" + e.getMessage());
        }
    }

    /** 下载签章文件(按上传记录)。 */
    @GetMapping("/upload-log/{logId}/file")
    public ResponseEntity<byte[]> sealFile(@PathVariable String logId) {
        byte[] data = service.downloadSeal(logId);
        String name = service.getUploadLog(logId).getFileName();
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(name == null ? "seal" : name, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /** 某协议的签章上传记录。 */
    @GetMapping("/{agreementId}/upload-logs")
    public R<List<AuthSealUploadLog>> uploadLogs(@PathVariable String agreementId) {
        return R.ok(service.listUploadLogs(agreementId));
    }

    @PostMapping("/{agreementId}/review")
    public R<Void> review(@PathVariable String agreementId, @RequestParam boolean pass,
                          @RequestParam(required = false) String opinion) {
        service.review(agreementId, pass, opinion);
        return R.ok();
    }

    /** 协议审核处理记录(审核人/结论/意见/时间)。 */
    @GetMapping("/{agreementId}/review-logs")
    public R<java.util.List<com.csg.prm.authorize.entity.AgreementReviewLog>> reviewLogs(@PathVariable String agreementId) {
        return R.ok(service.listReviewLogs(agreementId));
    }

    @PostMapping("/{agreementId}/archive")
    public R<Void> archive(@PathVariable String agreementId) {
        service.archive(agreementId);
        return R.ok();
    }

    @GetMapping("/{agreementId}")
    public R<AuthAgreement> detail(@PathVariable String agreementId) {
        return R.ok(service.getById(agreementId));
    }

    @GetMapping("/page")
    public R<PageResult<AuthAgreement>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String reviewStatus,
                                             @RequestParam(required = false) String archiveStatus,
                                             @RequestParam(required = false) String agreementType,
                                             @RequestParam(required = false) String deptName,
                                             @RequestParam(required = false) String archiveStart,
                                             @RequestParam(required = false) String archiveEnd) {
        return R.ok(service.page(current, size, reviewStatus, archiveStatus, agreementType, deptName, archiveStart, archiveEnd));
    }

    /** 记录存档访问审计(查看/下载)。 */
    @PostMapping("/{agreementId}/access")
    public R<Void> access(@PathVariable String agreementId,
                          @RequestParam(required = false) String action,
                          @RequestParam(required = false) String operator) {
        service.recordAccess(agreementId, action, operator);
        return R.ok();
    }

    /** 协议存档审计日志(归档/查看/下载)。 */
    @GetMapping("/{agreementId}/archive-logs")
    public R<java.util.List<com.csg.prm.authorize.entity.AgreementArchiveLog>> archiveLogs(@PathVariable String agreementId) {
        return R.ok(service.listArchiveLogs(agreementId));
    }
}
