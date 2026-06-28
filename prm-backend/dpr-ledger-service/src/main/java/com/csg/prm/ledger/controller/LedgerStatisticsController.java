package com.csg.prm.ledger.controller;

import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.dto.LedgerStatisticsVO;
import com.csg.prm.ledger.service.LedgerStatisticsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产权台账统计分析接口(IM-DAM-DPR-01-001-001-007)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/ledger/statistics")
public class LedgerStatisticsController {

    private final LedgerStatisticsService service;

    public LedgerStatisticsController(LedgerStatisticsService service) {
        this.service = service;
    }

    @GetMapping
    public Result<LedgerStatisticsVO> statistics() {
        return Result.success(service.statistics());
    }
}
