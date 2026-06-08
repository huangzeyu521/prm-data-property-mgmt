package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthCatalogItem;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.authorize.service.AuthCatalogService;
import com.csg.prm.authorize.service.AuthComplianceService;
import com.csg.prm.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F-03 补全能力测试:目录项(指引/场景/模板)、合规校验、协议全生命周期。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthExtrasTest {

    @Autowired private AuthCatalogService catalogService;
    @Autowired private AuthComplianceService complianceService;
    @Autowired private AuthAgreementService agreementService;

    @Test
    void catalog_save_disable_page() {
        AuthCatalogItem g = new AuthCatalogItem();
        g.setCategory(AuthCatalogItem.CAT_SCENARIO);
        g.setName("电力金融征信");
        String id = catalogService.save(g);
        assertNotNull(id);
        assertTrue(catalogService.page(1, 10, AuthCatalogItem.CAT_SCENARIO, null, null).getTotal() >= 1);
        catalogService.disable(id);
        assertTrue(catalogService.page(1, 10, AuthCatalogItem.CAT_SCENARIO, null, "停用").getTotal() >= 1);
    }

    @Test
    void compliance_check_and_page() {
        String id = complianceService.runCheck("AP-1", "黄", "授权范围接近确权边界");
        assertNotNull(id);
        assertTrue(complianceService.page(1, 10, "AP-1", null).getTotal() >= 1);
    }

    @Test
    void agreement_double_sign_lifecycle() {
        String id = agreementService.generate("AP-2", "TPL-1", "广州供电局");
        assertEquals(AuthAgreement.SEAL_PENDING, agreementService.getById(id).getSealStatus());
        // 未双签不可审核
        assertThrows(BizException.class, () -> agreementService.review(id, true));
        // 甲方签 -> 待对方签
        agreementService.signByGrantor(id, "oss://agreement/AP-2-grantor.pdf");
        assertEquals(AuthAgreement.SEAL_PARTIAL, agreementService.getById(id).getSealStatus());
        assertThrows(BizException.class, () -> agreementService.review(id, true), "仅单方签署不可审核");
        // 乙方签 -> 已双签
        agreementService.signByGrantee(id, "oss://agreement/AP-2-grantee.pdf");
        assertEquals(AuthAgreement.SEAL_SIGNED, agreementService.getById(id).getSealStatus());
        assertTrue(agreementService.getById(id).getGrantorSigned());
        assertTrue(agreementService.getById(id).getGranteeSigned());
        // 双签后可审核 -> 归档
        agreementService.review(id, true);
        assertEquals(AuthAgreement.REVIEW_PASS, agreementService.getById(id).getReviewStatus());
        agreementService.archive(id);
        assertEquals(AuthAgreement.ARCHIVE_YES, agreementService.getById(id).getArchiveStatus());
    }
}
