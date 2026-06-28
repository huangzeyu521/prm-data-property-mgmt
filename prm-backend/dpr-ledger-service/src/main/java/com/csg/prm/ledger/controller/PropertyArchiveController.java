package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.dto.PropertyArchiveQuery;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.service.PropertyArchiveService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产权档案管理接口(对应界面 IM-DAM-DPR-01-001-001-003 数据集产权档案管理)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/ledger/archive")
public class PropertyArchiveController {

    private final PropertyArchiveService service;

    public PropertyArchiveController(PropertyArchiveService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody PropertyArchive archive) {
        return Result.success(service.create(archive));
    }

    /** 产权事件回写(确权制卡/授权生效 → 实时更新台账确权/授权状态 + 变更留痕,P0-① 流程贯通) */
    @PostMapping("/writeback")
    public Result<Void> writeback(@Valid @RequestBody com.csg.prm.common.writeback.RightsEvent event) {
        service.applyRightsEvent(event);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody PropertyArchive archive) {
        service.update(archive);
        return Result.success();
    }

    @DeleteMapping("/{archiveId}")
    public Result<Void> delete(@PathVariable String archiveId) {
        service.delete(archiveId);
        return Result.success();
    }

    @GetMapping("/{archiveId}")
    public Result<PropertyArchive> detail(@PathVariable String archiveId) {
        return Result.success(service.getById(archiveId));
    }

    @PostMapping("/page")
    public Result<PageResult<PropertyArchive>> page(@Valid @RequestBody PropertyArchiveQuery query) {
        return Result.success(service.page(query));
    }
}
