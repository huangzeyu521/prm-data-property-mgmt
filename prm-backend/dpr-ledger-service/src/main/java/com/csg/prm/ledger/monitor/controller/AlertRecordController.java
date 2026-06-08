package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.ledger.monitor.dto.AlertRecordQuery;
import com.csg.prm.ledger.monitor.dto.AlertStatsVO;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.service.AlertNotificationService;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风险预警接口(权益变动监测预警 IM-DAM-DPR-01-001-002-002 / 风险预警通知 -005)。
 */
@RestController
@RequestMapping("/api/dpr/monitor/alert")
public class AlertRecordController {

    private final AlertRecordService service;
    private final AlertNotificationService notificationService;

    public AlertRecordController(AlertRecordService service, AlertNotificationService notificationService) {
        this.service = service;
        this.notificationService = notificationService;
    }

    @PostMapping("/page")
    public R<PageResult<AlertRecord>> page(@RequestBody AlertRecordQuery query) {
        return R.ok(service.page(query));
    }

    @PostMapping("/{alertId}/dispose")
    public R<Void> dispose(@PathVariable String alertId, @RequestParam(required = false) String feedback) {
        service.dispose(alertId, feedback);
        return R.ok();
    }

    @PostMapping("/{alertId}/close")
    public R<Void> close(@PathVariable String alertId, @RequestParam(required = false) String feedback) {
        service.close(alertId, feedback);
        return R.ok();
    }

    /** 对预警重新推送(按命中规则的通知对象+通知方式定向推送责任人)。 */
    @PostMapping("/{alertId}/push")
    public R<Integer> push(@PathVariable String alertId) {
        return R.ok(notificationService.repush(alertId));
    }

    @GetMapping("/stats")
    public R<AlertStatsVO> stats() {
        return R.ok(service.stats());
    }
}
