package com.csg.prm.confirm.dto;

import com.csg.prm.common.query.PageQuery;

public class ConfirmApplyQuery extends PageQuery {

    private String assetName;
    private String rightType;
    private String status;
    private String rightHolder;
    private String createTimeStart;
    private String createTimeEnd;
    private String registerType;   // 登记类型:初始确权 / 确权变更
    private String changeTrigger;  // 变更触发(确权变更):like 命中多触发拼接串(如"数据来源变更、管理要求变更")

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = registerType;
    }

    public String getChangeTrigger() {
        return changeTrigger;
    }

    public void setChangeTrigger(String changeTrigger) {
        this.changeTrigger = changeTrigger;
    }

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
