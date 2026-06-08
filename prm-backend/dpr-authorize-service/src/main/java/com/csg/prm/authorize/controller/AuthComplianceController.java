package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthCompliance;
import com.csg.prm.authorize.service.AuthComplianceService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 授权合规校验接口(IM-DAM-DPR-03-001-001-006)。 */
@RestController
@RequestMapping("/api/dpr/auth/compliance")
public class AuthComplianceController {

    private final AuthComplianceService service;

    public AuthComplianceController(AuthComplianceService service) {
        this.service = service;
    }

    @PostMapping("/check")
    public R<String> check(@RequestParam String applyId,
                           @RequestParam(required = false) String riskLevel,
                           @RequestParam(required = false) String problemDesc) {
        return R.ok(service.runCheck(applyId, riskLevel, problemDesc));
    }

    @GetMapping("/page")
    public R<PageResult<AuthCompliance>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String applyId,
                                              @RequestParam(required = false) String riskLevel) {
        return R.ok(service.page(current, size, applyId, riskLevel));
    }
}
