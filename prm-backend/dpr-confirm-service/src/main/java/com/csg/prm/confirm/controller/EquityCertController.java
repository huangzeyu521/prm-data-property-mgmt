package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.confirm.dto.CertRenderVO;
import com.csg.prm.confirm.entity.EquityCert;
import com.csg.prm.confirm.service.EquityCertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 确权权益证书接口(IM-DAM-DPR-02-001-003-003)。 */
@RestController
@RequestMapping("/api/dpr/confirm/cert")
public class EquityCertController {

    private final EquityCertService service;

    public EquityCertController(EquityCertService service) {
        this.service = service;
    }

    @PostMapping("/issue")
    public R<String> issue(@RequestParam String cardId,
                           @RequestParam(required = false) String issueUnit,
                           @RequestParam(required = false) String templateId,
                           @RequestParam(required = false) String templateName) {
        return R.ok(service.issue(cardId, issueUnit, templateId, templateName));
    }

    @GetMapping("/{certId}")
    public R<EquityCert> detail(@PathVariable String certId) {
        return R.ok(service.getById(certId));
    }

    /** 在线预览:渲染证书内容(证书+卡片+模板)。 */
    @GetMapping("/{certId}/render")
    public R<CertRenderVO> render(@PathVariable String certId) {
        return R.ok(service.render(certId));
    }

    @PostMapping("/{certId}/revoke")
    public R<Void> revoke(@PathVariable String certId) {
        service.revoke(certId);
        return R.ok();
    }

    @GetMapping("/page")
    public R<PageResult<EquityCert>> page(@RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String cardId) {
        return R.ok(service.page(current, size, cardId));
    }
}
