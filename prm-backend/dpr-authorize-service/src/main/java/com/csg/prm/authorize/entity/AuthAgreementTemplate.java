package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 授权协议模板库(可研 3.2.2.1.1.3.3.1)。
 * 按授权类型/使用目的分类管理协议模板,含内容、版本控制与套版文件上传下载。
 * 对应物理表 IM_AUTH_AGREEMENT_TEMPLATE。
 */
@TableName("IM_AUTH_AGREEMENT_TEMPLATE")
public class AuthAgreementTemplate extends BaseEntity {

    public static final String STATUS_ACTIVE = "生效中";
    public static final String STATUS_DISABLED = "停用";

    @TableId(value = "CEC_TEMPLATE_ID", type = IdType.ASSIGN_UUID)
    private String templateId;

    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    /** 授权类型:独占/共享/委托/运营... */
    @TableField("CEC_AUTH_TYPE")
    private String authType;

    /** 使用目的:内部分析/对外服务/联合建模... */
    @TableField("CEC_PURPOSE")
    private String purpose;

    @TableField("CEC_TEMPLATE_CONTENT")
    private String templateContent;

    @TableField("CEC_TEMPLATE_VERSION")
    private String templateVersion;

    @TableField("CEC_TEMPLATE_STATUS")
    private String templateStatus;

    /** 套版文件名(Word/PDF) */
    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 套版文件 Base64 */
    @TableField("CEC_FILE_DATA")
    private String fileData;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getTemplateStatus() { return templateStatus; }
    public void setTemplateStatus(String templateStatus) { this.templateStatus = templateStatus; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
}
