package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthAgreementTemplate;
import com.csg.prm.authorize.service.AuthAgreementTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.query.PageQuery;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** 授权协议模板库接口(可研 3.2.2.1.1.3.3.1):分类/版本/内容 + 上传下载。 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/agreement-template")
public class AuthAgreementTemplateController {

    private final AuthAgreementTemplateService service;

    public AuthAgreementTemplateController(AuthAgreementTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody AuthAgreementTemplate t) {
        return Result.success(service.create(t));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody AuthAgreementTemplate t) {
        service.update(t);
        return Result.success();
    }

    @DeleteMapping("/{templateId}")
    public Result<Void> delete(@PathVariable String templateId) {
        service.delete(templateId);
        return Result.success();
    }

    @PostMapping("/{templateId}/enable")
    public Result<Void> enable(@PathVariable String templateId) {
        service.enable(templateId);
        return Result.success();
    }

    @PostMapping("/{templateId}/disable")
    public Result<Void> disable(@PathVariable String templateId) {
        service.disable(templateId);
        return Result.success();
    }

    /** 上传套版文件。 */
    @PostMapping("/{templateId}/upload-file")
    public Result<Void> uploadFile(@PathVariable String templateId, @RequestParam("file") MultipartFile file) {
        try {
            service.uploadFile(templateId, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BusinessException("套版文件读取失败:" + e.getMessage());
        }
        return Result.success();
    }

    /** 下载套版文件。 */
    @GetMapping("/{templateId}/file")
    public ResponseEntity<byte[]> file(@PathVariable String templateId) {
        byte[] data = service.download(templateId);
        String name = service.getById(templateId).getFileName();
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(name == null ? "agreement-template" : name, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/{templateId}")
    public Result<AuthAgreementTemplate> detail(@PathVariable String templateId) {
        return Result.success(service.getById(templateId));
    }

    @GetMapping("/page")
    public Result<PageResult<AuthAgreementTemplate>> page(@Valid PageQuery page,
                                                     @RequestParam(required = false) String templateName,
                                                     @RequestParam(required = false) String authType,
                                                     @RequestParam(required = false) String purpose,
                                                     @RequestParam(required = false) String status) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), templateName, authType, purpose, status));
    }
}
