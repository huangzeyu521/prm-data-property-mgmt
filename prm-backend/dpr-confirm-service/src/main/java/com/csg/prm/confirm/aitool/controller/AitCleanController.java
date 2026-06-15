package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.confirm.aitool.dto.AitCleanRequest;
import com.csg.prm.confirm.aitool.entity.AitAuditBase;
import com.csg.prm.confirm.aitool.entity.AitCleanLog;
import com.csg.prm.confirm.aitool.service.AitCleanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
