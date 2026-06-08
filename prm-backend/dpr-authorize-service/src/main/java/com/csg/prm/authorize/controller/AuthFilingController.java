package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.authorize.service.AuthFilingService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 对外数据经营权授权备案接口(附录G / 附录F §3.4.6)。 */
@RestController
@RequestMapping("/api/dpr/auth/filing")
public class AuthFilingController {

    private final AuthFilingService service;

    public AuthFilingController(AuthFilingService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody AuthFiling filing) {
        return R.ok(service.create(filing));
    }

    @PostMapping("/{filingId}/file")
    public R<Void> file(@PathVariable String filingId) {
        service.file(filingId);
        return R.ok();
    }

    @GetMapping("/page")
    public R<PageResult<AuthFiling>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String filingStatus) {
        return R.ok(service.page(current, size, filingStatus));
    }
}
