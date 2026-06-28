package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthCatalogItem;
import com.csg.prm.authorize.service.AuthCatalogService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageQuery;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 授权域目录项接口:指引/场景/申请表单模板/协议模板库(IM-DAM-DPR-03-001-001-001/002/003 + 003-001)。 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/catalog")
public class AuthCatalogController {

    private final AuthCatalogService service;

    public AuthCatalogController(AuthCatalogService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> save(@Valid @RequestBody AuthCatalogItem item) {
        return Result.success(service.save(item));
    }

    @PostMapping("/{itemId}/enable")
    public Result<Void> enable(@PathVariable String itemId) {
        service.enable(itemId);
        return Result.success();
    }

    @PostMapping("/{itemId}/disable")
    public Result<Void> disable(@PathVariable String itemId) {
        service.disable(itemId);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<AuthCatalogItem>> page(@Valid PageQuery page,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String name,
                                               @RequestParam(required = false) String status) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), category, name, status));
    }
}
