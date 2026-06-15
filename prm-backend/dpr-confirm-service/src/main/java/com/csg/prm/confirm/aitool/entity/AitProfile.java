package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-结构化确权画像(#6):表级/附件级确权特征视图,
 * 汇聚 数据来源方式 + 数据特征 + 关键主体 + 约束信息,供分类分级/法律校验/授权判断输入。对应 IM_AIT_PROFILE。
 */
@TableName("IM_AIT_PROFILE")
public class AitProfile extends BaseEntity {

    public static final String LEVEL_TABLE = "表级";
    public static final String LEVEL_ATTACHMENT = "附件级";

    @TableId(value = "CEC_PROFILE_ID", type = IdType.ASSIGN_UUID)
    private String profileId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    @TableField("CEC_DATA_TABLE_REF")
    private String dataTableRef;

    @TableField("CEC_LEVEL")
    private String level;

    @TableField("CEC_SOURCE_METHOD")
    private String sourceMethod;

    @TableField("CEC_SOURCE_METHOD_BY")
    private String sourceMethodBy;

    @TableField("CEC_DATA_FEATURES")
    private String dataFeatures;

    @TableField("CEC_ELEMENTS_JSON")
    private String elementsJson;

    @TableField("CEC_CONFIDENCE")
    private Double confidence;

    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getDataTableRef() { return dataTableRef; }
    public void setDataTableRef(String dataTableRef) { this.dataTableRef = dataTableRef; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSourceMethod() { return sourceMethod; }
    public void setSourceMethod(String sourceMethod) { this.sourceMethod = sourceMethod; }
    public String getSourceMethodBy() { return sourceMethodBy; }
    public void setSourceMethodBy(String sourceMethodBy) { this.sourceMethodBy = sourceMethodBy; }
    public String getDataFeatures() { return dataFeatures; }
    public void setDataFeatures(String dataFeatures) { this.dataFeatures = dataFeatures; }
    public String getElementsJson() { return elementsJson; }
    public void setElementsJson(String elementsJson) { this.elementsJson = elementsJson; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
