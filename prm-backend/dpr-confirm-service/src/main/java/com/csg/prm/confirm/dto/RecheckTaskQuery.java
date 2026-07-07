package com.csg.prm.confirm.dto;

import com.csg.prm.common.query.PageRequest;

/** 变更联动工单分页查询(状态/类型/来源/资产关键字)。 */
public class RecheckTaskQuery extends PageRequest {

    private String status;
    private String taskType;
    private String source;
    private String assetName;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }
}
