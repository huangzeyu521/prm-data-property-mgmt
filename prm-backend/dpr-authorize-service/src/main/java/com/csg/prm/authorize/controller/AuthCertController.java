package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权权益证书接口(IM-DAM-DPR-03-001-004 授权权益管理)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/cert")
public class AuthCertController {

    private final AuthCertService service;

    public AuthCertController(AuthCertService service) {
        this.service = service;
    }

    @GetMapping("/{certId}")
    public Result<AuthCert> detail(@PathVariable String certId) {
        return Result.success(service.getById(certId));
    }

    /** 在线预览:渲染证书内容(证书+模板+合规校验)。 */
    @GetMapping("/{certId}/render")
    public Result<com.csg.prm.authorize.dto.AuthCertRenderVO> render(@PathVariable String certId) {
        return Result.success(service.render(certId));
    }

    @PostMapping("/{certId}/revoke")
    public Result<Void> revoke(@PathVariable String certId) {
        service.revoke(certId);
        return Result.success();
    }

    /**
     * 监测联动熔断:暂停某资产下所有生效证书并建追责记录。
     * 供权益动态监测(F-01)在识别违规/越权时联动调用(本地桩 / 生产 Feign)。
     */
    @PostMapping("/suspend-by-asset")
    public Result<Integer> suspendByAsset(@RequestParam String assetId,
                                     @RequestParam(required = false) String reason,
                                     @RequestParam(required = false) String sourceAlertId,
                                     @RequestParam(required = false) String violationType) {
        return Result.success(service.suspendByAsset(assetId, reason, sourceAlertId, violationType));
    }

    /** 到期续签:延长有效期(整改后亦可恢复已暂停证书) */
    @PostMapping("/{certId}/renew")
    public Result<Void> renew(@PathVariable String certId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validDate) {
        service.renew(certId, validDate);
        return Result.success();
    }

    /** 到期预警:days 天内到期(或已过期)的生效证书 */
    @GetMapping("/expiring")
    public Result<List<AuthCert>> expiring(@RequestParam(defaultValue = "30") int days) {
        return Result.success(service.findExpiring(days));
    }

    @PostMapping("/page")
    public Result<PageResult<AuthCert>> page(@Valid @RequestBody PageRequest query) {
        return Result.success(service.page(query));
    }
}
