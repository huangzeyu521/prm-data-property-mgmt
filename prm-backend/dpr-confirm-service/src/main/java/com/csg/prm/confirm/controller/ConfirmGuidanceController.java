package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmGuidance;
import com.csg.prm.confirm.service.ConfirmGuidanceService;
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

/** 确权指引管理接口(IM-DAM-DPR-02-001-001-001):增改删查 + 上传/下载 + 历史版本。 */
@RestController
@RequestMapping("/api/dpr/confirm/guidance")
public class ConfirmGuidanceController {

    private final ConfirmGuidanceService service;

    public ConfirmGuidanceController(ConfirmGuidanceService service) {
        this.service = service;
    }

    /** 新增(无id)/修改(带id)。 */
    @PostMapping
    public R<String> save(@RequestBody ConfirmGuidance g) {
        return R.ok(service.save(g));
    }

    /** 上传文件作为新版本(multipart:file + 标题/类型/发布人/内容)。 */
    @PostMapping("/upload-file")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam String title,
                                @RequestParam(required = false) String guidanceType,
                                @RequestParam(required = false) String publisher,
                                @RequestParam(required = false) String content) {
        if (file == null || file.isEmpty()) {
            throw new BizException("未选择文件");
        }
        ConfirmGuidance meta = new ConfirmGuidance();
        meta.setTitle(title);
        meta.setGuidanceType(guidanceType);
        meta.setPublisher(publisher);
        meta.setContent(content);
        try {
            return R.ok(service.uploadFile(meta, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException e) {
            throw new BizException("读取上传文件失败:" + e.getMessage());
        }
    }

    /** 下载/预览原件。 */
    @GetMapping("/{guidanceId}/download")
    public ResponseEntity<byte[]> download(@PathVariable String guidanceId) {
        ConfirmGuidance g = service.getById(guidanceId);
        byte[] data = service.download(guidanceId);
        String fn = g.getFileName() == null ? guidanceId : g.getFileName();
        ContentDisposition cd = ContentDisposition.attachment()
                .filename(fn, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header("Content-Disposition", cd.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{guidanceId}")
    public R<Void> delete(@PathVariable String guidanceId) {
        service.delete(guidanceId);
        return R.ok();
    }

    @GetMapping("/{guidanceId}")
    public R<ConfirmGuidance> detail(@PathVariable String guidanceId) {
        return R.ok(service.getById(guidanceId));
    }

    /** 同标题历史版本。 */
    @GetMapping("/versions")
    public R<List<ConfirmGuidance>> versions(@RequestParam String title) {
        return R.ok(service.versions(title));
    }

    /** 将某版本设为最新。 */
    @PostMapping("/{guidanceId}/set-latest")
    public R<Void> setLatest(@PathVariable String guidanceId) {
        service.setLatest(guidanceId);
        return R.ok();
    }

    @GetMapping("/page")
    public R<PageResult<ConfirmGuidance>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String title,
                                               @RequestParam(required = false) String guidanceType,
                                               @RequestParam(defaultValue = "true") boolean latestOnly) {
        return R.ok(service.page(current, size, title, guidanceType, latestOnly));
    }
}
