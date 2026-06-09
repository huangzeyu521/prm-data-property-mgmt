package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthCertTemplate;
import com.csg.prm.authorize.service.AuthCertTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.exception.BizException;
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

/** 授权权益证书模板接口(可研 3.2.2.1.1.3.4.2)。 */
@RestController
@RequestMapping("/api/dpr/auth/cert-template")
public class AuthCertTemplateController {

    private final AuthCertTemplateService service;

    public AuthCertTemplateController(AuthCertTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody AuthCertTemplate t) {
        return R.ok(service.create(t));
    }

    @PutMapping
    public R<Void> update(@RequestBody AuthCertTemplate t) {
        service.update(t);
        return R.ok();
    }

    @DeleteMapping("/{templateId}")
    public R<Void> delete(@PathVariable String templateId) {
        service.delete(templateId);
        return R.ok();
    }

    /** 上传套版文件。 */
    @PostMapping("/{templateId}/upload-file")
    public R<Void> uploadFile(@PathVariable String templateId, @RequestParam("file") MultipartFile file) {
        try {
            service.uploadFile(templateId, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BizException("套版文件读取失败:" + e.getMessage());
        }
        return R.ok();
    }

    /** 下载套版文件。 */
    @GetMapping("/{templateId}/file")
    public ResponseEntity<byte[]> file(@PathVariable String templateId) {
        byte[] data = service.download(templateId);
        String name = service.getById(templateId).getFileName();
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(name == null ? "cert-template" : name, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @PostMapping("/{templateId}/enable")
    public R<Void> enable(@PathVariable String templateId) {
        service.enable(templateId);
        return R.ok();
    }

    @PostMapping("/{templateId}/disable")
    public R<Void> disable(@PathVariable String templateId) {
        service.disable(templateId);
        return R.ok();
    }

    @GetMapping("/page")
    public R<PageResult<AuthCertTemplate>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String templateName,
                                                @RequestParam(required = false) String certType,
                                                @RequestParam(required = false) String templateStatus) {
        return R.ok(service.page(current, size, templateName, certType, templateStatus));
    }
}
