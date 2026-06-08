package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 授权域通用目录项。按 category 复用于:授权指引(GUIDANCE)、应用场景(SCENARIO)、
 * 申请表单模板(FORM_TEMPLATE)、协议模板库(AGREEMENT_TEMPLATE)。对应物理表 IM_AUTH_CATALOG_ITEM。
 */
@TableName("IM_AUTH_CATALOG_ITEM")
public class AuthCatalogItem extends BaseEntity {

    public static final String CAT_GUIDANCE = "GUIDANCE";
    public static final String CAT_SCENARIO = "SCENARIO";
    public static final String CAT_FORM_TEMPLATE = "FORM_TEMPLATE";
    public static final String CAT_AGREEMENT_TEMPLATE = "AGREEMENT_TEMPLATE";

    public static final String STATUS_ACTIVE = "生效中";
    public static final String STATUS_DISABLED = "停用";

    @TableId(value = "CEC_ITEM_ID", type = IdType.ASSIGN_UUID)
    private String itemId;

    /** 目录类别 */
    @TableField("CEC_CATEGORY")
    private String category;

    @TableField("CEC_NAME")
    private String name;

    /** 子类型/适用场景 */
    @TableField("CEC_ITEM_TYPE")
    private String itemType;

    @TableField("CEC_VERSION")
    private String version;

    @TableField("CEC_CONTENT")
    private String content;

    @TableField("CEC_STATUS")
    private String status;

    @TableField("CEC_PUBLISHER")
    private String publisher;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
}
