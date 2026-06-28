package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitDocSegment;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.service.AitAccuracyService;
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
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import com.csg.prm.common.exception.BusinessException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能确权辅助工具-材料智能解析接口(M1 / SW-001~004)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/aitool/material")
public class AitMaterialController {

    private final AitMaterialService service;
    private final AitAccuracyService accuracyService;

    public AitMaterialController(AitMaterialService service, AitAccuracyService accuracyService) {
        this.service = service;
        this.accuracyService = accuracyService;
    }

    /** 批量上传单次最多文件数(#1) */
    private static final int MAX_BATCH = 50;

    @PostMapping("/upload")
    public Result<String> upload(@Valid @RequestBody AitMaterial material) {
        return Result.success(service.upload(material));
    }

    /** 真实文件上传(单)(#1):multipart 表单字段 file,可选 applyId */
    @PostMapping("/upload-file")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam(required = false) String applyId,
                                @RequestParam(required = false) String assetId) {
        return Result.success(saveOne(file, applyId, assetId, null));
    }

    /** 真实文件批量上传(#1):multipart 字段 files[],单次≤50 个 */
    @PostMapping("/upload-batch")
    public Result<List<String>> uploadBatch(@RequestParam("files") MultipartFile[] files,
                                       @RequestParam(required = false) String applyId,
                                       @RequestParam(required = false) String assetId) {
        if (files == null || files.length == 0) {
            throw new BusinessException("未选择文件");
        }
        if (files.length > MAX_BATCH) {
            throw new BusinessException("批量上传单次不超过 " + MAX_BATCH + " 个文件(本次 " + files.length + ")");
        }
        String batchNo = "BATCH-" + System.currentTimeMillis();
        List<String> ids = new ArrayList<>();
        for (MultipartFile f : files) {
            ids.add(saveOne(f, applyId, assetId, batchNo));
        }
        return Result.success(ids);
    }

    private String saveOne(MultipartFile file, String applyId, String assetId, String batchNo) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件为空");
        }
        try {
            return service.uploadBinary(fixFilename(file.getOriginalFilename()), file.getBytes(), applyId, assetId, batchNo);
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败:" + e.getMessage());
        }
    }

    /**
     * 还原 multipart 文件名编码。Tomcat 默认按 ISO-8859-1 解码 Content-Disposition 的 filename,
     * 中文 UTF-8 字节被误读成乱码。仅当文件名所有字符都落在 Latin-1 区间(即典型的误解码特征)时,
     * 才按 ISO-8859-1→UTF-8 重解;若已含真实 CJK 字符(>0xFF)则原样返回,避免破坏已正确的文件名。
     */
    static String fixFilename(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        boolean allLatin1 = name.chars().allMatch(c -> c <= 0xFF);
        if (!allLatin1) {
            return name;
        }
        String recovered = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return recovered.indexOf('�') >= 0 ? name : recovered;
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
    public Result<Void> parse(@PathVariable String materialId) {
        // 派发异步前同步校验材料存在:@Async 线程内的"材料不存在"异常无人接收,否则前端误得"成功"
        service.getMaterial(materialId);
        service.submitParse(materialId);
        return Result.success();
    }

    /** 解析进度轮询(#2):返回解析状态、进度百分比、失败原因 */
    @GetMapping("/{materialId}/progress")
    public Result<AitMaterial> progress(@PathVariable String materialId) {
        return Result.success(service.getMaterial(materialId));
    }

    @GetMapping("/{materialId}/parse")
    public Result<AitParseResult> parseResult(@PathVariable String materialId) {
        return Result.success(service.getParse(materialId));
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
    public Result<List<AitMaterialService.TermSuggestion>> termCheck(@PathVariable String materialId) {
        return Result.success(service.termCheck(materialId));
    }

    /** 人工确认修改(#4):把某要素采用为标准术语,写回解析结果。 */
    @PostMapping("/{materialId}/term-confirm")
    public Result<Void> termConfirm(@PathVariable String materialId,
                               @RequestParam String field,
                               @RequestParam String standardTerm) {
        service.confirmTerm(materialId, field, standardTerm);
        return Result.success();
    }

    @GetMapping("/{materialId}/compares")
    public Result<List<AitCompare>> compares(@PathVariable String materialId) {
        return Result.success(service.compares(materialId));
    }

    @GetMapping("/page")
    public Result<PageResult<AitMaterial>> page(@Valid PageQuery query,
                                           @RequestParam(required = false) String batchNo,
                                           @RequestParam(required = false) String parseStatus,
                                           @RequestParam(required = false) String applyId) {
        return Result.success(service.page(query, batchNo, parseStatus, applyId));
    }

    /** #5 多粒度解析片段(granularity 可空=全部:PAGE/PARAGRAPH/CELL/TABLE/TITLE) */
    @GetMapping("/{materialId}/segments")
    public Result<List<AitDocSegment>> segments(@PathVariable String materialId,
                                           @RequestParam(required = false) String granularity) {
        return Result.success(service.segments(materialId, granularity));
    }

    /** #4 按数据表归集关联(applyId 或 dataTableRef 任一过滤) */
    @GetMapping("/aggregate")
    public Result<List<AitMaterialService.MaterialGroup>> aggregate(@RequestParam(required = false) String applyId,
                                                               @RequestParam(required = false) String dataTableRef) {
        return Result.success(service.aggregate(applyId, dataTableRef));
    }

    /** #7 解析准确度评测:对标注样本集逐字段比对,输出整体/分字段准确率与是否达标(≥95%)。 */
    @GetMapping("/accuracy")
    public Result<AitAccuracyService.AccuracyReport> accuracy() {
        return Result.success(accuracyService.evaluate());
    }

    /** 1.4#2 批量解析:按批次号排队解析其下全部未成功材料,返回派发数量。 */
    @PostMapping("/batch-parse")
    public Result<Integer> batchParse(@RequestParam String batchNo) {
        return Result.success(service.batchParse(batchNo));
    }

    /** 1.4#2 批量解析聚合进度(总数/已完成/失败/进行中/待解析 + 明细)。 */
    @GetMapping("/batch-progress")
    public Result<AitMaterialService.BatchProgress> batchProgress(@RequestParam String batchNo) {
        return Result.success(service.batchProgress(batchNo));
    }
}
