package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 统一运行日志(3.3#6):审计/操作/模型调用/规则命中/告警 统一管理。对应 IM_AIT_RUN_LOG。
 */
@TableName("IM_AIT_RUN_LOG")
public class AitRunLog extends BaseEntity {

    public static final String T_AUDIT = "审计";
    public static final String T_OP = "操作";
    public static final String T_MODEL = "模型调用";
    public static final String T_RULE = "规则命中";
    public static final String T_ALERT = "告警";

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_LOG_TYPE")
    private String logType;

    @TableField("CEC_SOURCE")
    private String source;

    @TableField("CEC_ACTION")
    private String action;

    @TableField("CEC_DETAIL")
    private String detail;

    @TableField("CEC_MODEL")
    private String model;

    @TableField("CEC_DURATION_MS")
    private Long durationMs;

    @TableField("CEC_RESULT")
    private String result;

    @TableField("CEC_TASK_ID")
    private String taskId;

    @TableField("CEC_LOG_TIME")
    private LocalDateTime logTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getLogType() { return logType; }
    public void setLogType(String logType) { this.logType = logType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public LocalDateTime getLogTime() { return logTime; }
    public void setLogTime(LocalDateTime logTime) { this.logTime = logTime; }
}
