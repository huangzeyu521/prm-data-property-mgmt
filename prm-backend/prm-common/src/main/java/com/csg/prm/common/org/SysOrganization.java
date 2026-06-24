package com.csg.prm.common.org;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 组织机构主数据(只读映射,镜像平台 DAMS.SYS_ORGANIZATION 的 PRM 消费子集)。
 * 数据由平台/4A 同步而来,PRM 仅读不写;故不继承 BaseEntity、不参与逻辑删除与公共字段填充。
 * province/bureau 不是真表的独立列,由组织树(parentId)+ orgLevel 上溯派生,见 {@link OrgService#resolve}。
 */
@TableName("SYS_ORGANIZATION")
public class SysOrganization implements Serializable {

    /** 主键ID */
    @TableId("ID")
    private String id;

    /** 同步组织id(业务主键) */
    @TableField("BIZ_ORG_ID")
    private String bizOrgId;

    /** 组织名 */
    @TableField("BIZ_ORG_NAME")
    private String bizOrgName;

    /** 同步组织code(省用 GD/GX…;地市用城市行政码) */
    @TableField("BIZ_ORG_CODE")
    private String bizOrgCode;

    /** 缩写 */
    @TableField("SHORT_NAME")
    private String shortName;

    /** 父id(组织树自关联) */
    @TableField("PARENT_ID")
    private String parentId;

    /** 组织等级:网级/省级/地市 */
    @TableField("ORG_LEVEL")
    private String orgLevel;

    /** 单位层级 */
    @TableField("ORG_TYPE")
    private String orgType;

    /** 城市code(地市局行政区划码) */
    @TableField("CITY_CODE")
    private String cityCode;

    /** 排序编号 */
    @TableField("SORT_NO")
    private String sortNo;

    /** 组织编码 */
    @TableField("BASE_ORG_CODE")
    private String baseOrgCode;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBizOrgId() { return bizOrgId; }
    public void setBizOrgId(String bizOrgId) { this.bizOrgId = bizOrgId; }
    public String getBizOrgName() { return bizOrgName; }
    public void setBizOrgName(String bizOrgName) { this.bizOrgName = bizOrgName; }
    public String getBizOrgCode() { return bizOrgCode; }
    public void setBizOrgCode(String bizOrgCode) { this.bizOrgCode = bizOrgCode; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getOrgLevel() { return orgLevel; }
    public void setOrgLevel(String orgLevel) { this.orgLevel = orgLevel; }
    public String getOrgType() { return orgType; }
    public void setOrgType(String orgType) { this.orgType = orgType; }
    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }
    public String getSortNo() { return sortNo; }
    public void setSortNo(String sortNo) { this.sortNo = sortNo; }
    public String getBaseOrgCode() { return baseOrgCode; }
    public void setBaseOrgCode(String baseOrgCode) { this.baseOrgCode = baseOrgCode; }
}
