package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.dto.DataAssetInfoQuery;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.service.DataAssetInfoService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据资产信息接口(对应"数据资产确权目录管理"底座 + 目录同步)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/ledger/asset")
public class DataAssetInfoController {

    private final DataAssetInfoService service;

    public DataAssetInfoController(DataAssetInfoService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> save(@Valid @RequestBody DataAssetInfo asset) {
        return Result.success(service.saveOrUpdateAsset(asset));
    }

    @PostMapping("/sync")
    public Result<Integer> sync(@RequestBody List<DataAssetInfo> assets) {
        return Result.success(service.syncBatch(assets));
    }

    @GetMapping("/{assetId}")
    public Result<DataAssetInfo> detail(@PathVariable String assetId) {
        return Result.success(service.getById(assetId));
    }

    @PostMapping("/page")
    public Result<PageResult<DataAssetInfo>> page(@Valid @RequestBody DataAssetInfoQuery query) {
        return Result.success(service.page(query));
    }
}
