package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCatalogItem;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.authorize.service.AuthCatalogService;
import com.csg.prm.authorize.service.AuthComplianceService;
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
 * F-03 补全能力测试:目录项(指引/场景/模板)、合规校验、协议全生命周期。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthExtrasTest {

    @Autowired private com.csg.prm.authorize.service.AuthMaterialService materialService;
    @Autowired private com.csg.prm.common.ai.DawatAiGateway dawatAi;
    @Autowired private AuthCatalogService catalogService;
    @Autowired private AuthComplianceService complianceService;
    @Autowired private AuthAgreementService agreementService;
    @Autowired private AuthApplyMapper applyMapper;

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
        // 规则化校验需申请存在:无卡片/无材料 → 应红/不通过
        AuthApply a = new AuthApply();
        a.setApplyId("AP-1");
        a.setAssetId("AST-001");
        a.setAssetName("客户用电信息表");
        a.setRightType("数据加工使用权");
        applyMapper.insert(a);

        var report = complianceService.runCheck("AP-1");
        assertNotNull(report);
        assertEquals("红", report.getRiskLevel());          // 未引用权益卡片 + 未传材料 → 不通过
        assertTrue(report.getItems().size() >= 6);          // 三维多项
        assertTrue(complianceService.page(1, 10, "AP-1", null).getTotal() >= 1);
    }

    @Test
    void agreement_double_sign_lifecycle() {
        String id = agreementService.generate("AP-2", "TPL-1", "广州供电局");
        assertEquals(AuthAgreement.SEAL_PENDING, agreementService.getById(id).getSealStatus());
        // 未双签不可审核
        assertThrows(BusinessException.class, () -> agreementService.review(id, true, null));
        // 甲方签 -> 待对方签
        agreementService.signByGrantor(id, "oss://agreement/AP-2-grantor.pdf");
        assertEquals(AuthAgreement.SEAL_PARTIAL, agreementService.getById(id).getSealStatus());
        assertThrows(BusinessException.class, () -> agreementService.review(id, true, null), "仅单方签署不可审核");
        // 乙方签 -> 已双签
        agreementService.signByGrantee(id, "oss://agreement/AP-2-grantee.pdf");
        assertEquals(AuthAgreement.SEAL_SIGNED, agreementService.getById(id).getSealStatus());
        assertTrue(agreementService.getById(id).getGrantorSigned());
        assertTrue(agreementService.getById(id).getGranteeSigned());
        // 双签后可审核 -> 归档
        agreementService.review(id, true, null);
        assertEquals(AuthAgreement.REVIEW_PASS, agreementService.getById(id).getReviewStatus());
        agreementService.archive(id);
        assertEquals(AuthAgreement.ARCHIVE_YES, agreementService.getById(id).getArchiveStatus());
    }

    @Test
    void auth_ai_material_check_reviews_each_material() {
        AuthApply a = new AuthApply();
        a.setAssetId("AST-AI-1");
        a.setAssetName("AI授权校验测试");
        a.setEquityCardId("EC-AI-1");
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        a.setScenario("电力金融征信");
        a.setSensitiveType("个人隐私");
        applyMapper.insert(a);

        com.csg.prm.authorize.entity.AuthMaterial sealed = new com.csg.prm.authorize.entity.AuthMaterial();
        sealed.setApplyId(a.getApplyId());
        sealed.setMaterialName("保密承诺函(附录E)");
        materialService.uploadFile(sealed, "保密承诺函-盖好.docx", docxBytes("承诺单位(盖章):广州供电局,严格保密"));

        com.csg.prm.authorize.entity.AuthMaterial unsealed = new com.csg.prm.authorize.entity.AuthMaterial();
        unsealed.setApplyId(a.getApplyId());
        unsealed.setMaterialName("表5 数据授权申请表");
        materialService.uploadFile(unsealed, "表5-申请表.docx", docxBytes("资产:AI授权校验测试,权益:数据加工使用权"));

        String json = materialService.aiCheck(a.getApplyId());
        assertTrue(json.contains("\"overall\""), "应输出整体结论");
        assertTrue(json.contains("保密承诺函"), "应逐份点名材料");
        assertTrue(json.contains("通过") && json.contains("存疑"), "盖章/未盖章应分别判定");
    }

    @Test
    void auth_pre_review_returns_opinion() {
        AuthApply a = new AuthApply();
        a.setAssetId("AST-AI-2");
        a.setAssetName("预审测试");
        a.setEquityCardId("EC-AI-2");
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据产品经营权");
        a.setScenario("电力金融征信");
        a.setSensitiveType("个人隐私");
        applyMapper.insert(a);

        String opinion = complianceService.preReview(a.getApplyId());
        assertNotNull(opinion);
        assertTrue(opinion.contains("先确后授"), "预审意见应含先确后授边界提醒");
        assertTrue(opinion.contains("开放目录"), "经营权应提醒对外开放目录");
    }

    @Test
    void batch_intent_parses_multiple_items() {
        String json = dawatAi.parseBatchIntent(
                "向南网综合能源股份有限公司授权台区负荷数据、充电桩运营数据、线损分析数据用于综合能源服务,数据加工使用权,批量授权");
        assertTrue(json.contains("台区负荷数据") && json.contains("充电桩运营数据") && json.contains("线损分析数据"),
                "应解析出 3 条明细:" + json);
        assertTrue(json.contains("数据加工使用权"), "应识别权益类型");
    }

    /** 最小合法 docx(纯 XML zip),供正文抽取 */
    private byte[] docxBytes(String text) {
        try {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(out)) {
                zip.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
                zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                        + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                        + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                        + "</Types>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
                zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                        + "<w:body><w:p><w:r><w:t>" + text + "</w:t></w:r></w:p></w:body></w:document>")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
