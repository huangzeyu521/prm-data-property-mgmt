package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.aitool.entity.AitParseRecord;
import com.csg.prm.confirm.aitool.service.AitParseRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;

/**
 * 智能确权辅助工具-解析记录档接口(1.4#1):查询 + CSV 导出。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/aitool/record")
public class AitParseRecordController {

    private final AitParseRecordService service;

    public AitParseRecordController(AitParseRecordService service) {
        this.service = service;
    }

    @GetMapping("/page")
    public Result<PageResult<AitParseRecord>> page(@Valid PageRequest query,
                                              @RequestParam(required = false) String fileName,
                                              @RequestParam(required = false) String field,
                                              @RequestParam(required = false) String operator) {
        return Result.success(service.page(query, fileName, field, operator));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String fileName,
                                         @RequestParam(required = false) String field,
                                         @RequestParam(required = false) String operator) {
        byte[] data = service.exportCsv(fileName, field, operator);
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.attachment()
                        .filename("parse-records.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(data);
    }
}
