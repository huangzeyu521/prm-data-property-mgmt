package com.csg.prm.ledger.monitor.dto;

import com.csg.prm.common.query.PageQuery;

/**
 * 预警通知查询条件:按接收方(责任人)、已读状态过滤——支撑铃铛"我的未读"。
 */
public class AlertNotificationQuery extends PageQuery {

    private String recipient;
    private String readStatus;

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        this.readStatus = readStatus;
    }
}
