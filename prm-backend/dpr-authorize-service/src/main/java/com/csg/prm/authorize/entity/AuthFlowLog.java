package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 授权申请审批处理记录(可研 3.2.2.1.1.3.2.1):
 * 每次状态流转(提交/审批通过/驳回)留痕——from→to、节点、责任人、审核意见、时间。
 * 对应物理表 IM_AUTH_FLOW_LOG。
 */
@TableName("IM_AUTH_FLOW_LOG")
public class AuthFlowLog extends BaseEntity {

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_APPLY_NO")
    private String applyNo;

    @TableField("CEC_FROM_STATUS")
    private String fromStatus;

    @TableField("CEC_TO_STATUS")
    private String toStatus;

    @TableField("CEC_NODE_NAME")
    private String nodeName;

    /** 责任人(本节点处理人/角色) */
    @TableField("CEC_RESPONDER")
    private String responder;

    /** 审核意见/结论 */
    @TableField("CEC_OPINION")
    private String opinion;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getApplyNo() { return applyNo; }
    public void setApplyNo(String applyNo) { this.applyNo = applyNo; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getNodeName() { return nodeName; }
    public void setNodeName(String nodeName) { this.nodeName = nodeName; }
    public String getResponder() { return responder; }
    public void setResponder(String responder) { this.responder = responder; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
}
