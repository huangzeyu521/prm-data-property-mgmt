package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthSealUploadLog;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.query.PageQuery;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/dpr/auth/agreement")
public class AuthAgreementController {

    private final AuthAgreementService service;

    public AuthAgreementController(AuthAgreementService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public Result<String> generate(@RequestParam String applyId,
                              @RequestParam(required = false) String templateId,
                              @RequestParam(required = false) String granteeOrg) {
        return Result.success(service.generate(applyId, templateId, granteeOrg));
    }

    /** 批量授权:一份批量清单生成一份《运营授权协议》(一清单一协议,清单各项=协议附件)。 */
    @PostMapping("/generate-for-batch")
    public Result<String> generateForBatch(@RequestParam String batchListId) {
        return Result.success(service.generateForBatch(batchListId));
    }

    @PostMapping("/{agreementId}/sign-grantor")
    public Result<Void> signByGrantor(@PathVariable String agreementId, @RequestParam(required = false) String fileUrl) {
        service.signByGrantor(agreementId, fileUrl);
        return Result.success();
    }

    @PostMapping("/{agreementId}/sign-grantee")
    public Result<Void> signByGrantee(@PathVariable String agreementId, @RequestParam(required = false) String fileUrl) {
        service.signByGrantee(agreementId, fileUrl);
        return Result.success();
    }

    /** 上传签章文件(multipart:file + role=授权方/被授权方):格式校验 + 签章有效性 + 记录。 */
    @PostMapping("/{agreementId}/upload-seal")
    public Result<AuthSealUploadLog> uploadSeal(@PathVariable String agreementId,
                                           @RequestParam("file") MultipartFile file,
                                           @RequestParam String role) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("未选择签章文件");
        }
        try {
            return Result.success(service.uploadSeal(agreementId, role, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException e) {
            throw new BusinessException("读取签章文件失败:" + e.getMessage());
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
    public Result<List<AuthSealUploadLog>> uploadLogs(@PathVariable String agreementId) {
        return Result.success(service.listUploadLogs(agreementId));
    }

    /** 协议审核(通过/驳回重签)。申报人只做签章/上传,不得自审。 */
    @com.csg.prm.common.auth.RequiresRole({"review", "admin"})
    @PostMapping("/{agreementId}/review")
    public Result<Void> review(@PathVariable String agreementId, @RequestParam boolean pass,
                          @RequestParam(required = false) String opinion) {
        service.review(agreementId, pass, opinion);
        return Result.success();
    }

    /** 协议审核处理记录(审核人/结论/意见/时间)。 */
    @GetMapping("/{agreementId}/review-logs")
    public Result<java.util.List<com.csg.prm.authorize.entity.AgreementReviewLog>> reviewLogs(@PathVariable String agreementId) {
        return Result.success(service.listReviewLogs(agreementId));
    }

    @PostMapping("/{agreementId}/archive")
    public Result<Void> archive(@PathVariable String agreementId) {
        service.archive(agreementId);
        return Result.success();
    }

    @GetMapping("/{agreementId}")
    public Result<AuthAgreement> detail(@PathVariable String agreementId) {
        return Result.success(service.getById(agreementId));
    }

    /** 协议要素核对(附录D §3.4.4):协议 + 来源申请单的 数据范围/场景/目的/利益分配/安全保障,供协议审核核对一致性。 */
    @GetMapping("/{agreementId}/elements")
    public Result<com.csg.prm.authorize.dto.AgreementElementsVO> elements(@PathVariable String agreementId) {
        return Result.success(service.elements(agreementId));
    }

    @GetMapping("/page")
    public Result<PageResult<AuthAgreement>> page(@Valid PageQuery page,
                                             @RequestParam(required = false) String reviewStatus,
                                             @RequestParam(required = false) String archiveStatus,
                                             @RequestParam(required = false) String agreementType,
                                             @RequestParam(required = false) String deptName,
                                             @RequestParam(required = false) String archiveStart,
                                             @RequestParam(required = false) String archiveEnd) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), reviewStatus, archiveStatus, agreementType, deptName, archiveStart, archiveEnd));
    }

    /** 记录存档访问审计(查看/下载)。 */
    @PostMapping("/{agreementId}/access")
    public Result<Void> access(@PathVariable String agreementId,
                          @RequestParam(required = false) String action,
                          @RequestParam(required = false) String operator) {
        service.recordAccess(agreementId, action, operator);
        return Result.success();
    }

    /** 协议存档审计日志(归档/查看/下载)。 */
    @GetMapping("/{agreementId}/archive-logs")
    public Result<java.util.List<com.csg.prm.authorize.entity.AgreementArchiveLog>> archiveLogs(@PathVariable String agreementId) {
        return Result.success(service.listArchiveLogs(agreementId));
    }
}
