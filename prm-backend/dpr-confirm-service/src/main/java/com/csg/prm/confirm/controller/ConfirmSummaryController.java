package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.Result;
import com.csg.prm.confirm.entity.ConfirmSummary;
import com.csg.prm.confirm.service.ConfirmSummaryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 确权汇总表(表3/表4)接口。 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/summary")
public class ConfirmSummaryController {

    private final ConfirmSummaryService service;

    public ConfirmSummaryController(ConfirmSummaryService service) {
        this.service = service;
    }

    @GetMapping("/by-apply/{applyId}")
    public Result<List<ConfirmSummary>> listByApply(@PathVariable String applyId) {
        return Result.success(service.listByApply(applyId));
    }
}
