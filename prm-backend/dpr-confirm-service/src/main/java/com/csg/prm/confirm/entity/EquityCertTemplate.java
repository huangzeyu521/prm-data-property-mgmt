package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 确权权益证书模板(对应界面 IM-DAM-DPR-02-001-003 模板配置)。
 * 已用于出证的模板禁物理删,仅停用/废止 + 新版本。对应物理表 IM_EQUITY_CERT_TEMPLATE。
 */
@TableName("IM_EQUITY_CERT_TEMPLATE")
public class EquityCertTemplate extends BaseEntity {

    public static final String STATUS_ACTIVE = "生效中";
    public static final String STATUS_DISABLED = "停用";

    @TableId(value = "CEC_TEMPLATE_ID", type = IdType.ASSIGN_UUID)
    private String templateId;

    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    @TableField("CEC_TEMPLATE_VERSION")
    private String templateVersion;

    /** 适用权益类型:持有权/加工使用权/产品经营权 */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    @TableField("CEC_TEMPLATE_CONTENT")
    private String templateContent;

    @TableField("CEC_TEMPLATE_STATUS")
    private String templateStatus;

    /** 模板套版文件名(Word/PDF) */
    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 模板套版文件 Base64(容器无依赖入库) */
    @TableField("CEC_FILE_DATA")
    private String fileData;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String templateVersion) { this.templateVersion = templateVersion; }
    public String getRightType() { return rightType; }
    public void setRightType(String rightType) { this.rightType = rightType; }
    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
    public String getTemplateStatus() { return templateStatus; }
    public void setTemplateStatus(String templateStatus) { this.templateStatus = templateStatus; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
}
