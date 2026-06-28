package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.monitor.dto.AlertRecordQuery;
import com.csg.prm.ledger.monitor.dto.AlertStatsVO;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.service.AlertNotificationService;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
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
 * 风险预警接口(权益变动监测预警 IM-DAM-DPR-01-001-002-002 / 风险预警通知 -005)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/monitor/alert")
public class AlertRecordController {

    private final AlertRecordService service;
    private final AlertNotificationService notificationService;

    public AlertRecordController(AlertRecordService service, AlertNotificationService notificationService) {
        this.service = service;
        this.notificationService = notificationService;
    }

    @PostMapping("/page")
    public Result<PageResult<AlertRecord>> page(@Valid @RequestBody AlertRecordQuery query) {
        return Result.success(service.page(query));
    }

    @PostMapping("/{alertId}/dispose")
    public Result<Void> dispose(@PathVariable String alertId, @RequestParam(required = false) String feedback) {
        service.dispose(alertId, feedback);
        return Result.success();
    }

    @PostMapping("/{alertId}/close")
    public Result<Void> close(@PathVariable String alertId, @RequestParam(required = false) String feedback) {
        service.close(alertId, feedback);
        return Result.success();
    }

    /** 对预警重新推送(按命中规则的通知对象+通知方式定向推送责任人)。 */
    @PostMapping("/{alertId}/push")
    public Result<Integer> push(@PathVariable String alertId) {
        return Result.success(notificationService.repush(alertId));
    }

    @GetMapping("/stats")
    public Result<AlertStatsVO> stats() {
        return Result.success(service.stats());
    }
}
