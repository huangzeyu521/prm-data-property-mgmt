package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 权属冲突识别规则配置(管理员可配:启停/优先级/阈值)。对应 IM_AIT_CONFLICT_RULE。
 * ruleType 与 AitConflict.TYPE_* 对齐:主体冲突/范围冲突/时效冲突/历史记录冲突/类型冲突。
 */
@TableName("IM_AIT_CONFLICT_RULE")
public class AitConflictRule extends BaseEntity {

    @TableId(value = "CEC_RULE_ID", type = IdType.ASSIGN_UUID)
    private String ruleId;

    @TableField("CEC_RULE_TYPE")
    private String ruleType;

    @TableField("CEC_RULE_NAME")
    private String ruleName;

    @TableField("CEC_ENABLED")
    private Integer enabled;

    @TableField("CEC_PRIORITY")
    private Integer priority;

    @TableField("CEC_THRESHOLD")
    private Double threshold;

    @TableField("CEC_REMARK")
    private String remark;

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Double getThreshold() { return threshold; }
    public void setThreshold(Double threshold) { this.threshold = threshold; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
