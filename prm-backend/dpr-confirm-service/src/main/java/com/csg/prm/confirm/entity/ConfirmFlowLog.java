package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 确权申请流转历史(可研 3.2.2.1.1.2.1.6 进度跟踪):
 * 每次状态流转(提交/审核通过/驳回/制卡)留痕——记录 from→to、节点、责任人、意见、时间,
 * 并同时生成"系统消息"进度通知(notifyContent/pushChannel),作为申请人可查的进度轨迹与通知。
 * 物理表 IM_CONFIRM_FLOW_LOG。
 */
@TableName("IM_CONFIRM_FLOW_LOG")
public class ConfirmFlowLog extends BaseEntity {

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

    @TableField("CEC_NODE")
    private Integer node;

    @TableField("CEC_NODE_NAME")
    private String nodeName;

    /** 责任人(本节点处理人/角色) */
    @TableField("CEC_RESPONDER")
    private String responder;

    @TableField("CEC_OPINION")
    private String opinion;

    /** 进度通知文案(系统消息) */
    @TableField("CEC_NOTIFY_CONTENT")
    private String notifyContent;

    /** 推送渠道(系统消息/邮件/短信...) */
    @TableField("CEC_PUSH_CHANNEL")
    private String pushChannel;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getApplyNo() {
        return applyNo;
    }

    public void setApplyNo(String applyNo) {
        this.applyNo = applyNo;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Integer getNode() {
        return node;
    }

    public void setNode(Integer node) {
        this.node = node;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getResponder() {
        return responder;
    }

    public void setResponder(String responder) {
        this.responder = responder;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public String getNotifyContent() {
        return notifyContent;
    }

    public void setNotifyContent(String notifyContent) {
        this.notifyContent = notifyContent;
    }

    public String getPushChannel() {
        return pushChannel;
    }

    public void setPushChannel(String pushChannel) {
        this.pushChannel = pushChannel;
    }
}
