package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthApplyTemplate;
import com.csg.prm.authorize.service.AuthApplyTemplateService;
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

/** 授权申请表单模板接口(可研 3.2.2.1.1.3.1.2):按授权类型配置字段/流程/验证规则。 */
@RestController
@RequestMapping("/api/dpr/auth/apply-template")
public class AuthApplyTemplateController {

    private final AuthApplyTemplateService service;

    public AuthApplyTemplateController(AuthApplyTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody AuthApplyTemplate t) {
        return R.ok(service.create(t));
    }

    @PutMapping
    public R<Void> update(@RequestBody AuthApplyTemplate t) {
        service.update(t);
        return R.ok();
    }

    @DeleteMapping("/{templateId}")
    public R<Void> delete(@PathVariable String templateId) {
        service.delete(templateId);
        return R.ok();
    }

    @PostMapping("/{templateId}/enable")
    public R<Void> enable(@PathVariable String templateId) {
        service.enable(templateId);
        return R.ok();
    }

    @PostMapping("/{templateId}/disable")
    public R<Void> disable(@PathVariable String templateId) {
        service.disable(templateId);
        return R.ok();
    }

    @GetMapping("/{templateId}")
    public R<AuthApplyTemplate> detail(@PathVariable String templateId) {
        return R.ok(service.getById(templateId));
    }

    @GetMapping("/page")
    public R<PageResult<AuthApplyTemplate>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String templateName,
                                                 @RequestParam(required = false) String authType,
                                                 @RequestParam(required = false) String templateStatus) {
        return R.ok(service.page(current, size, templateName, authType, templateStatus));
    }
}
