package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.Result;
import com.csg.prm.confirm.dto.ConfirmDashboardVO;
import com.csg.prm.confirm.service.ConfirmDashboardService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 确权看板接口(IM-DAM-DPR-04-002-001-001)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/dashboard")
public class ConfirmDashboardController {

    private final ConfirmDashboardService service;

    public ConfirmDashboardController(ConfirmDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public Result<ConfirmDashboardVO> dashboard(@RequestParam(required = false) String deptName,
                                           @RequestParam(required = false) String startTime,
                                           @RequestParam(required = false) String endTime) {
        return Result.success(service.dashboard(deptName, startTime, endTime));
    }
}
