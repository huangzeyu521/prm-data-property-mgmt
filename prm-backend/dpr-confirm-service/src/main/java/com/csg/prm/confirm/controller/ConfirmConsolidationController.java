package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.Result;
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
import org.springframework.validation.annotation.Validated;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 表级确权清单(M02)与权益归集判定/官方汇总表导出接口。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm")
public class ConfirmConsolidationController {

    private final ConfirmConsolidationService service;

    public ConfirmConsolidationController(ConfirmConsolidationService service) {
        this.service = service;
    }

    @PostMapping("/apply/{applyId}/table-items")
    public Result<Integer> saveTableItems(@PathVariable String applyId, @RequestBody List<ConfirmTableItem> items) {
        return Result.success(service.saveTableItems(applyId, items));
    }

    @GetMapping("/apply/{applyId}/table-items")
    public Result<List<ConfirmTableItem>> listTableItems(@PathVariable String applyId) {
        return Result.success(service.listTableItems(applyId));
    }

    @GetMapping("/apply/{applyId}/consolidation")
    public Result<ConsolidationResult> consolidation(@PathVariable String applyId) {
        return Result.success(service.judgeConsolidation(applyId));
    }

    /** 不落库试算:step1 选项变更时内联预览经营权归集判定(管制属性/涉第三方/经营权主张/其他约束)。 */
    @GetMapping("/consolidation/preview")
    public Result<ConsolidationResult> previewConsolidation(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean regulated,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean involvesThird,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean hasOperateClaim,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean otherRestriction) {
        return Result.success(service.previewConsolidation(regulated, involvesThird, hasOperateClaim, otherRestriction));
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
