package com.csg.prm.ledger.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 监测规则配置(对应界面 IM-DAM-DPR-01-001-002-004)。
 * 生效中/历史规则禁物理删除,仅可停用 + 新版本。对应物理表 IM_MONITOR_RULE。
 */
@TableName("IM_MONITOR_RULE")
public class MonitorRule extends BaseEntity {

    public static final String STATUS_DRAFT = "草稿";
    public static final String STATUS_ACTIVE = "生效中";
    public static final String STATUS_DISABLED = "停用";

    @TableId(value = "CEC_RULE_ID", type = IdType.ASSIGN_UUID)
    private String ruleId;

    @TableField("CEC_RULE_NAME")
    private String ruleName;

    /** 规则分类:权属变动/调用异常/到期提醒/合规 */
    @TableField("CEC_RULE_CATEGORY")
    private String ruleCategory;

    @TableField("CEC_MONITOR_TARGET")
    private String monitorTarget;

    /** 触发条件(DRL/JSON 表达式) */
    @TableField("CEC_TRIGGER_COND")
    private String triggerCond;

    @TableField("CEC_THRESHOLD")
    private String threshold;

    @TableField("CEC_PRIORITY")
    private String priority;

    @TableField("CEC_NOTIFY_TARGET")
    private String notifyTarget;

    /** 通知方式(可多选,逗号分隔):站内信/邮件/短信/eLink */
    @TableField("CEC_NOTIFY_CHANNEL")
    private String notifyChannel;

    @TableField("CEC_RULE_VERSION")
    private String ruleVersion;

    /** 生效状态:草稿/生效中/停用 */
    @TableField("CEC_EFFECT_STATUS")
    private String effectStatus;

    /** 命中是否联动熔断授权(暂停被授权资产的生效证书) */
    @TableField("CEC_CIRCUIT_BREAK")
    private Boolean circuitBreak;

    public Boolean getCircuitBreak() {
        return circuitBreak;
    }

    public void setCircuitBreak(Boolean circuitBreak) {
        this.circuitBreak = circuitBreak;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleCategory() {
        return ruleCategory;
    }

    public void setRuleCategory(String ruleCategory) {
        this.ruleCategory = ruleCategory;
    }

    public String getMonitorTarget() {
        return monitorTarget;
    }

    public void setMonitorTarget(String monitorTarget) {
        this.monitorTarget = monitorTarget;
    }

    public String getTriggerCond() {
        return triggerCond;
    }

    public void setTriggerCond(String triggerCond) {
        this.triggerCond = triggerCond;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getNotifyTarget() {
        return notifyTarget;
    }

    public void setNotifyTarget(String notifyTarget) {
        this.notifyTarget = notifyTarget;
    }

    public String getNotifyChannel() {
        return notifyChannel;
    }

    public void setNotifyChannel(String notifyChannel) {
        this.notifyChannel = notifyChannel;
    }

    public String getRuleVersion() {
        return ruleVersion;
    }

    public void setRuleVersion(String ruleVersion) {
        this.ruleVersion = ruleVersion;
    }

    public String getEffectStatus() {
        return effectStatus;
    }

    public void setEffectStatus(String effectStatus) {
        this.effectStatus = effectStatus;
    }
}
