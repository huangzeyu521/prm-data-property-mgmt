package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.dto.PropertyTreeNode;
import com.csg.prm.ledger.service.PropertyTreeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产权树接口(对应界面 IM-DAM-DPR-01-001-001-002 数据资产产权树展示)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/ledger/tree")
public class PropertyTreeController {

    private final PropertyTreeService service;

    public PropertyTreeController(PropertyTreeService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<PropertyTreeNode>> tree() {
        return Result.success(service.buildTree());
    }
}
