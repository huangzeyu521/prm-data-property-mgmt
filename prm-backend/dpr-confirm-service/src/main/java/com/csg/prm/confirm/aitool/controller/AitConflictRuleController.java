package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.Result;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.confirm.aitool.entity.AitConflictRule;
import com.csg.prm.confirm.aitool.service.AitConflictRuleService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/dpr/confirm/aitool/conflict-rule")
@RequiresRole({"admin"})
public class AitConflictRuleController {

    private final AitConflictRuleService service;

    public AitConflictRuleController(AitConflictRuleService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<AitConflictRule>> list() {
        return Result.success(service.list());
    }

    @PostMapping
    public Result<String> save(@Valid @RequestBody AitConflictRule rule) {
        return Result.success(service.save(rule));
    }

    @PostMapping("/{ruleId}/toggle")
    public Result<Void> toggle(@PathVariable String ruleId, @RequestParam boolean on) {
        service.toggle(ruleId, on);
        return Result.success();
    }
}
