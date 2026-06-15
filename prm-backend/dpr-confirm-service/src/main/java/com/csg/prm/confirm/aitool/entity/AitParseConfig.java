package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-解析元数据配置(1.4#4):管理员按业务场景配置 字段映射规则 / 提取逻辑 / 置信度阈值。
 * 对应 IM_AIT_PARSE_CONFIG。
 */
@TableName("IM_AIT_PARSE_CONFIG")
public class AitParseConfig extends BaseEntity {

    public static final String DEFAULT_SCENE = "default";

    @TableId(value = "CEC_CONFIG_ID", type = IdType.ASSIGN_UUID)
    private String configId;

    @TableField("CEC_SCENE")
    private String scene;

    /** 字段映射规则 JSON,形如 {"原始字段名":"模板字段键"} */
    @TableField("CEC_FIELD_MAPPING_JSON")
    private String fieldMappingJson;

    /** 提取逻辑配置 JSON(开关/参数) */
    @TableField("CEC_EXTRACT_LOGIC_JSON")
    private String extractLogicJson;

    @TableField("CEC_CONFIDENCE_THRESHOLD")
    private Double confidenceThreshold;

    @TableField("CEC_ENABLED")
    private Integer enabled;

    @TableField("CEC_REMARK")
    private String remark;

    public String getConfigId() { return configId; }
    public void setConfigId(String configId) { this.configId = configId; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getFieldMappingJson() { return fieldMappingJson; }
    public void setFieldMappingJson(String fieldMappingJson) { this.fieldMappingJson = fieldMappingJson; }
    public String getExtractLogicJson() { return extractLogicJson; }
    public void setExtractLogicJson(String extractLogicJson) { this.extractLogicJson = extractLogicJson; }
    public Double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(Double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
