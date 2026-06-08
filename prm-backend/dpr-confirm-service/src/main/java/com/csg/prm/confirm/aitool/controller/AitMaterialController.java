package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.csg.prm.common.exception.BizException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能确权辅助工具-材料智能解析接口(M1 / SW-001~004)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/material")
public class AitMaterialController {

    private final AitMaterialService service;

    public AitMaterialController(AitMaterialService service) {
        this.service = service;
    }

    /** 批量上传单次最多文件数(#1) */
    private static final int MAX_BATCH = 50;

    @PostMapping("/upload")
    public R<String> upload(@RequestBody AitMaterial material) {
        return R.ok(service.upload(material));
    }

    /** 真实文件上传(单)(#1):multipart 表单字段 file,可选 applyId */
    @PostMapping("/upload-file")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String applyId) {
        return R.ok(saveOne(file, applyId, null));
    }

    /** 真实文件批量上传(#1):multipart 字段 files[],单次≤50 个 */
    @PostMapping("/upload-batch")
    public R<List<String>> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                       @RequestParam(required = false) String applyId) {
        if (files == null || files.length == 0) {
            throw new BizException("未选择文件");
        }
        if (files.length > MAX_BATCH) {
            throw new BizException("批量上传单次不超过 " + MAX_BATCH + " 个文件(本次 " + files.length + ")");
        }
        String batchNo = "BATCH-" + System.currentTimeMillis();
        List<String> ids = new ArrayList<>();
        for (MultipartFile f : files) {
            ids.add(saveOne(f, applyId, batchNo));
        }
        return R.ok(ids);
    }

    private String saveOne(MultipartFile file, String applyId, String batchNo) {
        if (file == null || file.isEmpty()) {
            throw new BizException("文件为空");
        }
        try {
            return service.uploadBinary(file.getOriginalFilename(), file.getBytes(), applyId, batchNo);
        } catch (IOException e) {
            throw new BizException("读取上传文件失败:" + e.getMessage());
        }
    }

    /** 下载/预览已上传的原件(#1) */
    @GetMapping("/{materialId}/file")
    public ResponseEntity<byte[]> file(@PathVariable String materialId) {
        AitMaterial m = service.getMaterial(materialId);
        byte[] data = service.loadFile(materialId);
        String fn = URLEncoder.encode(m.getFileName() == null ? materialId : m.getFileName(),
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /** 触发解析(异步):立即返回,前端轮询 /progress 获取 0–100% 进度(#2) */
    @PostMapping("/{materialId}/parse")
    public R<Void> parse(@PathVariable String materialId) {
        service.submitParse(materialId);
        return R.ok();
    }

    /** 解析进度轮询(#2):返回解析状态、进度百分比、失败原因 */
    @GetMapping("/{materialId}/progress")
    public R<AitMaterial> progress(@PathVariable String materialId) {
        return R.ok(service.getMaterial(materialId));
    }

    @GetMapping("/{materialId}/parse")
    public R<AitParseResult> parseResult(@PathVariable String materialId) {
        return R.ok(service.getParse(materialId));
    }

    /** 导出解析结果为 Excel(#8) */
    @GetMapping("/{materialId}/export")
    public ResponseEntity<byte[]> export(@PathVariable String materialId) {
        byte[] data = service.exportParseExcel(materialId);
        String fn = URLEncoder.encode("解析结果-" + materialId + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/{materialId}/term-check")
    public R<List<AitMaterialService.TermSuggestion>> termCheck(@PathVariable String materialId) {
        return R.ok(service.termCheck(materialId));
    }

    @GetMapping("/{materialId}/compares")
    public R<List<AitCompare>> compares(@PathVariable String materialId) {
        return R.ok(service.compares(materialId));
    }

    @GetMapping("/page")
    public R<PageResult<AitMaterial>> page(PageQuery query,
                                           @RequestParam(required = false) String batchNo,
                                           @RequestParam(required = false) String parseStatus,
                                           @RequestParam(required = false) String applyId) {
        return R.ok(service.page(query, batchNo, parseStatus, applyId));
    }
}
