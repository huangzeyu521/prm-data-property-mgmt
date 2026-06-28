package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.dto.PropertyChangeRecordQuery;
import com.csg.prm.ledger.entity.PropertyChangeRecord;
import com.csg.prm.ledger.service.PropertyChangeRecordService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产权变更记录接口(对应界面 IM-DAM-DPR-01-001-001-006 产权变更记录管理)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/ledger/change-record")
public class PropertyChangeRecordController {

    private final PropertyChangeRecordService service;

    public PropertyChangeRecordController(PropertyChangeRecordService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<PropertyChangeRecord>> page(@Valid @RequestBody PropertyChangeRecordQuery query) {
        return Result.success(service.page(query));
    }
}
