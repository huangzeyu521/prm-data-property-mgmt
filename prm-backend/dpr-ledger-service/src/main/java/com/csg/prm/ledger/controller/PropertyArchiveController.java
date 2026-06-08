package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.ledger.dto.PropertyArchiveQuery;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.service.PropertyArchiveService;
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
@RequestMapping("/api/dpr/ledger/archive")
public class PropertyArchiveController {

    private final PropertyArchiveService service;

    public PropertyArchiveController(PropertyArchiveService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody PropertyArchive archive) {
        return R.ok(service.create(archive));
    }

    /** 产权事件回写(确权制卡/授权生效 → 实时更新台账确权/授权状态 + 变更留痕,P0-① 流程贯通) */
    @PostMapping("/writeback")
    public R<Void> writeback(@RequestBody com.csg.prm.common.writeback.RightsEvent event) {
        service.applyRightsEvent(event);
        return R.ok();
    }

    @PutMapping
    public R<Void> update(@RequestBody PropertyArchive archive) {
        service.update(archive);
        return R.ok();
    }

    @DeleteMapping("/{archiveId}")
    public R<Void> delete(@PathVariable String archiveId) {
        service.delete(archiveId);
        return R.ok();
    }

    @GetMapping("/{archiveId}")
    public R<PropertyArchive> detail(@PathVariable String archiveId) {
        return R.ok(service.getById(archiveId));
    }

    @PostMapping("/page")
    public R<PageResult<PropertyArchive>> page(@RequestBody PropertyArchiveQuery query) {
        return R.ok(service.page(query));
    }
}
