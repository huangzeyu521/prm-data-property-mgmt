package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitAuditBase;
import com.csg.prm.confirm.aitool.entity.AitAuditResult;
import com.csg.prm.confirm.aitool.entity.AitEvidence;
import com.csg.prm.confirm.aitool.service.AitAuditAgentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 智能确权 Agent 审核与推理决策接口(可研 3.1)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/agent")
public class AitAuditAgentController {

    private final AitAuditAgentService service;

    public AitAuditAgentController(AitAuditAgentService service) {
        this.service = service;
    }

    /** 发起 Agent 多阶段审核(规则预筛→快速/深度通道→结构化结论)。 */
    @PostMapping("/audit")
    public R<AitAuditResult> audit(@RequestParam String applyId) {
        return R.ok(service.audit(applyId));
    }

    /** 取某申请的最新 Agent 审核结果。 */
    @GetMapping("/{applyId}")
    public R<AitAuditResult> get(@PathVariable String applyId) {
        return R.ok(service.getByApply(applyId));
    }

    /** 3.2#1 批量审核(传 applyIds 数组),形成台账。 */
    @PostMapping("/batch-audit")
    public R<List<AitAuditResult>> batchAudit(@RequestBody List<String> applyIds) {
        return R.ok(service.batchAudit(applyIds));
    }

    /** 3.2#1 字段级结论(该申请各材料的清洗审核底表)。 */
    @GetMapping("/{applyId}/field-level")
    public R<List<AitAuditBase>> fieldLevel(@PathVariable String applyId) {
        return R.ok(service.fieldLevel(applyId));
    }

    /** 3.2#4 审核证据链档案。 */
    @GetMapping("/{applyId}/evidence")
    public R<AitEvidence> evidence(@PathVariable String applyId) {
        return R.ok(service.getEvidence(applyId));
    }

    /** 3.2#5 审核台账分页(多维筛选)。 */
    @GetMapping("/ledger/page")
    public R<PageResult<AitAuditResult>> ledgerPage(PageQuery query,
                                                    @RequestParam(required = false) String assetId,
                                                    @RequestParam(required = false) String dataClass,
                                                    @RequestParam(required = false) String riskLevel,
                                                    @RequestParam(required = false) String channel,
                                                    @RequestParam(required = false) String authAdvice) {
        return R.ok(service.ledgerPage(query, assetId, dataClass, riskLevel, channel, authAdvice));
    }

    /** 3.2#5 台账汇总统计(按风险/级别/建议/通道/业务域)。 */
    @GetMapping("/ledger/stats")
    public R<Map<String, Object>> ledgerStats() {
        return R.ok(service.ledgerStats());
    }

    /** 3.2#5 审核台账导出 Excel。 */
    @GetMapping("/ledger/export")
    public ResponseEntity<byte[]> ledgerExport(@RequestParam(required = false) String assetId,
                                               @RequestParam(required = false) String dataClass,
                                               @RequestParam(required = false) String riskLevel,
                                               @RequestParam(required = false) String channel,
                                               @RequestParam(required = false) String authAdvice) {
        return excel("审核台账.xlsx",
                service.exportLedgerExcel(assetId, dataClass, riskLevel, channel, authAdvice));
    }

    /** 3.2#3 审核报告(Word)。 */
    @GetMapping("/{applyId}/report")
    public ResponseEntity<byte[]> report(@PathVariable String applyId) {
        return wordResp("智能确权审核报告-" + applyId + ".docx", service.exportReportWord(applyId));
    }

    /** 3.2#3 确权登记辅助材料(Word)。 */
    @GetMapping("/{applyId}/registration-doc")
    public ResponseEntity<byte[]> registrationDoc(@PathVariable String applyId) {
        return wordResp("确权登记辅助材料-" + applyId + ".docx", service.registrationDoc(applyId));
    }

    /** 3.2#3 法律意见辅助材料(Word)。 */
    @GetMapping("/{applyId}/legal-opinion")
    public ResponseEntity<byte[]> legalOpinion(@PathVariable String applyId) {
        return wordResp("法律意见辅助材料-" + applyId + ".docx", service.legalOpinion(applyId));
    }

    private ResponseEntity<byte[]> wordResp(String name, byte[] data) {
        String fn = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(data);
    }

    private ResponseEntity<byte[]> excel(String name, byte[] data) {
        String fn = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
