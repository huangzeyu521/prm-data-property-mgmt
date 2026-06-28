package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitDecision;
import com.csg.prm.confirm.aitool.service.AitDecisionService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能确权辅助工具-确权决策支持接口(M3 / SW-007~008)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/aitool/decision")
public class AitDecisionController {

    private final AitDecisionService service;

    public AitDecisionController(AitDecisionService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public Result<AitDecision> analyze(@RequestParam String applyId) {
        return Result.success(service.analyze(applyId));
    }

    @GetMapping("/by-apply/{applyId}")
    public Result<AitDecision> byApply(@PathVariable String applyId) {
        return Result.success(service.getByApply(applyId));
    }

    @GetMapping("/page")
    public Result<PageResult<AitDecision>> page(@Valid PageQuery query,
                                           @RequestParam(required = false) String prediction) {
        return Result.success(service.page(query, prediction));
    }
}
