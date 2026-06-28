package com.csg.prm.confirm.dto;

import com.csg.prm.common.query.PageQuery;

/**
 * 权益卡片查询条件(权益卡片生成管理,库表级)。
 * sysName=系统名(like on assetId「SYS:系统名」);tableName=库表名(like on assetName,卡片下沉库表级后 assetName=库表名);
 * cardStatus=状态(eq);rightType=权属类型(like,短名命中全名)。
 */
public class EquityCardQuery extends PageQuery {

    private String sysName;
    private String tableName;
    private String cardStatus;
    private String rightType;

    public String getSysName() {
        return sysName;
    }

    public void setSysName(String sysName) {
        this.sysName = sysName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(String cardStatus) {
        this.cardStatus = cardStatus;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }
}
