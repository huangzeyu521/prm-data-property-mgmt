package com.csg.prm.ledger.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.dto.DataAssetInfoQuery;
import com.csg.prm.ledger.entity.DataAssetInfo;

import java.util.List;

/**
 * 数据资产信息服务。提供目录同步入库与查询能力。
 */
public interface DataAssetInfoService {

    /** 单条登记/更新(目录同步落地) */
    String saveOrUpdateAsset(DataAssetInfo asset);

    /** 批量同步(全量/增量) */
    int syncBatch(List<DataAssetInfo> assets);

    DataAssetInfo getById(String assetId);

    PageResult<DataAssetInfo> page(DataAssetInfoQuery query);

    List<DataAssetInfo> listAll();
}
