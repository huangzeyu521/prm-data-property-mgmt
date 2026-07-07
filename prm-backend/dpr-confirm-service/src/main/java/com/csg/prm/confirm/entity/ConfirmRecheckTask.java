package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 变更联动工单(35号文 §二(三)2 权益变更"按季度定期重新确权"的闭环载体)。物理表 IM_CONFIRM_RECHECK_TASK。
 * <p>三类来源统一入池:季度到期扫描(ReConfirmReminderJob)/监测联动(权属变动预警派生)/变更生效联动(授权处置)。
 * 两种处置出口:① 派生确权变更草稿(applyId 回链,申请完成自动销号);② 复核确认无变化(留痕人/时间/结论)。
 * 重新确权的合法产出有两种 —— 发生变更 或 复核确认无变化,后者同样必须留痕,否则"季度重确权"无法销号闭环。
 */
@TableName("IM_CONFIRM_RECHECK_TASK")
public class ConfirmRecheckTask extends BaseEntity {

    public static final String STATUS_OPEN = "待处置";
    public static final String STATUS_CHANGING = "变更申请中";   // 已派生变更草稿,随确权变更单流转
    public static final String STATUS_NO_CHANGE = "已复核无变化"; // 复核确认权益信息未变动(留痕销号)
    public static final String STATUS_DONE = "已完成";           // 派生变更单终审生效 / 授权处置完成

    public static final String TYPE_RECHECK = "重确权";
    public static final String TYPE_AUTH_DISPOSAL = "授权处置";

    public static final String SOURCE_QUARTER_SCAN = "季度到期扫描";
    public static final String SOURCE_MONITOR = "监测联动";
    public static final String SOURCE_CHANGE_EFFECT = "变更生效联动";
    public static final String SOURCE_MANUAL = "手工发起";

    @TableId(value = "CEC_TASK_ID", type = IdType.ASSIGN_UUID)
    private String taskId;

    @TableField("CEC_TASK_NO")
    private String taskNo;

    /** 工单类型:重确权 / 授权处置 */
    @TableField("CEC_TASK_TYPE")
    private String taskType;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_ASSET_NAME")
    private String assetName;

    /** 触发来源:季度到期扫描 / 监测联动 / 变更生效联动 / 手工发起 */
    @TableField("CEC_SOURCE")
    private String source;

    /** 变更触发类型(重确权:权益到期/数据来源变更/管理要求变更/数据新增;授权处置:承袭变更单触发) */
    @TableField("CEC_TRIGGER_TYPE")
    private String triggerType;

    /** 工单事由(到期卡片编号/预警描述/受影响授权与处置建议) */
    @TableField("CEC_REASON")
    private String reason;

    /** 处置期限(到期扫描=卡片有效期;其余=生成日+30天) */
    @TableField("CEC_DUE_DATE")
    private LocalDateTime dueDate;

    @TableField("CEC_STATUS")
    private String status;

    /** 派生的确权变更申请ID(出口①回链,申请完成自动销号) */
    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 关联业务编号(到期权益卡片编号 / 受影响授权编号) */
    @TableField("CEC_REF_NO")
    private String refNo;

    @TableField("CEC_HANDLER_ID")
    private String handlerId;

    @TableField("CEC_HANDLER_NAME")
    private String handlerName;

    @TableField("CEC_HANDLE_TIME")
    private LocalDateTime handleTime;

    /** 处置结论留痕(复核无变化结论/授权处置说明) */
    @TableField("CEC_HANDLE_NOTE")
    private String handleNote;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public void setHandlerId(String handlerId) {
        this.handlerId = handlerId;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleNote() {
        return handleNote;
    }

    public void setHandleNote(String handleNote) {
        this.handleNote = handleNote;
    }
}
