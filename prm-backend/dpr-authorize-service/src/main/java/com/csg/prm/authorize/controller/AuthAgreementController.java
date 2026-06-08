package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 授权协议接口:生成/签章上传/审核/存档/下载(IM-DAM-DPR-03-001-003)。 */
@RestController
@RequestMapping("/api/dpr/auth/agreement")
public class AuthAgreementController {

    private final AuthAgreementService service;

    public AuthAgreementController(AuthAgreementService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public R<String> generate(@RequestParam String applyId,
                              @RequestParam(required = false) String templateId,
                              @RequestParam(required = false) String granteeOrg) {
        return R.ok(service.generate(applyId, templateId, granteeOrg));
    }

    @PostMapping("/{agreementId}/sign-grantor")
    public R<Void> signByGrantor(@PathVariable String agreementId, @RequestParam(required = false) String fileUrl) {
        service.signByGrantor(agreementId, fileUrl);
        return R.ok();
    }

    @PostMapping("/{agreementId}/sign-grantee")
    public R<Void> signByGrantee(@PathVariable String agreementId, @RequestParam(required = false) String fileUrl) {
        service.signByGrantee(agreementId, fileUrl);
        return R.ok();
    }

    @PostMapping("/{agreementId}/review")
    public R<Void> review(@PathVariable String agreementId, @RequestParam boolean pass) {
        service.review(agreementId, pass);
        return R.ok();
    }

    @PostMapping("/{agreementId}/archive")
    public R<Void> archive(@PathVariable String agreementId) {
        service.archive(agreementId);
        return R.ok();
    }

    @GetMapping("/{agreementId}")
    public R<AuthAgreement> detail(@PathVariable String agreementId) {
        return R.ok(service.getById(agreementId));
    }

    @GetMapping("/page")
    public R<PageResult<AuthAgreement>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String reviewStatus,
                                             @RequestParam(required = false) String archiveStatus) {
        return R.ok(service.page(current, size, reviewStatus, archiveStatus));
    }
}
