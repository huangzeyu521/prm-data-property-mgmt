package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 授权指引材料(可研 3.2.2.1.1.3.1.1)。授权政策文件/流程图/常见问答/申请步骤说明等,
 * 支持在线阅读与下载、历史版本。对应物理表 IM_AUTH_GUIDANCE。
 */
@TableName("IM_AUTH_GUIDANCE")
public class AuthGuidance extends BaseEntity {

    @TableId(value = "CEC_GUIDANCE_ID", type = IdType.ASSIGN_UUID)
    private String guidanceId;

    @TableField("CEC_TITLE")
    private String title;

    /** 材料类型:授权政策/流程图/常见问答/申请步骤/材料样例 */
    @TableField("CEC_GUIDANCE_TYPE")
    private String guidanceType;

    @TableField("CEC_VERSION")
    private String version;

    @TableField("CEC_PUBLISHER")
    private String publisher;

    @TableField("CEC_PUBLISH_DATE")
    private LocalDateTime publishDate;

    @TableField("CEC_FILE_URL")
    private String fileUrl;

    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 上传文件二进制(Base64);列表响应中置空以减负,下载时取用 */
    @TableField("CEC_FILE_DATA")
    private String fileData;

    /** 是否当前最新版本(同标题仅一条为最新) */
    @TableField("CEC_IS_LATEST")
    private Boolean isLatest;

    @TableField("CEC_CONTENT")
    private String content;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }

    public String getGuidanceId() { return guidanceId; }
    public void setGuidanceId(String guidanceId) { this.guidanceId = guidanceId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getGuidanceType() { return guidanceType; }
    public void setGuidanceType(String guidanceType) { this.guidanceType = guidanceType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }
    public LocalDateTime getPublishDate() { return publishDate; }
    public void setPublishDate(LocalDateTime publishDate) { this.publishDate = publishDate; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
