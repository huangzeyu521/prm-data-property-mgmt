package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.ledger.monitor.dto.AlertNotificationQuery;
import com.csg.prm.ledger.monitor.entity.AlertNotification;
import com.csg.prm.ledger.monitor.service.AlertNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风险预警通知接口:铃铛"我的未读" + 已读标记(可研 3.2.2.1.1.1.2.4 推送/通知)。
 */
@RestController
@RequestMapping("/api/dpr/monitor/notification")
public class AlertNotificationController {

    private final AlertNotificationService service;

    public AlertNotificationController(AlertNotificationService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public R<PageResult<AlertNotification>> page(@RequestBody AlertNotificationQuery query) {
        return R.ok(service.page(query));
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount(@RequestParam(required = false) String recipient) {
        return R.ok(service.unreadCount(recipient));
    }

    @PostMapping("/{notifyId}/read")
    public R<Void> read(@PathVariable String notifyId) {
        service.markRead(notifyId);
        return R.ok();
    }

    @PostMapping("/read-all")
    public R<Integer> readAll(@RequestParam(required = false) String recipient) {
        return R.ok(service.markAllRead(recipient));
    }
}
