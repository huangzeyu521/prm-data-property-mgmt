package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.service.AitConflictService;
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
 * 智能确权辅助工具-权属冲突识别接口(M2 / SW-005~006)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/conflict")
public class AitConflictController {

    private final AitConflictService service;

    public AitConflictController(AitConflictService service) {
        this.service = service;
    }

    /** 登记权属主张(构建知识图谱) */
    @PostMapping("/claim")
    public R<String> addClaim(@RequestBody AitKgClaim claim) {
        return R.ok(service.addClaim(claim));
    }

    /** 提交当前申请主张并做四类冲突检测 */
    @PostMapping("/detect")
    public R<List<AitConflict>> detect(@RequestBody AitKgClaim current) {
        current.setSourceType(AitKgClaim.SRC_CURRENT);
        service.addClaim(current);
        return R.ok(service.detect(current));
    }

    @GetMapping("/claims")
    public R<List<AitKgClaim>> claims(@RequestParam String assetId) {
        return R.ok(service.claims(assetId));
    }

    /** 冲突列表(支持 资产/冲突类型/风险等级/起止时间 多维筛选)(#17) */
    @GetMapping("/list")
    public R<List<AitConflict>> list(@RequestParam(required = false) String assetId,
                                     @RequestParam(required = false) String conflictType,
                                     @RequestParam(required = false) String riskLevel,
                                     @RequestParam(required = false) String startTime,
                                     @RequestParam(required = false) String endTime) {
        return R.ok(service.conflicts(assetId, conflictType, riskLevel, startTime, endTime));
    }

    @PostMapping("/{conflictId}/resolve")
    public R<Void> resolve(@PathVariable String conflictId, @RequestParam(required = false) String feedback) {
        service.resolve(conflictId, feedback);
        return R.ok();
    }

    /** 冲突分析报告(支持多维筛选)(#17) */
    @GetMapping("/report")
    public R<Map<String, Object>> report(@RequestParam String assetId,
                                         @RequestParam(required = false) String conflictType,
                                         @RequestParam(required = false) String riskLevel,
                                         @RequestParam(required = false) String startTime,
                                         @RequestParam(required = false) String endTime) {
        return R.ok(service.report(assetId, conflictType, riskLevel, startTime, endTime));
    }

    /** 导出冲突分析报告为 Word(.docx)(#17;PDF 由前端打印导出) */
    @GetMapping("/report/export")
    public ResponseEntity<byte[]> exportReport(@RequestParam String assetId,
                                               @RequestParam(required = false) String conflictType,
                                               @RequestParam(required = false) String riskLevel,
                                               @RequestParam(required = false) String startTime,
                                               @RequestParam(required = false) String endTime) {
        byte[] data = service.exportReportWord(assetId, conflictType, riskLevel, startTime, endTime);
        String fn = URLEncoder.encode("权属冲突分析报告-" + assetId + ".docx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fn)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(data);
    }
}
