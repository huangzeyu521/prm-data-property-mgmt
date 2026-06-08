package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.EquityCertTemplate;
import com.csg.prm.confirm.service.EquityCertTemplateService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/** 确权权益证书模板接口(IM-DAM-DPR-02-001-003 模板)。 */
@RestController
@RequestMapping("/api/dpr/confirm/cert-template")
public class EquityCertTemplateController {

    private final EquityCertTemplateService service;

    public EquityCertTemplateController(EquityCertTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody EquityCertTemplate t) {
        return R.ok(service.create(t));
    }

    @PutMapping
    public R<Void> update(@RequestBody EquityCertTemplate t) {
        service.update(t);
        return R.ok();
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

    /** 上传模板套版文件(PDF/Word/图片)。 */
    @PostMapping("/{templateId}/upload-file")
    public R<Void> uploadFile(@PathVariable String templateId, @RequestParam("file") MultipartFile file) {
        try {
            service.uploadFile(templateId, file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new BizException("套版文件读取失败:" + e.getMessage());
        }
        return R.ok();
    }

    /** 下载模板套版文件。 */
    @GetMapping("/{templateId}/file")
    public ResponseEntity<byte[]> file(@PathVariable String templateId) {
        byte[] data = service.download(templateId);
        String name = service.getById(templateId).getFileName();
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename(name == null ? "template" : name, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @GetMapping("/page")
    public R<PageResult<EquityCertTemplate>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String templateName,
                                                  @RequestParam(required = false) String templateStatus) {
        return R.ok(service.page(current, size, templateName, templateStatus));
    }
}
