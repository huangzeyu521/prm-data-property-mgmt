package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.dto.AuthDashboardVO;
import com.csg.prm.authorize.service.AuthDashboardService;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权看板接口(IM-DAM-DPR-04-003-001-001)。
 */
@RestController
@RequestMapping("/api/dpr/auth/dashboard")
public class AuthDashboardController {

    private final AuthDashboardService service;

    public AuthDashboardController(AuthDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public R<AuthDashboardVO> dashboard() {
        return R.ok(service.dashboard());
    }
}
