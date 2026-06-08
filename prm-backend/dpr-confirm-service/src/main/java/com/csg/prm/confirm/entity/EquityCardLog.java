package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 权益卡片变更历史(生成/冻结/解冻/注销/变更全程留痕,权益可追溯 3.2.6)。
 * 对应物理表 IM_EQUITY_CARD_LOG。
 */
@TableName("IM_EQUITY_CARD_LOG")
public class EquityCardLog extends BaseEntity {

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_CARD_ID")
    private String cardId;

    /** 动作:生成/冻结/解冻/注销/变更 */
    @TableField("CEC_ACTION")
    private String action;

    @TableField("CEC_FROM_STATUS")
    private String fromStatus;

    @TableField("CEC_TO_STATUS")
    private String toStatus;

    @TableField("CEC_REASON")
    private String reason;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
