package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.authorize.service.AuthFilingService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 对外数据经营权授权备案接口(附录G / 附录F §3.4.6)。 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/filing")
public class AuthFilingController {

    private final AuthFilingService service;

    public AuthFilingController(AuthFilingService service) {
        this.service = service;
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody AuthFiling filing) {
        return Result.success(service.create(filing));
    }

    @PostMapping("/{filingId}/file")
    public Result<Void> file(@PathVariable String filingId) {
        service.file(filingId);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<AuthFiling>> page(@Valid PageRequest page,
                                          @RequestParam(required = false) String filingStatus,
                                          @RequestParam(required = false) String filingType) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), filingStatus, filingType));
    }
}
