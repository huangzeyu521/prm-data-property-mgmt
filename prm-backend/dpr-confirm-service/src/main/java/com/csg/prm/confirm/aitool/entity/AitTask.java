package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 批量任务(3.3#5):并发控制/断点续跑/失败重试/留痕 + 任务监控。对应 IM_AIT_TASK。
 */
@TableName("IM_AIT_TASK")
public class AitTask extends BaseEntity {

    public static final String TYPE_PARSE = "BATCH_PARSE";
    public static final String TYPE_AUDIT = "BATCH_AUDIT";
    public static final String ST_PENDING = "待运行";
    public static final String ST_RUNNING = "运行中";
    public static final String ST_DONE = "已完成";
    public static final String ST_PARTIAL = "部分失败";
    public static final String ST_PAUSED = "已暂停";

    @TableId(value = "CEC_TASK_ID", type = IdType.ASSIGN_UUID)
    private String taskId;

    @TableField("CEC_TASK_TYPE")
    private String taskType;

    @TableField("CEC_TASK_NAME")
    private String taskName;

    @TableField("CEC_ITEMS_JSON")
    private String itemsJson;

    @TableField("CEC_ITEM_STATE_JSON")
    private String itemStateJson;

    @TableField("CEC_TOTAL")
    private Integer total;

    @TableField("CEC_DONE")
    private Integer done;

    @TableField("CEC_FAILED")
    private Integer failed;

    @TableField("CEC_STATUS")
    private String status;

    @TableField("CEC_CONCURRENCY")
    private Integer concurrency;

    @TableField("CEC_RETRY_MAX")
    private Integer retryMax;

    @TableField("CEC_CURSOR")
    private Integer cursor;

    @TableField("CEC_LAST_ERROR")
    private String lastError;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }
    public String getItemStateJson() { return itemStateJson; }
    public void setItemStateJson(String itemStateJson) { this.itemStateJson = itemStateJson; }
    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }
    public Integer getDone() { return done; }
    public void setDone(Integer done) { this.done = done; }
    public Integer getFailed() { return failed; }
    public void setFailed(Integer failed) { this.failed = failed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getConcurrency() { return concurrency; }
    public void setConcurrency(Integer concurrency) { this.concurrency = concurrency; }
    public Integer getRetryMax() { return retryMax; }
    public void setRetryMax(Integer retryMax) { this.retryMax = retryMax; }
    public Integer getCursor() { return cursor; }
    public void setCursor(Integer cursor) { this.cursor = cursor; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
