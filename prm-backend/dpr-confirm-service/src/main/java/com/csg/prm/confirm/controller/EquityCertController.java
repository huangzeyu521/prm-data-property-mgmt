package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.dto.CertRenderVO;
import com.csg.prm.confirm.entity.EquityCert;
import com.csg.prm.confirm.service.EquityCertService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 确权权益证书接口(IM-DAM-DPR-02-001-003-003)。 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/cert")
public class EquityCertController {

    private final EquityCertService service;

    public EquityCertController(EquityCertService service) {
        this.service = service;
    }

    @PostMapping("/issue")
    public Result<String> issue(@RequestParam String cardId,
                           @RequestParam(required = false) String issueUnit,
                           @RequestParam(required = false) String templateId,
                           @RequestParam(required = false) String templateName) {
        return Result.success(service.issue(cardId, issueUnit, templateId, templateName));
    }

    @GetMapping("/{certId}")
    public Result<EquityCert> detail(@PathVariable String certId) {
        return Result.success(service.getById(certId));
    }

    /** 在线预览:渲染证书内容(证书+卡片+模板)。 */
    @GetMapping("/{certId}/render")
    public Result<CertRenderVO> render(@PathVariable String certId) {
        return Result.success(service.render(certId));
    }

    @PostMapping("/{certId}/revoke")
    public Result<Void> revoke(@PathVariable String certId) {
        service.revoke(certId);
        return Result.success();
    }

    @GetMapping("/page")
    public Result<PageResult<EquityCert>> page(@Valid PageRequest page,
                                          @RequestParam(required = false) String cardId) {
        return Result.success(service.page(page.getCurrent(), page.getSize(), cardId));
    }
}
