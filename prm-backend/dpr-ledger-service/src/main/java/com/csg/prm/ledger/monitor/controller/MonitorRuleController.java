package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.ledger.monitor.dto.MonitorRuleQuery;
import com.csg.prm.ledger.monitor.entity.MonitorRule;
import com.csg.prm.ledger.monitor.service.MonitorRuleService;
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
@RequestMapping("/api/dpr/monitor/rule")
public class MonitorRuleController {

    private final MonitorRuleService service;

    public MonitorRuleController(MonitorRuleService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody MonitorRule rule) {
        return R.ok(service.create(rule));
    }

    @PutMapping
    public R<Void> update(@RequestBody MonitorRule rule) {
        service.update(rule);
        return R.ok();
    }

    @PostMapping("/{ruleId}/enable")
    public R<Void> enable(@PathVariable String ruleId) {
        service.enable(ruleId);
        return R.ok();
    }

    @PostMapping("/{ruleId}/disable")
    public R<Void> disable(@PathVariable String ruleId) {
        service.disable(ruleId);
        return R.ok();
    }

    /** 物理删除(仅草稿规则);生效中/历史规则会被拒绝,请改用停用。 */
    @DeleteMapping("/{ruleId}")
    public R<Void> delete(@PathVariable String ruleId) {
        service.delete(ruleId);
        return R.ok();
    }

    @GetMapping("/{ruleId}")
    public R<MonitorRule> detail(@PathVariable String ruleId) {
        return R.ok(service.getById(ruleId));
    }

    @PostMapping("/page")
    public R<PageResult<MonitorRule>> page(@RequestBody MonitorRuleQuery query) {
        return R.ok(service.page(query));
    }
}
