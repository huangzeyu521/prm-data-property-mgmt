package com.csg.prm.ledger.dto;

import com.csg.prm.common.query.PageQuery;

/**
 * 数据资产信息查询条件。
 */
public class DataAssetInfoQuery extends PageQuery {

    private String assetName;
    private String subsidiaryName;
    private String systemName;
    private String assetType;

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getSubsidiaryName() {
        return subsidiaryName;
    }

    public void setSubsidiaryName(String subsidiaryName) {
        this.subsidiaryName = subsidiaryName;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }
}
