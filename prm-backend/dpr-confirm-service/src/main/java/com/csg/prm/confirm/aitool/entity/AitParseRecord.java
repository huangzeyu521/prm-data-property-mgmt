package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 智能确权辅助工具-解析记录档(1.4#1):逐字段留档 解析时间/文档名/字段/值/置信度/操作人,可查询导出。
 * 对应 IM_AIT_PARSE_RECORD。
 */
@TableName("IM_AIT_PARSE_RECORD")
public class AitParseRecord extends BaseEntity {

    @TableId(value = "CEC_RECORD_ID", type = IdType.ASSIGN_UUID)
    private String recordId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    @TableField("CEC_FILE_NAME")
    private String fileName;

    @TableField("CEC_BATCH_NO")
    private String batchNo;

    @TableField("CEC_FIELD")
    private String field;

    @TableField("CEC_FIELD_VALUE")
    private String fieldValue;

    @TableField("CEC_CONFIDENCE")
    private Double confidence;

    @TableField("CEC_OPERATOR_ID")
    private String operatorId;

    @TableField("CEC_OPERATOR_NAME")
    private String operatorName;

    @TableField("CEC_PARSE_TIME")
    private LocalDateTime parseTime;

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getParseTime() { return parseTime; }
    public void setParseTime(LocalDateTime parseTime) { this.parseTime = parseTime; }
}
