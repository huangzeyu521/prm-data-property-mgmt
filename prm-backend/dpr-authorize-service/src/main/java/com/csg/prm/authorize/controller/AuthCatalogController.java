package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthCatalogItem;
import com.csg.prm.authorize.service.AuthCatalogService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 授权域目录项接口:指引/场景/申请表单模板/协议模板库(IM-DAM-DPR-03-001-001-001/002/003 + 003-001)。 */
@RestController
@RequestMapping("/api/dpr/auth/catalog")
public class AuthCatalogController {

    private final AuthCatalogService service;

    public AuthCatalogController(AuthCatalogService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> save(@RequestBody AuthCatalogItem item) {
        return R.ok(service.save(item));
    }

    @PostMapping("/{itemId}/enable")
    public R<Void> enable(@PathVariable String itemId) {
        service.enable(itemId);
        return R.ok();
    }

    @PostMapping("/{itemId}/disable")
    public R<Void> disable(@PathVariable String itemId) {
        service.disable(itemId);
        return R.ok();
    }

    @GetMapping("/page")
    public R<PageResult<AuthCatalogItem>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String status) {
        return R.ok(service.page(current, size, category, name, status));
    }
}
