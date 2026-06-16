package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.aitool.dto.AitCleanRequest;
import com.csg.prm.confirm.aitool.entity.AitAuditBase;
import com.csg.prm.confirm.aitool.entity.AitCleanLog;
import com.csg.prm.confirm.aitool.entity.AitTplCompare;
import com.csg.prm.confirm.aitool.service.AitCleanService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能确权辅助工具-数据清洗与标准化接口(可研 1.2 / M1.2)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/clean")
public class AitCleanController {

    private final AitCleanService service;

    public AitCleanController(AitCleanService service) {
        this.service = service;
    }

    /** 运行清洗与标准化:rows 为空则从材料解析结果自动派生。返回审核底表/日志/待补正清单/统计。 */
    @PostMapping("/{materialId}")
    public R<AitCleanService.CleanResult> clean(@PathVariable String materialId,
                                                @RequestBody(required = false) AitCleanRequest req) {
        return R.ok(service.clean(materialId, req));
    }

    /** #4 统一审核底表 */
    @GetMapping("/{materialId}/audit-base")
    public R<List<AitAuditBase>> auditBase(@PathVariable String materialId) {
        return R.ok(service.auditBase(materialId));
    }

    /** #5 待补正信息清单(缺失/冲突/异常/重复) */
    @GetMapping("/{materialId}/pending")
    public R<List<AitAuditBase>> pending(@PathVariable String materialId) {
        return R.ok(service.pending(materialId));
    }

    /** #6 清洗日志(原始值/规则/结果/方式) */
    @GetMapping("/{materialId}/log")
    public R<List<AitCleanLog>> log(@PathVariable String materialId) {
        return R.ok(service.cleanLog(materialId));
    }

    /** 1.1.1.1#4 上传结构化模板(支持多份),与材料抽取内容自动关联审核,生成对比日志。 */
    @PostMapping("/{materialId}/template-compare")
    public R<List<AitCleanService.TplCompareResult>> templateCompare(@PathVariable String materialId,
                                                                     @RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BizException("未选择结构化模板文件");
        }
        List<AitCleanService.TplCompareResult> out = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            try {
                out.add(service.templateCompare(materialId, fixName(f.getOriginalFilename()), f.getBytes()));
            } catch (IOException e) {
                throw new BizException("读取模板文件失败:" + e.getMessage());
            }
        }
        return R.ok(out);
    }

    /** 对比日志(多模板累积)。 */
    @GetMapping("/{materialId}/template-compare")
    public R<List<AitTplCompare>> templateCompareLog(@PathVariable String materialId) {
        return R.ok(service.templateCompareLog(materialId));
    }

    /** 对比结果下载(CSV)。 */
    @GetMapping("/{materialId}/template-compare/export")
    public ResponseEntity<byte[]> exportTemplateCompare(@PathVariable String materialId) {
        byte[] data = service.exportTemplateCompare(materialId);
        String fn = URLEncoder.encode("结构化模板对比结果-" + materialId + ".csv", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }

    /** multipart 文件名 UTF-8 还原(与材料上传一致)。 */
    private static String fixName(String name) {
        if (name == null || name.isEmpty()) {
            return "模板.xlsx";
        }
        boolean allLatin1 = name.chars().allMatch(c -> c <= 0xFF);
        if (!allLatin1) {
            return name;
        }
        String recovered = new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        return recovered.indexOf('�') >= 0 ? name : recovered;
    }
}
