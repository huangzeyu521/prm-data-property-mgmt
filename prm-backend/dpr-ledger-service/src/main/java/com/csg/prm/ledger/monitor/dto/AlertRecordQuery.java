package com.csg.prm.ledger.monitor.dto;

import com.csg.prm.common.query.PageQuery;

public class AlertRecordQuery extends PageQuery {

    private String alertLevel;
    private String disposeStatus;
    private String assetId;

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getDisposeStatus() {
        return disposeStatus;
    }

    public void setDisposeStatus(String disposeStatus) {
        this.disposeStatus = disposeStatus;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }
}
