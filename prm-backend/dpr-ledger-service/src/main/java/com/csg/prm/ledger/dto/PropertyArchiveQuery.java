package com.csg.prm.ledger.dto;

import com.csg.prm.common.query.PageRequest;

/**
 * 产权档案多维查询条件(对应"产权信息查询筛选"页面)。
 */
public class PropertyArchiveQuery extends PageRequest {

    /** 资产名称模糊匹配 */
    private String assetName;
    /** 产权类型 */
    private String rightType;
    /** 责任部门 */
    private String respDept;
    /** 确权状态 */
    private String confirmStatus;

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

    public String getRespDept() {
        return respDept;
    }

    public void setRespDept(String respDept) {
        this.respDept = respDept;
    }

    public String getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus) {
        this.confirmStatus = confirmStatus;
    }
}
