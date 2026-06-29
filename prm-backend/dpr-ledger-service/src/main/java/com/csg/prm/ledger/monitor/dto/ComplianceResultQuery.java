package com.csg.prm.ledger.monitor.dto;

import com.csg.prm.common.query.PageRequest;

public class ComplianceResultQuery extends PageRequest {

    private String checkResult;
    private String assetId;

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
}
