package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 授权申请表单模板(可研 3.2.2.1.1.3.1.2)。
 * 按授权类型(独占/共享/委托)配置申请表单的自定义字段、流程与验证规则。
 * 对应物理表 IM_AUTH_APPLY_TEMPLATE。
 */
@TableName("IM_AUTH_APPLY_TEMPLATE")
public class AuthApplyTemplate extends BaseEntity {

    public static final String STATUS_ACTIVE = "生效中";
    public static final String STATUS_DISABLED = "停用";

    @TableId(value = "CEC_TEMPLATE_ID", type = IdType.ASSIGN_UUID)
    private String templateId;

    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    /** 授权类型:独占/共享/委托 */
    @TableField("CEC_AUTH_TYPE")
    private String authType;

    /** 字段配置 JSON:[{name,label,type,required,rule}] */
    @TableField("CEC_FIELDS_JSON")
    private String fieldsJson;

    /** 流程说明 */
    @TableField("CEC_FLOW_DESC")
    private String flowDesc;

    @TableField("CEC_TEMPLATE_VERSION")
    private String templateVersion;

    @TableField("CEC_TEMPLATE_STATUS")
    private String templateStatus;

    @TableField("CEC_REMARK")
    private String remark;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }
    public String getFlowDesc() { return flowDesc; }
    public void setFlowDesc(String flowDesc) { this.flowDesc = flowDesc; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getTemplateStatus() { return templateStatus; }
    public void setTemplateStatus(String templateStatus) { this.templateStatus = templateStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
