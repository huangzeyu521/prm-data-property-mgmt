package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthScenario;
import com.csg.prm.authorize.service.AuthScenarioService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 授权应用场景配置接口(可研 3.2.2.1.1.3.1.3):用途场景 + 申请原因模板。 */
@RestController
@RequestMapping("/api/dpr/auth/scenario")
public class AuthScenarioController {

    private final AuthScenarioService service;

    public AuthScenarioController(AuthScenarioService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody AuthScenario s) {
        return R.ok(service.create(s));
    }

    @PutMapping
    public R<Void> update(@RequestBody AuthScenario s) {
        service.update(s);
        return R.ok();
    }

    @DeleteMapping("/{scenarioId}")
    public R<Void> delete(@PathVariable String scenarioId) {
        service.delete(scenarioId);
        return R.ok();
    }

    @PostMapping("/{scenarioId}/enable")
    public R<Void> enable(@PathVariable String scenarioId) {
        service.enable(scenarioId);
        return R.ok();
    }

    @PostMapping("/{scenarioId}/disable")
    public R<Void> disable(@PathVariable String scenarioId) {
        service.disable(scenarioId);
        return R.ok();
    }

    @GetMapping("/{scenarioId}")
    public R<AuthScenario> detail(@PathVariable String scenarioId) {
        return R.ok(service.getById(scenarioId));
    }

    @GetMapping("/page")
    public R<PageResult<AuthScenario>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String status) {
        return R.ok(service.page(current, size, keyword, category, status));
    }
}
