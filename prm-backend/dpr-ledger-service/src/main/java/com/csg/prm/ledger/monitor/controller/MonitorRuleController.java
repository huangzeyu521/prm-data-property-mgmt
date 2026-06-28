package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.monitor.dto.MonitorRuleQuery;
import com.csg.prm.ledger.monitor.entity.MonitorRule;
import com.csg.prm.ledger.monitor.service.MonitorRuleService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监测规则配置接口(IM-DAM-DPR-01-001-002-004)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/monitor/rule")
public class MonitorRuleController {

    private final MonitorRuleService service;

    public MonitorRuleController(MonitorRuleService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody MonitorRule rule) {
        return Result.success(service.create(rule));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody MonitorRule rule) {
        service.update(rule);
        return Result.success();
    }

    @PostMapping("/{ruleId}/enable")
    public Result<Void> enable(@PathVariable String ruleId) {
        service.enable(ruleId);
        return Result.success();
    }

    @PostMapping("/{ruleId}/disable")
    public Result<Void> disable(@PathVariable String ruleId) {
        service.disable(ruleId);
        return Result.success();
    }

    /** 物理删除(仅草稿规则);生效中/历史规则会被拒绝,请改用停用。 */
    @DeleteMapping("/{ruleId}")
    public Result<Void> delete(@PathVariable String ruleId) {
        service.delete(ruleId);
        return Result.success();
    }

    @GetMapping("/{ruleId}")
    public Result<MonitorRule> detail(@PathVariable String ruleId) {
        return Result.success(service.getById(ruleId));
    }

    @PostMapping("/page")
    public Result<PageResult<MonitorRule>> page(@Valid @RequestBody MonitorRuleQuery query) {
        return Result.success(service.page(query));
    }
}
