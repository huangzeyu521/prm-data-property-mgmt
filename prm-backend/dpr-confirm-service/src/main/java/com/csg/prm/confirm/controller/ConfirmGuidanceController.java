package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.entity.ConfirmGuidance;
import com.csg.prm.confirm.service.ConfirmGuidanceService;
import jakarta.validation.Valid;
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
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 确权指引管理接口(IM-DAM-DPR-02-001-001-001):增改删查 + 上传/下载 + 历史版本。 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/guidance")
public class ConfirmGuidanceController {

    private final ConfirmGuidanceService service;

    public ConfirmGuidanceController(ConfirmGuidanceService service) {
        this.service = service;
    }

    /** 新增(无id)/修改(带id)。 */
    @PostMapping
    public Result<String> save(@Valid @RequestBody ConfirmGuidance g) {
        return Result.success(service.save(g));
    }

    /** 上传文件作为新版本(multipart:file + 标题/类型/发布人/内容)。 */
    @PostMapping("/upload-file")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam String title,
                                @RequestParam(required = false) String guidanceType,
                                @RequestParam(required = false) String publisher,
                                @RequestParam(required = false) String content) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("未选择文件");
        }
        ConfirmGuidance meta = new ConfirmGuidance();
        meta.setTitle(title);
        meta.setGuidanceType(guidanceType);
        meta.setPublisher(publisher);
        meta.setContent(content);
        try {
            return Result.success(service.uploadFile(meta, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败:" + e.getMessage());
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

    /** 在线阅览原件(inline:PDF 浏览器内嵌,其余按二进制流)。供"工作指引存档"在线查看。 */
    @GetMapping("/{guidanceId}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable String guidanceId) {
        ConfirmGuidance g = service.getById(guidanceId);
        byte[] data = service.download(guidanceId);
        String fn = g.getFileName() == null ? guidanceId : g.getFileName();
        MediaType mt = fn.toLowerCase().endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition cd = ContentDisposition.inline().filename(fn, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .header("Content-Disposition", cd.toString())
                .contentType(mt)
                .body(data);
    }

    @DeleteMapping("/{guidanceId}")
    public Result<Void> delete(@PathVariable String guidanceId) {
        service.delete(guidanceId);
        return Result.success();
    }

    @GetMapping("/{guidanceId}")
    public Result<ConfirmGuidance> detail(@PathVariable String guidanceId) {
        return Result.success(service.getById(guidanceId));
    }

    /** 同标题历史版本。 */
    @GetMapping("/versions")
    public Result<List<ConfirmGuidance>> versions(@RequestParam String title) {
        return Result.success(service.versions(title));
    }

    /** 将某版本设为最新。 */
    @PostMapping("/{guidanceId}/set-latest")
    public Result<Void> setLatest(@PathVariable String guidanceId) {
        service.setLatest(guidanceId);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<ConfirmGuidance>> page(@Valid PageRequest page,
                                               @RequestParam(required = false) String title,
                                               @RequestParam(required = false) String guidanceType,
                                               @RequestParam(required = false) String excludeType,
                                               @RequestParam(defaultValue = "true") boolean latestOnly) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), title, guidanceType, excludeType, latestOnly));
    }
}
