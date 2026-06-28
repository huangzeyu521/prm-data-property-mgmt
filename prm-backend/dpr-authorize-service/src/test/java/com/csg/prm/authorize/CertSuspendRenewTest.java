package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.Accountability;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.service.AccountabilityService;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.query.PageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 监测联动熔断(暂停授权+追责留痕)与到期续签集成测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class CertSuspendRenewTest {

    @Autowired
    private AuthCertService certService;
    @Autowired
    private AccountabilityService accountabilityService;

    private String genCert(String assetId, LocalDateTime validDate) {
        AuthApply apply = new AuthApply();
        apply.setApplyId("AP-" + assetId);
        apply.setAssetId(assetId);
        apply.setAssetName(assetId + "-表");
        apply.setGranteeOrg("广州供电局");
        apply.setRightType("数据加工使用权");
        apply.setScope("全字段");
        apply.setValidDate(validDate);
        return certService.generateFromApply(apply);
    }

    @Test
    void suspend_by_asset_should_suspend_certs_and_open_accountability() {
        genCert("DA-SUSP-1", LocalDateTime.now().plusDays(180));
        genCert("DA-SUSP-1", LocalDateTime.now().plusDays(180));

        int n = certService.suspendByAsset("DA-SUSP-1", "检测到越权调用", "ALERT-SUSP", "越权调用");
        assertEquals(2, n, "应暂停该资产下全部生效证书");

        // 追责记录自动建账(待追责)
        var accs = accountabilityService.page(new PageQuery(), null, "DA-SUSP-1");
        assertEquals(2, accs.getTotal());
        assertEquals(Accountability.STATUS_PENDING, accs.getRecords().get(0).getHandleStatus());
        assertEquals("越权调用", accs.getRecords().get(0).getViolationType());
    }

    @Test
    void accountability_handle_close_loop() {
        String certId = genCert("DA-ACC-1", LocalDateTime.now().plusDays(90));
        certService.suspendByAsset("DA-ACC-1", "违规外发", "ALERT-ACC", "违规使用");
        Accountability acc = accountabilityService.page(new PageQuery(), null, "DA-ACC-1").getRecords().get(0);

        accountabilityService.handle(acc.getAccountId(), "广州供电局数字化部", "已启动追责");
        assertEquals(Accountability.STATUS_HANDLING,
                accountabilityService.page(new PageQuery(), null, "DA-ACC-1").getRecords().get(0).getHandleStatus());

        accountabilityService.close(acc.getAccountId(), "已问责整改");
        assertEquals(Accountability.STATUS_DONE,
                accountabilityService.page(new PageQuery(), null, "DA-ACC-1").getRecords().get(0).getHandleStatus());
    }

    @Test
    void renew_should_restore_suspended_cert_and_extend_validity() {
        String certId = genCert("DA-RENEW-1", LocalDateTime.now().plusDays(5));
        certService.suspendByAsset("DA-RENEW-1", "到期未续", "ALERT-R", "到期未续");
        assertEquals(AuthCert.STATUS_SUSPENDED, certService.getById(certId).getCertStatus());

        LocalDateTime newDate = LocalDateTime.now().plusYears(1);
        certService.renew(certId, newDate);
        AuthCert renewed = certService.getById(certId);
        assertEquals(AuthCert.STATUS_EFFECTIVE, renewed.getCertStatus(), "整改续签后应恢复生效");
        assertTrue(renewed.getValidDate().isAfter(LocalDateTime.now().plusMonths(11)));
    }

    @Test
    void renew_should_reject_revoked_or_past_date() {
        String certId = genCert("DA-RENEW-2", LocalDateTime.now().plusDays(5));
        // 过去日期被拒
        assertThrows(BusinessException.class, () -> certService.renew(certId, LocalDateTime.now().minusDays(1)));
        // 已撤销不可续签
        certService.revoke(certId);
        assertThrows(BusinessException.class, () -> certService.renew(certId, LocalDateTime.now().plusDays(30)));
    }

    @Test
    void find_expiring_should_list_certs_within_window() {
        genCert("DA-EXP-SOON", LocalDateTime.now().plusDays(10));
        genCert("DA-EXP-FAR", LocalDateTime.now().plusDays(400));

        boolean soonListed = certService.findExpiring(30).stream()
                .anyMatch(c -> "DA-EXP-SOON".equals(c.getAssetId()));
        boolean farListed = certService.findExpiring(30).stream()
                .anyMatch(c -> "DA-EXP-FAR".equals(c.getAssetId()));
        assertTrue(soonListed, "30天内到期证书应被列出");
        assertTrue(!farListed, "远期证书不应进入到期预警");
    }
}
