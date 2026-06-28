package com.csg.prm.ledger.monitor.controller;

import com.csg.prm.common.api.Result;
import com.csg.prm.ledger.monitor.dto.LinkageResult;
import com.csg.prm.ledger.monitor.service.RightsLinkageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权益动态监测联动接口(附录F 3.4.5)。
 * 上报违规/越权 -> 生成预警 + 联动熔断暂停授权 + 追责。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/monitor/linkage")
public class RightsLinkageController {

    private final RightsLinkageService service;

    public RightsLinkageController(RightsLinkageService service) {
        this.service = service;
    }

    @PostMapping("/violation")
    public Result<LinkageResult> violation(@RequestParam String assetId,
                                      @RequestParam(required = false) String ruleId,
                                      @RequestParam(required = false) String violationType,
                                      @RequestParam(required = false) String desc) {
        return Result.success(service.onViolation(assetId, ruleId, violationType, desc));
    }

    /** 权属变动联动:派生重确权工单(数据新增/来源变更/到期,附录F 3.3.2) */
    @PostMapping("/re-confirm")
    public Result<LinkageResult> reConfirm(@RequestParam String assetId,
                                      @RequestParam(required = false) String assetName,
                                      @RequestParam(required = false) String rightType,
                                      @RequestParam(required = false) String triggerType,
                                      @RequestParam(required = false) String desc) {
        return Result.success(service.triggerReConfirm(assetId, assetName, rightType, triggerType, desc));
    }
}
