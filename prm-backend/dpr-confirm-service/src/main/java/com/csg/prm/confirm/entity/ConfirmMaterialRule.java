package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 确权应交材料清单规则(可配置,替代硬编码 CODE_MATERIAL):
 * 场景 × 触发条件 → 应交材料。材料变更=改数据,前后端单一真源,不动代码。
 * triggerType:ALWAYS 总是必交 / TABLE2 涉第三方(B–F 或 G–J)时必交 / SOURCE 选中来源码(A–F)时交 / RELATION 选中关联码(G–J)时交。
 */
@TableName("IM_CONFIRM_MATERIAL_RULE")
public class ConfirmMaterialRule extends BaseEntity {

    public static final String T_ALWAYS = "ALWAYS";
    public static final String T_TABLE2 = "TABLE2";
    public static final String T_SOURCE = "SOURCE";
    public static final String T_RELATION = "RELATION";

    @TableId(value = "CEC_RULE_ID", type = IdType.ASSIGN_UUID)
    private String ruleId;

    /** 场景:确权 / 批量授权 / 一事一议授权 …(可扩展) */
    @TableField("CEC_SCENE")
    private String scene;

    /** 触发类型:ALWAYS / TABLE2 / SOURCE / RELATION */
    @TableField("CEC_TRIGGER_TYPE")
    private String triggerType;

    /** 触发码:A–J(SOURCE/RELATION 用);ALWAYS/TABLE2 为空 */
    @TableField("CEC_TRIGGER_CODE")
    private String triggerCode;

    /** 触发码的人读标签(如 自行生产 / 个人隐私) */
    @TableField("CEC_TRIGGER_LABEL")
    private String triggerLabel;

    /** 应交材料名称 */
    @TableField("CEC_MATERIAL_NAME")
    private String materialName;

    /** 必填 / 视情况 */
    @TableField("CEC_REQUIRED")
    private String required;

    /** 证据类型:表单 / 凭证 / 说明 */
    @TableField("CEC_EVIDENCE_TYPE")
    private String evidenceType;

    /** 具体内容与要求明细 */
    @TableField("CEC_DETAIL")
    private String detail;

    /** 关联资料模板(可接 IM_AIT_DOC_TEMPLATE) */
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
    public String getTriggerCode() { return triggerCode; }
    public void setTriggerCode(String triggerCode) { this.triggerCode = triggerCode; }
    public String getTriggerLabel() { return triggerLabel; }
    public void setTriggerLabel(String triggerLabel) { this.triggerLabel = triggerLabel; }
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
