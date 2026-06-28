package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.dto.AuthDashboardVO;
import com.csg.prm.authorize.service.AuthDashboardService;
import com.csg.prm.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

/**
 * 授权看板接口(IM-DAM-DPR-04-003-001-001)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/dashboard")
public class AuthDashboardController {

    private final AuthDashboardService service;

    public AuthDashboardController(AuthDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public Result<AuthDashboardVO> dashboard(@RequestParam(required = false) String scenario,
                                        @RequestParam(required = false) String deptName,
                                        @RequestParam(required = false) String startTime,
                                        @RequestParam(required = false) String endTime) {
        return Result.success(service.dashboard(scenario, deptName, startTime, endTime));
    }
}
