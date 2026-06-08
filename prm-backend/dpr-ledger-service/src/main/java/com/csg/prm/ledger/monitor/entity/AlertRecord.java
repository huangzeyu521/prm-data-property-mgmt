package com.csg.prm.ledger.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 风险预警记录(对应界面 IM-DAM-DPR-01-001-002-005)。
 * 预警记录禁物理删除,仅可标记处置状态。对应物理表 IM_ALERT_RECORD。
 */
@TableName("IM_ALERT_RECORD")
public class AlertRecord extends BaseEntity {

    public static final String STATUS_PENDING = "待处理";
    public static final String STATUS_PROCESSING = "处理中";
    public static final String STATUS_CLOSED = "已关闭";

    public static final String LEVEL_URGENT = "紧急";
    public static final String LEVEL_IMPORTANT = "重要";
    public static final String LEVEL_NORMAL = "普通";

    @TableId(value = "CEC_ALERT_ID", type = IdType.ASSIGN_UUID)
    private String alertId;

    @TableField("CEC_RULE_ID")
    private String ruleId;

    /** 来源:状态监控/合规检查 */
    @TableField("CEC_SOURCE")
    private String source;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 预警级别:紧急/重要/普通(红/黄/绿) */
    @TableField("CEC_ALERT_LEVEL")
    private String alertLevel;

    @TableField("CEC_TRIGGER_COND")
    private String triggerCond;

    @TableField("CEC_ABNORMAL_DESC")
    private String abnormalDesc;

    @TableField("CEC_ALERT_TIME")
    private LocalDateTime alertTime;

    /** 处置状态:待处理/处理中/已关闭 */
    @TableField("CEC_DISPOSE_STATUS")
    private String disposeStatus;

    @TableField("CEC_DISPOSE_FEEDBACK")
    private String disposeFeedback;

    @TableField("CEC_RESPONDER_ID")
    private String responderId;

    @TableField("CEC_CLOSE_TIME")
    private LocalDateTime closeTime;

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getTriggerCond() {
        return triggerCond;
    }

    public void setTriggerCond(String triggerCond) {
        this.triggerCond = triggerCond;
    }

    public String getAbnormalDesc() {
        return abnormalDesc;
    }

    public void setAbnormalDesc(String abnormalDesc) {
        this.abnormalDesc = abnormalDesc;
    }

    public LocalDateTime getAlertTime() {
        return alertTime;
    }

    public void setAlertTime(LocalDateTime alertTime) {
        this.alertTime = alertTime;
    }

    public String getDisposeStatus() {
        return disposeStatus;
    }

    public void setDisposeStatus(String disposeStatus) {
        this.disposeStatus = disposeStatus;
    }

    public String getDisposeFeedback() {
        return disposeFeedback;
    }

    public void setDisposeFeedback(String disposeFeedback) {
        this.disposeFeedback = disposeFeedback;
    }

    public String getResponderId() {
        return responderId;
    }

    public void setResponderId(String responderId) {
        this.responderId = responderId;
    }

    public LocalDateTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalDateTime closeTime) {
        this.closeTime = closeTime;
    }
}
