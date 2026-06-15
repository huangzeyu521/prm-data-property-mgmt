package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-统一审核底表(#4/#5):文档抽取结果↔结构化模板映射后的标准行,
 * 标注 正常/缺失/冲突/异常/重复 与待补正建议;CEC_DATA_TABLE_REF 作附件↔主表关联索引。
 * 对应 IM_AIT_AUDIT_BASE。
 */
@TableName("IM_AIT_AUDIT_BASE")
public class AitAuditBase extends BaseEntity {

    public static final String ST_OK = "正常";
    public static final String ST_MISSING = "缺失";
    public static final String ST_CONFLICT = "冲突";
    public static final String ST_ABNORMAL = "异常";
    public static final String ST_DUPLICATE = "重复";

    @TableId(value = "CEC_AUDIT_ID", type = IdType.ASSIGN_UUID)
    private String auditId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    @TableField("CEC_BATCH_NO")
    private String batchNo;

    @TableField("CEC_ROW_NO")
    private Integer rowNo;

    @TableField("CEC_TEMPLATE_FIELD")
    private String templateField;

    @TableField("CEC_FIELD_LABEL")
    private String fieldLabel;

    @TableField("CEC_RAW_VALUE")
    private String rawValue;

    @TableField("CEC_CLEAN_VALUE")
    private String cleanValue;

    @TableField("CEC_STATUS")
    private String status;

    @TableField("CEC_ISSUE")
    private String issue;

    @TableField("CEC_SUGGESTION")
    private String suggestion;

    @TableField("CEC_DATA_TABLE_REF")
    private String dataTableRef;

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getTemplateField() { return templateField; }
    public void setTemplateField(String templateField) { this.templateField = templateField; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }
    public String getCleanValue() { return cleanValue; }
    public void setCleanValue(String cleanValue) { this.cleanValue = cleanValue; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getIssue() { return issue; }
    public void setIssue(String issue) { this.issue = issue; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getDataTableRef() { return dataTableRef; }
    public void setDataTableRef(String dataTableRef) { this.dataTableRef = dataTableRef; }
}
