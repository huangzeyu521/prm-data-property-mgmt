package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.monitor.dto.ComplianceReportVO;
import com.csg.prm.ledger.monitor.dto.ComplianceResultQuery;
import com.csg.prm.ledger.monitor.entity.ComplianceResult;
import com.csg.prm.ledger.monitor.service.ComplianceCheckService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合规性检查接口(IM-DAM-DPR-01-001-002-003)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/monitor/compliance")
public class ComplianceCheckController {

    private final ComplianceCheckService service;

    public ComplianceCheckController(ComplianceCheckService service) {
        this.service = service;
    }

    /** 手工启动多维合规检查(有效期/权限范围/申请材料/协议内容),返回检查报告。 */
    @PostMapping("/check")
    public Result<ComplianceReportVO> check() {
        return Result.success(service.runComplianceCheck());
    }

    /** 按报告ID查看检查报告。 */
    @GetMapping("/report/{reportId}")
    public Result<ComplianceReportVO> report(@PathVariable String reportId) {
        return Result.success(service.report(reportId));
    }

    @PostMapping("/page")
    public Result<PageResult<ComplianceResult>> page(@Valid @RequestBody ComplianceResultQuery query) {
        return Result.success(service.page(query));
    }
}
