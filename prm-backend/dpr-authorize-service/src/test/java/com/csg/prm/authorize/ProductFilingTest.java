package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.authorize.entity.AuthMaterial;
import com.csg.prm.authorize.mapper.AuthAgreementMapper;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.mapper.AuthMaterialMapper;
import com.csg.prm.authorize.service.AuthFilingService;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据产品备案(附录D 附件2 表2)测试:协议第四章(三)——乙方对外提供数据产品/服务须在甲方处备案。
 * 涉及数据表 ⊆ 协议附件1《数据授权清单》(越权校验) + 完成备案须先传《安全合规评审意见》。
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductFilingTest {

    @Autowired private AuthFilingService filingService;
    @Autowired private AuthAgreementMapper agreementMapper;
    @Autowired private AuthApplyMapper applyMapper;
    @Autowired private AuthMaterialMapper materialMapper;

    /** 造一份已归档的经营权协议(专项,覆盖数据表=客户用电信息表)。 */
    private String archivedAgreement(String applyId) {
        AuthApply apply = new AuthApply();
        apply.setApplyId(applyId);
        apply.setAssetId("AST-PF-" + applyId);
        apply.setAssetName("客户用电信息表");
        apply.setGranteeOrg("南网数字产业公司");
        apply.setRightType("经营权");
        applyMapper.insert(apply);
        AuthAgreement ag = new AuthAgreement();
        ag.setAgreementNo("XY-PF-" + applyId);
        ag.setApplyId(applyId);
        ag.setGranteeOrg("南网数字产业公司");
        ag.setAgreementType("经营权");
        ag.setSealStatus(AuthAgreement.SEAL_SIGNED);
        ag.setReviewStatus(AuthAgreement.REVIEW_PASS);
        ag.setArchiveStatus(AuthAgreement.ARCHIVE_YES);
        agreementMapper.insert(ag);
        return ag.getAgreementId();
    }

    private AuthFiling productFiling(String agreementId, String tables) {
        AuthFiling f = new AuthFiling();
        f.setFilingType(AuthFiling.TYPE_PRODUCT);
        f.setAgreementId(agreementId);
        f.setFilingOrg("南网数字产业公司");
        f.setProductName("电力信用分产品");
        f.setProductIntro("基于用电行为的企业信用评估数据产品");
        f.setAppScenario("金融征信");
        f.setServiceTarget("银行/金融机构");
        f.setInvolvedTables(tables);
        return f;
    }

    @Test
    void product_filing_requires_tables_within_agreement_attachment1() {
        String agId = archivedAgreement("AP-PF-1");
        // 清单外数据表 → 越权拦截
        BusinessException ex = assertThrows(BusinessException.class,
                () -> filingService.create(productFiling(agId, "客户用电信息表、台区负荷数据")));
        assertTrue(ex.getMessage().contains("台区负荷数据"), "应点名清单外的表:" + ex.getMessage());
        // 全部落在附件1内 → 建档成功(CP-前缀,待备案)
        String filingId = filingService.create(productFiling(agId, "客户用电信息表"));
        assertNotNull(filingId);
    }

    @Test
    void product_filing_file_requires_review_opinion_material() {
        String agId = archivedAgreement("AP-PF-2");
        String filingId = filingService.create(productFiling(agId, "客户用电信息表"));
        // 未传《安全合规评审意见》→ 不得完成备案
        BusinessException ex = assertThrows(BusinessException.class, () -> filingService.file(filingId));
        assertTrue(ex.getMessage().contains("评审意见"), ex.getMessage());
        // 上传评审意见(材料挂 filingId)后放行
        AuthMaterial m = new AuthMaterial();
        m.setApplyId(filingId);
        m.setMaterialName("安全合规评审意见");
        materialMapper.insert(m);
        filingService.file(filingId);
    }

    @Test
    void product_filing_requires_archived_agreement() {
        // 未归档协议不可产品备案
        AuthApply apply = new AuthApply();
        apply.setApplyId("AP-PF-3");
        apply.setAssetId("AST-PF-3");
        apply.setAssetName("线损分析数据");
        apply.setGranteeOrg("南网数字产业公司");
        apply.setRightType("经营权");
        applyMapper.insert(apply);
        AuthAgreement ag = new AuthAgreement();
        ag.setAgreementNo("XY-PF-3");
        ag.setApplyId("AP-PF-3");
        ag.setGranteeOrg("南网数字产业公司");
        ag.setAgreementType("经营权");
        ag.setArchiveStatus(AuthAgreement.ARCHIVE_NO);
        agreementMapper.insert(ag);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> filingService.create(productFiling(ag.getAgreementId(), "线损分析数据")));
        assertTrue(ex.getMessage().contains("归档"), ex.getMessage());
    }

    @Test
    void auth_filing_type_defaults_and_page_filter() {
        // 存量口径:不带 filingType 的建档默认「授权备案」;分页可按类型过滤
        AuthFiling f = new AuthFiling();
        f.setFilingOrg("广东电网");
        f.setGranteeOrg("南网数字产业公司");
        f.setRightType("经营权");
        String id = filingService.create(f);
        assertNotNull(id);
        assertTrue(filingService.page(1, 50, null, AuthFiling.TYPE_AUTH).getTotal() >= 1, "授权备案(含存量空类型)可查");
        assertEquals(0, filingService.page(1, 50, "已备案", AuthFiling.TYPE_PRODUCT).getRecords().stream()
                .filter(x -> !AuthFiling.TYPE_PRODUCT.equals(x.getFilingType())).count(), "产品备案过滤不串类型");
    }
}
