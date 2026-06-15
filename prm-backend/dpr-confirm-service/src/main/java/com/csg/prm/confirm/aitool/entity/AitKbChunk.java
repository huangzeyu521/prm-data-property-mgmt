package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 产权知识库-切片(2.1#2/#3):分段正文 + 条款编号 + 标签 + 向量 + 条款元数据(来源/生效日期/适用范围)。
 * 对应 IM_AIT_KB_CHUNK。
 */
@TableName("IM_AIT_KB_CHUNK")
public class AitKbChunk extends BaseEntity {

    public static final String STATUS_VALID = "有效";
    public static final String STATUS_INVALID = "失效";

    @TableId(value = "CEC_CHUNK_ID", type = IdType.ASSIGN_UUID)
    private String chunkId;

    @TableField("CEC_DOC_ID")
    private String docId;

    @TableField("CEC_DOC_TYPE")
    private String docType;

    @TableField("CEC_DOMAIN")
    private String domain;

    @TableField("CEC_TITLE")
    private String title;

    @TableField("CEC_CLAUSE_NO")
    private String clauseNo;

    @TableField("CEC_TAGS")
    private String tags;

    @TableField("CEC_CONTENT")
    private String content;

    @TableField("CEC_VECTOR_JSON")
    private String vectorJson;

    @TableField("CEC_VECTOR_DIM")
    private Integer vectorDim;

    @TableField("CEC_EFFECTIVE_DATE")
    private String effectiveDate;

    @TableField("CEC_SCOPE")
    private String scope;

    @TableField("CEC_STATUS")
    private String status;

    @TableField("CEC_SEQ")
    private Integer seq;

    public String getChunkId() { return chunkId; }
    public void setChunkId(String chunkId) { this.chunkId = chunkId; }
    public String getDocId() { return docId; }
    public void setDocId(String docId) { this.docId = docId; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getClauseNo() { return clauseNo; }
    public void setClauseNo(String clauseNo) { this.clauseNo = clauseNo; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVectorJson() { return vectorJson; }
    public void setVectorJson(String vectorJson) { this.vectorJson = vectorJson; }
    public Integer getVectorDim() { return vectorDim; }
    public void setVectorDim(Integer vectorDim) { this.vectorDim = vectorDim; }
    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getSeq() { return seq; }
    public void setSeq(Integer seq) { this.seq = seq; }
}
