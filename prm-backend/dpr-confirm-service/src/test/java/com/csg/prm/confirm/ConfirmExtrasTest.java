package com.csg.prm.confirm;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmGuidance;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import com.csg.prm.confirm.entity.EquityCertTemplate;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmGuidanceService;
import com.csg.prm.confirm.service.ConfirmMaterialService;
import com.csg.prm.confirm.service.EquityCertService;
import com.csg.prm.confirm.service.EquityCertTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F-02 补全能力测试:确权指引、材料上传与校验、权益证书签发、证书模板生命周期。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmExtrasTest {

    @Autowired private ConfirmApplyService applyService;
    @Autowired private ConfirmGuidanceService guidanceService;
    @Autowired private ConfirmMaterialService materialService;
    @Autowired private EquityCertService certService;
    @Autowired private EquityCertTemplateService templateService;

    @Test
    void apply_supports_multi_right_type_and_one_case_one_meeting_mode() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-EXT-MODE");
        a.setAssetName("多类型一事一议");
        a.setRightType("数据资源持有权、数据加工使用权");
        a.setRightHolder("中国南方电网有限责任公司");
        a.setApplyMode("一事一议");
        String id = applyService.saveDraft(a);

        ConfirmApply saved = applyService.getById(id);
        assertEquals("一事一议", saved.getApplyMode(), "申请模式应持久化");
        assertTrue(saved.getRightType().contains("、"), "多权属类型应合并保存");
    }

    @Test
    void ai_material_check_reviews_each_material() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-AICHK-1");
        a.setAssetName("AI校验测试集");
        a.setRightType("数据资源持有权");
        a.setRightHolder("广东电网有限责任公司");
        String applyId = applyService.saveDraft(a);

        ConfirmMaterial sealed = new ConfirmMaterial();
        sealed.setApplyId(applyId);
        sealed.setMaterialName("数据确权证明材料(权属/来源凭证)");
        sealed.setMaterialType("证明材料");
        materialService.uploadFile(sealed, "证明-盖好.docx",
                docxBytes("数据持有权,广东电网有限责任公司,有效期3年,自行生产,已盖章"));

        ConfirmMaterial unsealed = new ConfirmMaterial();
        unsealed.setApplyId(applyId);
        unsealed.setMaterialName("《表1 数据确权信息清单(系统级)》");
        unsealed.setMaterialType("表1");
        materialService.uploadFile(unsealed, "表1-清单.docx", docxBytes("资产:AI校验测试集,权属类型:数据资源持有权"));

        String json = materialService.aiCheck(applyId);
        assertTrue(json.contains("\"overall\""), "应输出整体结论");
        assertTrue(json.contains("数据确权证明材料"), "应逐份点名材料");
        assertTrue(json.contains("通过"), "含盖章材料应判通过");
        assertTrue(json.contains("存疑"), "无盖章表述材料应判存疑");
        assertTrue(json.contains("suggestion"), "应给出补正建议");
    }

    /** 生成含指定中文正文的最小合法 docx(纯 XML zip,无字体依赖,供正文抽取) */
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
                zip.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
                zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                        + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                        + "</Relationships>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                zip.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
                zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                        + "<w:body><w:p><w:r><w:t>" + text + "</w:t></w:r></w:p></w:body></w:document>")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("测试 docx 生成失败", e);
        }
    }

    @Test
    void guidance_save_and_page() {
        ConfirmGuidance g = new ConfirmGuidance();
        g.setTitle("数据确权操作指引");
        g.setGuidanceType("操作说明");
        String id = guidanceService.save(g);
        assertNotNull(id);
        assertEquals("v1", guidanceService.getById(id).getVersion());
        assertTrue(guidanceService.page(1, 10, "确权", null, false).getTotal() >= 1);
    }

    @Test
    void material_upload_then_check() {
        ConfirmMaterial m = new ConfirmMaterial();
        m.setApplyId("APP-1");
        m.setMaterialName("采购协议.pdf");
        m.setMaterialType("采购协议");
        String id = materialService.upload(m);
        assertEquals(ConfirmMaterial.CHECK_PENDING, materialService.listByApply("APP-1").get(0).getCheckResult());

        materialService.check(id, true, null);
        assertEquals(ConfirmMaterial.CHECK_PASS, materialService.listByApply("APP-1").get(0).getCheckResult());
        assertTrue(materialService.page(1, 10, "APP-1", null).getTotal() >= 1);
    }

    @Test
    void cert_issue_and_revoke() {
        String certId = certService.issue("CARD-1", "中国南方电网有限责任公司", null, "持有权证书模板");
        assertNotNull(certId);
        assertEquals("生效", certService.getById(certId).getCertStatus());
        certService.revoke(certId);
        assertEquals("已注销", certService.getById(certId).getCertStatus());
    }

    @Test
    void template_lifecycle() {
        EquityCertTemplate t = new EquityCertTemplate();
        t.setTemplateName("数据资源持有权证书");
        t.setRightType("数据资源持有权");
        String id = templateService.create(t);
        assertTrue(templateService.page(1, 10, "持有权", "生效中").getTotal() >= 1);
        templateService.disable(id);
        assertTrue(templateService.page(1, 10, null, "停用").getTotal() >= 1);
    }
}
