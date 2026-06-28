package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthScenario;
import com.csg.prm.authorize.service.AuthScenarioService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageQuery;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/dpr/auth/scenario")
public class AuthScenarioController {

    private final AuthScenarioService service;

    public AuthScenarioController(AuthScenarioService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody AuthScenario s) {
        return Result.success(service.create(s));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody AuthScenario s) {
        service.update(s);
        return Result.success();
    }

    @DeleteMapping("/{scenarioId}")
    public Result<Void> delete(@PathVariable String scenarioId) {
        service.delete(scenarioId);
        return Result.success();
    }

    @PostMapping("/{scenarioId}/enable")
    public Result<Void> enable(@PathVariable String scenarioId) {
        service.enable(scenarioId);
        return Result.success();
    }

    @PostMapping("/{scenarioId}/disable")
    public Result<Void> disable(@PathVariable String scenarioId) {
        service.disable(scenarioId);
        return Result.success();
    }

    @GetMapping("/{scenarioId}")
    public Result<AuthScenario> detail(@PathVariable String scenarioId) {
        return Result.success(service.getById(scenarioId));
    }

    @GetMapping("/page")
    public Result<PageResult<AuthScenario>> page(@Valid PageQuery page,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(required = false) String status) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), keyword, category, status));
    }
}
