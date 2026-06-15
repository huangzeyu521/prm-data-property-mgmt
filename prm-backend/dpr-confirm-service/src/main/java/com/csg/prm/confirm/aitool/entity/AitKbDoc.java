package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 产权知识库-文档(2.1#1/#6):法规/行业标准/内部制度/审核规则/典型案例 + 知识域 + 版本治理。对应 IM_AIT_KB_DOC。
 */
@TableName("IM_AIT_KB_DOC")
public class AitKbDoc extends BaseEntity {

    public static final String STATUS_VALID = "有效";
    public static final String STATUS_INVALID = "失效";

    @TableId(value = "CEC_DOC_ID", type = IdType.ASSIGN_UUID)
    private String docId;

    @TableField("CEC_DOC_TYPE")
    private String docType;

    @TableField("CEC_DOMAIN")
    private String domain;

    @TableField("CEC_TITLE")
    private String title;

    @TableField("CEC_SOURCE")
    private String source;

    @TableField("CEC_EFFECTIVE_DATE")
    private String effectiveDate;

    @TableField("CEC_SCOPE")
    private String scope;

    @TableField("CEC_VERSION")
    private String version;

    @TableField("CEC_IS_LATEST")
    private Boolean isLatest;

    @TableField("CEC_STATUS")
    private String status;

    @TableField("CEC_CONTENT")
    private String content;

    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Boolean getIsLatest() { return isLatest; }
    public void setIsLatest(Boolean isLatest) { this.isLatest = isLatest; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
