package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-清洗日志(#6):逐字段记录 原始值 / 命中规则 / 清洗后值 / 清洗方式,支持转换追溯。
 * 对应 IM_AIT_CLEAN_LOG。
 */
@TableName("IM_AIT_CLEAN_LOG")
public class AitCleanLog extends BaseEntity {

    public static final String METHOD_RULE = "规则";
    public static final String METHOD_MODEL = "模型";
    public static final String METHOD_HYBRID = "规则+模型";

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    @TableField("CEC_BATCH_NO")
    private String batchNo;

    @TableField("CEC_ROW_NO")
    private Integer rowNo;

    @TableField("CEC_FIELD")
    private String field;

    @TableField("CEC_RAW_KEY")
    private String rawKey;

    @TableField("CEC_ORIGINAL_VALUE")
    private String originalValue;

    @TableField("CEC_RULE")
    private String rule;

    @TableField("CEC_CLEANED_VALUE")
    private String cleanedValue;

    @TableField("CEC_METHOD")
    private String method;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getRawKey() { return rawKey; }
    public void setRawKey(String rawKey) { this.rawKey = rawKey; }
    public String getOriginalValue() { return originalValue; }
    public void setOriginalValue(String originalValue) { this.originalValue = originalValue; }
    public String getRule() { return rule; }
    public void setRule(String rule) { this.rule = rule; }
    public String getCleanedValue() { return cleanedValue; }
    public void setCleanedValue(String cleanedValue) { this.cleanedValue = cleanedValue; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}
