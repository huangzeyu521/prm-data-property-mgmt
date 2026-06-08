package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthCertTemplate;
import com.csg.prm.authorize.service.AuthCertTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 授权权益证书模板接口(可研 3.2.2.1.1.3.4.2)。 */
@RestController
@RequestMapping("/api/dpr/auth/cert-template")
public class AuthCertTemplateController {

    private final AuthCertTemplateService service;

    public AuthCertTemplateController(AuthCertTemplateService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody AuthCertTemplate t) {
        return R.ok(service.create(t));
    }

    @PutMapping
    public R<Void> update(@RequestBody AuthCertTemplate t) {
        service.update(t);
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

    @GetMapping("/page")
    public R<PageResult<AuthCertTemplate>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String templateName,
                                                @RequestParam(required = false) String certType,
                                                @RequestParam(required = false) String templateStatus) {
        return R.ok(service.page(current, size, templateName, certType, templateStatus));
    }
}
