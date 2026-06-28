package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.dto.LedgerOverviewVO;
import com.csg.prm.ledger.service.LedgerOverviewService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产权总体概览接口(对应界面 IM-DAM-DPR-01-001-001-001 产权总体概览)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/ledger/overview")
public class LedgerOverviewController {

    private final LedgerOverviewService service;

    public LedgerOverviewController(LedgerOverviewService service) {
        this.service = service;
    }

    @GetMapping
    public Result<LedgerOverviewVO> overview() {
        return Result.success(service.overview());
    }
}
