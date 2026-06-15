package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.confirm.aitool.entity.AitConflictRule;
import com.csg.prm.confirm.aitool.service.AitConflictRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权属冲突识别规则配置接口(#1,管理员):启停/优先级/阈值。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/conflict-rule")
@RequiresRole({"admin"})
public class AitConflictRuleController {

    private final AitConflictRuleService service;

    public AitConflictRuleController(AitConflictRuleService service) {
        this.service = service;
    }

    @GetMapping
    public R<List<AitConflictRule>> list() {
        return R.ok(service.list());
    }

    @PostMapping
    public R<String> save(@RequestBody AitConflictRule rule) {
        return R.ok(service.save(rule));
    }

    @PostMapping("/{ruleId}/toggle")
    public R<Void> toggle(@PathVariable String ruleId, @RequestParam boolean on) {
        service.toggle(ruleId, on);
        return R.ok();
    }
}
