package com.csg.prm.ledger.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 风险预警定向通知(对应可研 3.2.2.1.1.1.2.4 "推送/系统消息通知责任人员")。
 * 每条预警按命中规则的通知对象+通知方式生成定向通知,记录已读状态,支持铃铛读"我的未读"。
 * 物理表 IM_ALERT_NOTIFICATION。
 */
@TableName("IM_ALERT_NOTIFICATION")
public class AlertNotification extends BaseEntity {

    public static final String READ_UNREAD = "未读";
    public static final String READ_READ = "已读";

    @TableId(value = "CEC_NOTIFY_ID", type = IdType.ASSIGN_UUID)
    private String notifyId;

    @TableField("CEC_ALERT_ID")
    private String alertId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 接收方(责任人/责任部门,取自命中规则的通知对象) */
    @TableField("CEC_RECIPIENT")
    private String recipient;

    /** 推送渠道(站内信/邮件/短信/eLink,逗号分隔,取自命中规则的通知方式) */
    @TableField("CEC_CHANNEL")
    private String channel;

    @TableField("CEC_TITLE")
    private String title;

    @TableField("CEC_CONTENT")
    private String content;

    @TableField("CEC_ALERT_LEVEL")
    private String alertLevel;

    /** 已读状态:未读/已读 */
    @TableField("CEC_READ_STATUS")
    private String readStatus;

    @TableField("CEC_PUSH_TIME")
    private LocalDateTime pushTime;

    @TableField("CEC_READ_TIME")
    private LocalDateTime readTime;

    public String getNotifyId() {
        return notifyId;
    }

    public void setNotifyId(String notifyId) {
        this.notifyId = notifyId;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        this.readStatus = readStatus;
    }

    public LocalDateTime getPushTime() {
        return pushTime;
    }

    public void setPushTime(LocalDateTime pushTime) {
        this.pushTime = pushTime;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }

    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }
}
