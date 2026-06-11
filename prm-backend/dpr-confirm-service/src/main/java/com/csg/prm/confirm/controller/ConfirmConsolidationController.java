package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.confirm.entity.ConfirmTableItem;
import com.csg.prm.confirm.service.ConfirmConsolidationService;
import com.csg.prm.confirm.service.ConfirmConsolidationService.ConsolidationResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 表级确权清单(M02)与权益归集判定/官方汇总表导出接口。
 */
@RestController
@RequestMapping("/api/dpr/confirm")
public class ConfirmConsolidationController {

    private final ConfirmConsolidationService service;

    public ConfirmConsolidationController(ConfirmConsolidationService service) {
        this.service = service;
    }

    @PostMapping("/apply/{applyId}/table-items")
    public R<Integer> saveTableItems(@PathVariable String applyId, @RequestBody List<ConfirmTableItem> items) {
        return R.ok(service.saveTableItems(applyId, items));
    }

    @GetMapping("/apply/{applyId}/table-items")
    public R<List<ConfirmTableItem>> listTableItems(@PathVariable String applyId) {
        return R.ok(service.listTableItems(applyId));
    }

    @GetMapping("/apply/{applyId}/consolidation")
    public R<ConsolidationResult> consolidation(@PathVariable String applyId) {
        return R.ok(service.judgeConsolidation(applyId));
    }

    @GetMapping("/summary/confirm-export")
    public ResponseEntity<byte[]> exportConfirmSummary() {
        return excel(service.exportConfirmSummary(), "数据确权信息汇总表.xlsx");
    }

    @GetMapping("/summary/equity-export")
    public ResponseEntity<byte[]> exportEquityConsolidation() {
        return excel(service.exportEquityConsolidation(), "数据权益内部管理汇总表(分子公司共享网公司).xlsx");
    }

    private ResponseEntity<byte[]> excel(byte[] data, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
