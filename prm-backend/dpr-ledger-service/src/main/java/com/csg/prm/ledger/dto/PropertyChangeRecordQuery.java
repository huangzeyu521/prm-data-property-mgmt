package com.csg.prm.ledger.dto;

import com.csg.prm.common.query.PageRequest;

/**
 * 产权变更记录查询条件(对应"产权变更记录管理"页面多维筛选)。
 */
public class PropertyChangeRecordQuery extends PageRequest {

    private String assetId;
    private String changeType;
    private String operatorId;
    private String sourceFlow;
    private String changeTimeStart;
    private String changeTimeEnd;

    public String getSourceFlow() {
        return sourceFlow;
    }

    public void setSourceFlow(String sourceFlow) {
        this.sourceFlow = sourceFlow;
    }

    public String getChangeTimeStart() {
        return changeTimeStart;
    }

    public void setChangeTimeStart(String changeTimeStart) {
        this.changeTimeStart = changeTimeStart;
    }

    public String getChangeTimeEnd() {
        return changeTimeEnd;
    }

    public void setChangeTimeEnd(String changeTimeEnd) {
        this.changeTimeEnd = changeTimeEnd;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
}
