package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.query.PageQuery;
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
@RequestMapping("/api/dpr/auth/cert")
public class AuthCertController {

    private final AuthCertService service;

    public AuthCertController(AuthCertService service) {
        this.service = service;
    }

    @GetMapping("/{certId}")
    public R<AuthCert> detail(@PathVariable String certId) {
        return R.ok(service.getById(certId));
    }

    @PostMapping("/{certId}/revoke")
    public R<Void> revoke(@PathVariable String certId) {
        service.revoke(certId);
        return R.ok();
    }

    /**
     * 监测联动熔断:暂停某资产下所有生效证书并建追责记录。
     * 供权益动态监测(F-01)在识别违规/越权时联动调用(本地桩 / 生产 Feign)。
     */
    @PostMapping("/suspend-by-asset")
    public R<Integer> suspendByAsset(@RequestParam String assetId,
                                     @RequestParam(required = false) String reason,
                                     @RequestParam(required = false) String sourceAlertId,
                                     @RequestParam(required = false) String violationType) {
        return R.ok(service.suspendByAsset(assetId, reason, sourceAlertId, violationType));
    }

    /** 到期续签:延长有效期(整改后亦可恢复已暂停证书) */
    @PostMapping("/{certId}/renew")
    public R<Void> renew(@PathVariable String certId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validDate) {
        service.renew(certId, validDate);
        return R.ok();
    }

    /** 到期预警:days 天内到期(或已过期)的生效证书 */
    @GetMapping("/expiring")
    public R<List<AuthCert>> expiring(@RequestParam(defaultValue = "30") int days) {
        return R.ok(service.findExpiring(days));
    }

    @PostMapping("/page")
    public R<PageResult<AuthCert>> page(@RequestBody PageQuery query) {
        return R.ok(service.page(query));
    }
}
