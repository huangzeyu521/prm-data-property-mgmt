package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-资料模板库(1.4#3):确权书/授权函/权属证明等标准模板,在线编辑正文 + 附件 + 版本管理 + 下载。
 * 对应 IM_AIT_DOC_TEMPLATE。
 */
@TableName("IM_AIT_DOC_TEMPLATE")
public class AitDocTemplate extends BaseEntity {

    @TableId(value = "CEC_TEMPLATE_ID", type = IdType.ASSIGN_UUID)
    private String templateId;

    @TableField("CEC_TEMPLATE_TYPE")
    private String templateType;

    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    @TableField("CEC_VERSION")
    private String version;

    @TableField("CEC_CONTENT")
    private String content;

    @TableField("CEC_FILE_NAME")
    private String fileName;

    @TableField("CEC_FILE_DATA")
    private String fileData;

    @TableField("CEC_IS_LATEST")
    private Boolean isLatest;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateType() { return templateType; }
    public void setTemplateType(String templateType) { this.templateType = templateType; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }
}
