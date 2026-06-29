package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.dto.AuthComplianceReport;
import com.csg.prm.authorize.entity.AuthCompliance;
import com.csg.prm.authorize.service.AuthComplianceService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** 授权合规校验接口(IM-DAM-DPR-03-001-001-006):规则化三维自动校验 + 报告 + 导出。 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/compliance")
public class AuthComplianceController {

    private final AuthComplianceService service;

    public AuthComplianceController(AuthComplianceService service) {
        this.service = service;
    }

    /** 规则化自动校验,返回三维报告。 */
    @PostMapping("/check")
    public Result<AuthComplianceReport> check(@RequestParam String applyId) {
        return Result.success(service.runCheck(applyId));
    }

    /** 导出校验记录(CSV)。 */
    /** 合规 AI 预审意见(规则结果+上下文交大模型,非门禁) */
    @PostMapping("/pre-review")
    public Result<String> preReview(@RequestParam String applyId) {
        return Result.success(service.preReview(applyId));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String applyId,
                                         @RequestParam(required = false) String riskLevel) {
        byte[] data = service.exportRecords(applyId, riskLevel);
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("auth-compliance.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    @GetMapping("/page")
    public Result<PageResult<AuthCompliance>> page(@Valid PageRequest page,
                                              @RequestParam(required = false) String applyId,
                                              @RequestParam(required = false) String riskLevel) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), applyId, riskLevel));
    }
}
