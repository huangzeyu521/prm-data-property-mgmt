package com.csg.prm.confirm.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 系统操作日志:登录 + 用户管理类操作留痕(谁/动作/对象/详情/结果/时间/IP)。 */
@TableName("IM_SYS_OPLOG")
public class SysOpLog {

    @TableId("CEC_LOG_ID")
    private String logId;
    @TableField("CEC_USER_ID")
    private String userId;
    @TableField("CEC_USER_NAME")
    private String userName;
    @TableField("CEC_ACTION")
    private String action;
    @TableField("CEC_TARGET")
    private String target;
    @TableField("CEC_DETAIL")
    private String detail;
    @TableField("CEC_IP")
    private String ip;
    @TableField("CEC_RESULT")
    private String result;
    @TableField("CEC_CREATE_TIME")
    private LocalDateTime createTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
