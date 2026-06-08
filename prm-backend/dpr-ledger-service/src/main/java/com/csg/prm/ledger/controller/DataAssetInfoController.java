package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.ledger.dto.DataAssetInfoQuery;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.service.DataAssetInfoService;
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
@RequestMapping("/api/dpr/ledger/asset")
public class DataAssetInfoController {

    private final DataAssetInfoService service;

    public DataAssetInfoController(DataAssetInfoService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> save(@RequestBody DataAssetInfo asset) {
        return R.ok(service.saveOrUpdateAsset(asset));
    }

    @PostMapping("/sync")
    public R<Integer> sync(@RequestBody List<DataAssetInfo> assets) {
        return R.ok(service.syncBatch(assets));
    }

    @GetMapping("/{assetId}")
    public R<DataAssetInfo> detail(@PathVariable String assetId) {
        return R.ok(service.getById(assetId));
    }

    @PostMapping("/page")
    public R<PageResult<DataAssetInfo>> page(@RequestBody DataAssetInfoQuery query) {
        return R.ok(service.page(query));
    }
}
