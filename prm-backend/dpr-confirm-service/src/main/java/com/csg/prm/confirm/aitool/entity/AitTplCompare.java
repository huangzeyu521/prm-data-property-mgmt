package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-结构化模板对比日志(1.1.1.1#4):用户上传的结构化模板字段与材料抽取内容自动关联审核后的对比结果。
 * 支持多模板(按 batchNo+templateName 区分),供对比日志展示与对比结果下载。对应 IM_AIT_TPL_COMPARE。
 */
@TableName("IM_AIT_TPL_COMPARE")
public class AitTplCompare extends BaseEntity {

    /** 一致性判定 */
    public static final String C_MATCH = "一致";
    public static final String C_MISMATCH = "不一致";
    public static final String C_MISSING = "缺失";

    @TableId(value = "CEC_TPL_CMP_ID", type = IdType.ASSIGN_UUID)
    private String tplCmpId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    /** 每次上传一个批次(可多模板累积) */
    @TableField("CEC_BATCH_NO")
    private String batchNo;

    /** 模板文件名 */
    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    @TableField("CEC_ROW_NO")
    private Integer rowNo;

    /** 模板字段名 */
    @TableField("CEC_TPL_FIELD")
    private String tplField;

    /** 模板字段值 */
    @TableField("CEC_TPL_VALUE")
    private String tplValue;

    /** 材料抽取的对应值 */
    @TableField("CEC_MATERIAL_VALUE")
    private String materialValue;

    /** 一致性:一致/不一致/缺失 */
    @TableField("CEC_CONSISTENCY")
    private String consistency;

    /** 该值在材料中的所在位置(定位片段) */
    @TableField("CEC_SOURCE_LOCATION")
    private String sourceLocation;

    public String getTplCmpId() { return tplCmpId; }
    public void setTplCmpId(String tplCmpId) { this.tplCmpId = tplCmpId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Integer getRowNo() { return rowNo; }
    public void setRowNo(Integer rowNo) { this.rowNo = rowNo; }
    public String getTplField() { return tplField; }
    public void setTplField(String tplField) { this.tplField = tplField; }
    public String getTplValue() { return tplValue; }
    public void setTplValue(String tplValue) { this.tplValue = tplValue; }
    public String getMaterialValue() { return materialValue; }
    public void setMaterialValue(String materialValue) { this.materialValue = materialValue; }
    public String getConsistency() { return consistency; }
    public void setConsistency(String consistency) { this.consistency = consistency; }
    public String getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(String sourceLocation) { this.sourceLocation = sourceLocation; }
}
