package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthApplyTemplate;
import com.csg.prm.authorize.service.AuthApplyTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageRequest;
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

/** 授权申请表单模板接口(可研 3.2.2.1.1.3.1.2):按授权类型配置字段/流程/验证规则。 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/apply-template")
public class AuthApplyTemplateController {

    private final AuthApplyTemplateService service;

    public AuthApplyTemplateController(AuthApplyTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody AuthApplyTemplate t) {
        return Result.success(service.create(t));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody AuthApplyTemplate t) {
        service.update(t);
        return Result.success();
    }

    @DeleteMapping("/{templateId}")
    public Result<Void> delete(@PathVariable String templateId) {
        service.delete(templateId);
        return Result.success();
    }

    @PostMapping("/{templateId}/enable")
    public Result<Void> enable(@PathVariable String templateId) {
        service.enable(templateId);
        return Result.success();
    }

    @PostMapping("/{templateId}/disable")
    public Result<Void> disable(@PathVariable String templateId) {
        service.disable(templateId);
        return Result.success();
    }

    @GetMapping("/{templateId}")
    public Result<AuthApplyTemplate> detail(@PathVariable String templateId) {
        return Result.success(service.getById(templateId));
    }

    @GetMapping("/page")
    public Result<PageResult<AuthApplyTemplate>> page(@Valid PageRequest page,
                                                 @RequestParam(required = false) String templateName,
                                                 @RequestParam(required = false) String authType,
                                                 @RequestParam(required = false) String templateStatus) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), templateName, authType, templateStatus));
    }
}
