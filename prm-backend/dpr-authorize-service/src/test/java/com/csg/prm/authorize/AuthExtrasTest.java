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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        a.setRightType("使用权");
        applyMapper.insert(a);

        var report = complianceService.runCheck("AP-1");
        assertNotNull(report);
        assertEquals("红", report.getRiskLevel());          // 未引用权益卡片 + 未传材料 → 不通过
        assertTrue(report.getItems().size() >= 6);          // 三维多项
        assertTrue(complianceService.page(1, 10, "AP-1", null).getTotal() >= 1);
    }

    /** 要素落定(补齐违约金/送达信息)并生成正式稿——签章门禁的前置动作。 */
    private void finalizeWithDefaults(String agreementId) {
        agreementService.saveNegotiation(agreementId, new com.csg.prm.authorize.dto.AgreementNegotiationVO(
                null, null, null, null, null, null, null, null, null,
                "10", null, "邮箱:test@csg.cn", null, null, null, null, null));
        agreementService.finalizeDoc(agreementId);
    }

    @Test
    void agreement_double_sign_autoFinalize_lifecycle() {
        // 协议环节无独立人工审核节点(35号文):要素落定→双签→承诺函(附录E)齐,系统自动核验通过+归档。
        String id = agreementService.generate("AP-2", "TPL-1", "广州供电局");
        AuthAgreement g0 = agreementService.getById(id);
        assertEquals(AuthAgreement.SEAL_PENDING, g0.getSealStatus());
        assertEquals(AuthAgreement.REVIEW_PENDING, g0.getReviewStatus());   // 未核验
        assertEquals(AuthAgreement.ARCHIVE_NO, g0.getArchiveStatus());       // 未归档
        assertEquals(AuthAgreement.DOC_DRAFT, g0.getDocStatus(), "生成即草案(要素预填,待落定)");
        // 草案不可签章(要素落定门禁)
        assertThrows(BusinessException.class,
                () -> agreementService.signByGrantor(id, "oss://x.pdf"), "草案不可签章");
        finalizeWithDefaults(id);
        assertEquals(AuthAgreement.DOC_FINAL, agreementService.getById(id).getDocStatus());
        // 甲方签 -> 待对方签(尚未触发自动收尾)
        agreementService.signByGrantor(id, "oss://agreement/AP-2-grantor.pdf");
        AuthAgreement g1 = agreementService.getById(id);
        assertEquals(AuthAgreement.SEAL_PARTIAL, g1.getSealStatus());
        assertEquals(AuthAgreement.REVIEW_PENDING, g1.getReviewStatus());    // 单签不收尾
        assertEquals(AuthAgreement.ARCHIVE_NO, g1.getArchiveStatus());
        // 乙方签 -> 已双签,但《保密承诺函》(附录E)未收口 -> 不归档不开权限
        agreementService.signByGrantee(id, "oss://agreement/AP-2-grantee.pdf");
        AuthAgreement g2 = agreementService.getById(id);
        assertEquals(AuthAgreement.SEAL_SIGNED, g2.getSealStatus());
        assertTrue(g2.getGrantorSigned());
        assertTrue(g2.getGranteeSigned());
        assertEquals(AuthAgreement.REVIEW_PENDING, g2.getReviewStatus(), "双签但缺承诺函,不得自动核验");
        assertEquals(AuthAgreement.ARCHIVE_NO, g2.getArchiveStatus(), "双签但缺承诺函,不得归档");
        // 承诺函收口 -> 三条件齐 -> 系统自动核验通过 + 自动归档
        agreementService.uploadConfidentiality(id, "保密承诺函.pdf", new byte[128]);
        AuthAgreement g3 = agreementService.getById(id);
        assertEquals(AuthAgreement.REVIEW_PASS, g3.getReviewStatus(), "双签+承诺函齐后系统自动核验通过");
        assertEquals(AuthAgreement.ARCHIVE_YES, g3.getArchiveStatus(), "双签+承诺函齐后系统自动归档");
        // 自动写入系统核验记录
        assertTrue(agreementService.listReviewLogs(id).stream()
                .anyMatch(l -> "系统自动核验".equals(l.getReviewer())), "应有系统自动核验记录");
    }

    @Test
    void appendixD_doc_renders_full_template() {
        // 协议草案按附录D《南方电网数据授权运营协议》全量生成:可变要素填充 + 11章法律条款 + 附件1清单。
        AuthApply a = new AuthApply();
        a.setApplyId("AP-APD-1");
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId("AST-9001");          // 非 SYS: 前缀的内部资产ID,不得外泄到协议
        a.setAssetName("客户用电信息表");
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScenario("电力金融征信");
        a.setScope("全字段");
        a.setBusinessDomain("营销域");      // 系统名称回退展示业务域
        a.setSchemaName("BILLING");
        a.setStatus(AuthApply.STATUS_EFFECTIVE);
        applyMapper.insert(a);
        String id = agreementService.generate("AP-APD-1", null, "广州供电局");
        String html = new String(agreementService.appendixDDoc(id), java.nio.charset.StandardCharsets.UTF_8);
        // 系统名称不得外泄内部资产ID,回退展示业务域
        assertFalse(html.contains("AST-9001"), "附件1 系统名称不得外泄内部资产ID(AST-xxx)");
        assertTrue(html.contains("营销域"), "系统名称回退展示业务域");
        assertTrue(html.contains("南方电网数据授权运营协议"), "标题");
        assertTrue(html.contains("广州供电局"), "乙方=被授权方");
        assertTrue(html.contains("一、授权数据目的、范围及内容"), "第一章");
        assertTrue(html.contains("二、授权时空范围"), "第二章·时空范围");
        assertTrue(html.contains("四、甲方的主要权利义务"), "甲方义务条款");
        assertTrue(html.contains("五、乙方的主要权利义务"), "乙方义务条款");
        assertTrue(html.contains("十、争议解决"), "争议解决条款");
        assertTrue(html.contains("外部许可条件"), "外部许可8条件");
        assertTrue(html.contains("附件1:数据授权清单"), "附件1清单");
        assertTrue(html.contains("客户用电信息表"), "附件1带出来源申请单的数据表");
        assertTrue(html.contains("本协议经双方法定代表人或授权代表签字、单位盖章后生效"), "生效条件原文");
    }

    @Test
    void uploadSeal_double_sign_autoFinalize() {
        // 前端真实路径=uploadSeal(上传签章件)。要素落定→双方上传签章件→承诺函先行已收口 -> 双签即自动核验+归档。
        String id = agreementService.generate("AP-9", "TPL-1", "深圳供电局");
        byte[] valid = new byte[128]; // ≥64 字节视为签章清晰可核验
        // 要素未落定(草案)不可上传签章
        assertThrows(BusinessException.class,
                () -> agreementService.uploadSeal(id, "授权方", "grantor.pdf", valid), "草案不可签章");
        finalizeWithDefaults(id);
        // 承诺函先行收口(顺序无关:三条件齐即收尾)
        agreementService.uploadConfidentiality(id, "保密承诺函.pdf", valid);
        agreementService.uploadSeal(id, "授权方", "grantor.pdf", valid);
        AuthAgreement p = agreementService.getById(id);
        assertEquals(AuthAgreement.SEAL_PARTIAL, p.getSealStatus());
        assertEquals(AuthAgreement.REVIEW_PENDING, p.getReviewStatus(), "单方上传不收尾");
        agreementService.uploadSeal(id, "被授权方", "grantee.pdf", valid);
        AuthAgreement f = agreementService.getById(id);
        assertEquals(AuthAgreement.SEAL_SIGNED, f.getSealStatus());
        assertEquals(AuthAgreement.REVIEW_PASS, f.getReviewStatus(), "双签+承诺函齐后系统自动核验通过");
        assertEquals(AuthAgreement.ARCHIVE_YES, f.getArchiveStatus(), "双签后系统自动归档");
    }

    @Test
    void auth_ai_material_check_reviews_each_material() {
        AuthApply a = new AuthApply();
        a.setAssetId("AST-AI-1");
        a.setAssetName("AI授权校验测试");
        a.setEquityCardId("EC-AI-1");
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
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
        materialService.uploadFile(unsealed, "表5-申请表.docx", docxBytes("资产:AI授权校验测试,权益:使用权"));

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
        a.setRightType("经营权");
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
                "向南网综合能源股份有限公司授权台区负荷数据、充电桩运营数据、线损分析数据用于综合能源服务,使用权,批量授权");
        assertTrue(json.contains("台区负荷数据") && json.contains("充电桩运营数据") && json.contains("线损分析数据"),
                "应解析出 3 条明细:" + json);
        assertTrue(json.contains("使用权"), "应识别权益类型");
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
