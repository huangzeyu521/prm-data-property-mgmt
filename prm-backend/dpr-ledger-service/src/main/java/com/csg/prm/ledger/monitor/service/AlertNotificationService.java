package com.csg.prm.ledger.monitor.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.monitor.dto.AlertNotificationQuery;
import com.csg.prm.ledger.monitor.entity.AlertNotification;
import com.csg.prm.ledger.monitor.entity.AlertRecord;

/**
 * 风险预警通知/推送服务:预警生成后按命中规则的通知对象+通知方式定向推送,记录已读状态。
 */
public interface AlertNotificationService {

    /** 为一条预警按规则(notifyTarget/notifyChannel)生成定向通知并推送(站内+多渠道桩)。 */
    void pushForAlert(AlertRecord alert, String ruleId);

    /** 对某条预警重新推送(可重推),返回新生成的通知数。 */
    int repush(String alertId);

    /** 分页查询通知(按接收方/已读状态)。 */
    PageResult<AlertNotification> page(AlertNotificationQuery query);

    /** 未读数(recipient 为空则统计全部,支撑铃铛红点)。 */
    long unreadCount(String recipient);

    /** 标记单条已读。 */
    void markRead(String notifyId);

    /** 标记某接收方全部已读(recipient 为空则全部),返回标记数。 */
    int markAllRead(String recipient);
}
