package com.csg.prm.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 产权档案("一数一档" 产权户口本)。对应物理表 IM_PROPERTY_ARCHIVE。
 * 体现三权分置:持有权 / 加工使用权 / 产品经营权。
 */
@TableName("IM_PROPERTY_ARCHIVE")
public class PropertyArchive extends BaseEntity {

    @TableId(value = "CEC_ARCHIVE_ID", type = IdType.ASSIGN_UUID)
    private String archiveId;

    /** 关联数据资产ID(资产平台 GUID) */
    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 资产名称(冗余,便于检索/展示) */
    @TableField("CEC_ASSET_NAME")
    private String assetName;

    /** 产权类型:持有权/加工使用权/产品经营权 */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    /** 权利主体 */
    @TableField("CEC_RIGHT_SUBJECT")
    private String rightSubject;

    /** 权利客体 */
    @TableField("CEC_RIGHT_OBJECT")
    private String rightObject;

    /** 取得方式:自研/采购… */
    @TableField("CEC_ACQUIRE_MODE")
    private String acquireMode;

    /** 使用权限/范围 */
    @TableField("CEC_USE_SCOPE")
    private String useScope;

    /** 责任部门 */
    @TableField("CEC_RESP_DEPT")
    private String respDept;

    /** 有效期 */
    @TableField("CEC_VALID_DATE")
    private LocalDateTime validDate;

    /** 确权状态:未确权/申请中/已确权/失败 */
    @TableField("CEC_CONFIRM_STATUS")
    private String confirmStatus;

    /** 授权状态 */
    @TableField("CEC_AUTH_STATUS")
    private String authStatus;

    /** 关联权益卡片ID(F-02) */
    @TableField("CEC_EQUITY_CARD_ID")
    private String equityCardId;

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
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

    public String getRightSubject() {
        return rightSubject;
    }

    public void setRightSubject(String rightSubject) {
        this.rightSubject = rightSubject;
    }

    public String getRightObject() {
        return rightObject;
    }

    public void setRightObject(String rightObject) {
        this.rightObject = rightObject;
    }

    public String getAcquireMode() {
        return acquireMode;
    }

    public void setAcquireMode(String acquireMode) {
        this.acquireMode = acquireMode;
    }

    public String getUseScope() {
        return useScope;
    }

    public void setUseScope(String useScope) {
        this.useScope = useScope;
    }

    public String getRespDept() {
        return respDept;
    }

    public void setRespDept(String respDept) {
        this.respDept = respDept;
    }

    public LocalDateTime getValidDate() {
        return validDate;
    }

    public void setValidDate(LocalDateTime validDate) {
        this.validDate = validDate;
    }

    public String getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus) {
        this.confirmStatus = confirmStatus;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public String getEquityCardId() {
        return equityCardId;
    }

    public void setEquityCardId(String equityCardId) {
        this.equityCardId = equityCardId;
    }
}
