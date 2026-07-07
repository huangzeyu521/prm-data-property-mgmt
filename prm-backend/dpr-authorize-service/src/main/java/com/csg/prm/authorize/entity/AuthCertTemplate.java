package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 授权权益证书模板(可研 3.2.2.1.1.3.4.2)。支持多种授权类型的证书模板管理,
 * 包括专项授权证书、批量授权证书等;授权证书出证时按模板自动填充。
 * 已用于出证的模板禁物理删,仅停用/废止 + 新版本。物理表 IM_AUTH_CERT_TEMPLATE。
 */
@TableName("IM_AUTH_CERT_TEMPLATE")
public class AuthCertTemplate extends BaseEntity {

    public static final String STATUS_ACTIVE = "生效中";
    public static final String STATUS_DISABLED = "停用";

    /** 证书类型:专项授权证书 / 批量授权证书 */
    public static final String TYPE_SPECIAL = "专项授权证书";
    public static final String TYPE_BATCH = "批量授权证书";

    @TableId(value = "CEC_TEMPLATE_ID", type = IdType.ASSIGN_UUID)
    private String templateId;

    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    @TableField("CEC_TEMPLATE_VERSION")
    private String templateVersion;

    /** 证书类型:专项授权证书 / 批量授权证书 */
    @TableField("CEC_CERT_TYPE")
    private String certType;

    /** 适用授权权益类型:使用权 / 经营权 */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    @TableField("CEC_TEMPLATE_CONTENT")
    private String templateContent;

    @TableField("CEC_TEMPLATE_STATUS")
    private String templateStatus;

    /** 套版文件名(Word/PDF) */
    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 套版文件 Base64 */
    @TableField("CEC_FILE_DATA")
    private String fileData;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getCertType() { return certType; }
    public void setCertType(String certType) { this.certType = certType; }
    public String getRightType() { return rightType; }
    public void setRightType(String rightType) { this.rightType = rightType; }
    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
    public String getTemplateStatus() { return templateStatus; }
    public void setTemplateStatus(String templateStatus) { this.templateStatus = templateStatus; }
}
