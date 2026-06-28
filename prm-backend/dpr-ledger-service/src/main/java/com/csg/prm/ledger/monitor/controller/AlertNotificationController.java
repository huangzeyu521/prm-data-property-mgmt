package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.monitor.dto.AlertNotificationQuery;
import com.csg.prm.ledger.monitor.entity.AlertNotification;
import com.csg.prm.ledger.monitor.service.AlertNotificationService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/dpr/monitor/notification")
public class AlertNotificationController {

    private final AlertNotificationService service;

    public AlertNotificationController(AlertNotificationService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<AlertNotification>> page(@Valid @RequestBody AlertNotificationQuery query) {
        return Result.success(service.page(query));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount(@RequestParam(required = false) String recipient) {
        return Result.success(service.unreadCount(recipient));
    }

    @PostMapping("/{notifyId}/read")
    public Result<Void> read(@PathVariable String notifyId) {
        service.markRead(notifyId);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Integer> readAll(@RequestParam(required = false) String recipient) {
        return Result.success(service.markAllRead(recipient));
    }
}
