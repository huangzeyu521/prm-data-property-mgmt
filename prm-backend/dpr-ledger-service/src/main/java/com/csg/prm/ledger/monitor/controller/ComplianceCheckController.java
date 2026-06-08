package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.ledger.monitor.dto.ComplianceReportVO;
import com.csg.prm.ledger.monitor.dto.ComplianceResultQuery;
import com.csg.prm.ledger.monitor.entity.ComplianceResult;
import com.csg.prm.ledger.monitor.service.ComplianceCheckService;
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
@RequestMapping("/api/dpr/monitor/compliance")
public class ComplianceCheckController {

    private final ComplianceCheckService service;

    public ComplianceCheckController(ComplianceCheckService service) {
        this.service = service;
    }

    /** 手工启动多维合规检查(有效期/权限范围/申请材料/协议内容),返回检查报告。 */
    @PostMapping("/check")
    public R<ComplianceReportVO> check() {
        return R.ok(service.runComplianceCheck());
    }

    /** 按报告ID查看检查报告。 */
    @GetMapping("/report/{reportId}")
    public R<ComplianceReportVO> report(@PathVariable String reportId) {
        return R.ok(service.report(reportId));
    }

    @PostMapping("/page")
    public R<PageResult<ComplianceResult>> page(@RequestBody ComplianceResultQuery query) {
        return R.ok(service.page(query));
    }
}
