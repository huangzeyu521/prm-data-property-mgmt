package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 授权应交材料清单规则(可配置,单一真源):场景(批量/一事一议) × 触发条件 → 应交材料。
 * triggerType:ALWAYS 总是必交(表5) / THIRD_PARTY 涉第三方来源时必交(第三方许可凭证) / SENSITIVE 涉隐私·商密时必交(信息授权协议)。
 * 材料变更=改这张表的数据,前后端单一真源,不动代码、不重部署。
 */
@TableName("IM_AUTH_MATERIAL_RULE")
public class AuthMaterialRule extends BaseEntity {

    public static final String T_ALWAYS = "ALWAYS";
    public static final String T_THIRD_PARTY = "THIRD_PARTY";
    public static final String T_SENSITIVE = "SENSITIVE";

    @TableId(value = "CEC_RULE_ID", type = IdType.ASSIGN_UUID)
    private String ruleId;

    /** 场景:批量 / 一事一议(对齐 AuthApply.authMode) */
    @TableField("CEC_SCENE")
    private String scene;

    /** 触发类型:ALWAYS / THIRD_PARTY / SENSITIVE */
    @TableField("CEC_TRIGGER_TYPE")
    private String triggerType;

    /** 应交材料名称 */
    @TableField("CEC_MATERIAL_NAME")
    private String materialName;

    /** 必填 / 视情况 */
    @TableField("CEC_REQUIRED")
    private String required;

    /** 证据类型:表单 / 凭证 / 协议 */
    @TableField("CEC_EVIDENCE_TYPE")
    private String evidenceType;

    /** 具体内容与要求明细 */
    @TableField("CEC_DETAIL")
    private String detail;

    /** 关联资料模板(可接模板库) */
    @TableField("CEC_TEMPLATE_REF")
    private String templateRef;

    @TableField("CEC_SORT_NO")
    private Integer sortNo;

    @TableField("CEC_ENABLED")
    private Boolean enabled;

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getRequired() { return required; }
    public void setRequired(String required) { this.required = required; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String evidenceType) { this.evidenceType = evidenceType; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getTemplateRef() { return templateRef; }
    public void setTemplateRef(String templateRef) { this.templateRef = templateRef; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
