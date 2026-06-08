package com.csg.prm.confirm.dto;

import com.csg.prm.common.query.PageQuery;

public class ConfirmApplyQuery extends PageQuery {

    private String assetName;
    private String rightType;
    private String status;
    private String rightHolder;
    private String createTimeStart;
    private String createTimeEnd;

    public String getRightHolder() {
        return rightHolder;
    }

    public void setRightHolder(String rightHolder) {
        this.rightHolder = rightHolder;
    }

    public String getCreateTimeStart() {
        return createTimeStart;
    }

    public void setCreateTimeStart(String createTimeStart) {
        this.createTimeStart = createTimeStart;
    }

    public String getCreateTimeEnd() {
        return createTimeEnd;
    }

    public void setCreateTimeEnd(String createTimeEnd) {
        this.createTimeEnd = createTimeEnd;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
