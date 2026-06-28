package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitDocTemplate;
import com.csg.prm.confirm.aitool.service.AitDocTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 智能确权辅助工具-资料模板库接口(1.4#3):模板 CRUD + 在线编辑 + 版本管理 + 下载。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/aitool/template")
public class AitDocTemplateController {

    private final AitDocTemplateService service;

    public AitDocTemplateController(AitDocTemplateService service) {
        this.service = service;
    }

    @GetMapping("/page")
    public Result<PageResult<AitDocTemplate>> page(@Valid PageQuery query,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(defaultValue = "true") boolean onlyLatest) {
        return Result.success(service.page(query, type, name, onlyLatest));
    }

    @GetMapping("/{id}")
    public Result<AitDocTemplate> get(@PathVariable String id) {
        return Result.success(service.getById(id));
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody AitDocTemplate t) {
        return Result.success(service.create(t));
    }

    /** 在线编辑当前版本。 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AitDocTemplate t) {
        service.updateContent(t);
        return Result.success();
    }

    /** 发布新版本(版本号自增)。 */
    @PostMapping("/new-version")
    public Result<String> newVersion(@Valid @RequestBody AitDocTemplate t) {
        return Result.success(service.newVersion(t));
    }

    @PostMapping("/{id}/set-latest")
    public Result<Void> setLatest(@PathVariable String id) {
        service.setLatest(id);
        return Result.success();
    }

    @GetMapping("/versions")
    public Result<List<AitDocTemplate>> versions(@RequestParam String templateName) {
        return Result.success(service.versions(templateName));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        AitDocTemplate t = service.getById(id);
        byte[] data = service.download(id);
        String base = (t.getFileName() != null) ? t.getFileName() : (t.getTemplateName() + "-" + t.getVersion() + ".txt");
        String fn = URLEncoder.encode(base, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
