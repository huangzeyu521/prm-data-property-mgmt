package com.csg.prm.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 数据资产信息(由数据资产管理平台-数据目录管理同步)。对应物理表 IM_DPM_DATA_ASSET_INFO。
 * 为确权对象、产权树、总体概览提供基础信息。
 */
@TableName("IM_DPM_DATA_ASSET_INFO")
public class DataAssetInfo extends BaseEntity {

    /** 资产ID(资产平台 GUID,外部输入) */
    @TableId(value = "CEC_ASSET_ID", type = IdType.INPUT)
    private String assetId;

    @TableField("CEC_ASSET_NAME")
    private String assetName;

    @TableField("CEC_ASSET_TYPE")
    private String assetType;

    @TableField("CEC_SOURCE_OF_ASSETS")
    private String assetSource;

    @TableField("CEC_ASSET_STATUS")
    private String assetStatus;

    @TableField("CEC_ASSET_OWNER")
    private String assetOwner;

    /** 子公司(产权树第一层) */
    @TableField("CEC_SUBSIDIARY_NAME")
    private String subsidiaryName;

    /** 所属系统(产权树第二层) */
    @TableField("CEC_SYSTEM_NAME")
    private String systemName;

    /** 模式 schema(产权树第三层) */
    @TableField("CEC_SCHEMA_NAME")
    private String schemaName;

    /** 分类分级标签 */
    @TableField("CEC_SECURITY_LEVEL")
    private String securityLevel;

    @TableField("CEC_RESP_DEPT")
    private String respDept;

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

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetSource() {
        return assetSource;
    }

    public void setAssetSource(String assetSource) {
        this.assetSource = assetSource;
    }

    public String getAssetStatus() {
        return assetStatus;
    }

    public void setAssetStatus(String assetStatus) {
        this.assetStatus = assetStatus;
    }

    public String getAssetOwner() {
        return assetOwner;
    }

    public void setAssetOwner(String assetOwner) {
        this.assetOwner = assetOwner;
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

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getRespDept() {
        return respDept;
    }

    public void setRespDept(String respDept) {
        this.respDept = respDept;
    }
}
