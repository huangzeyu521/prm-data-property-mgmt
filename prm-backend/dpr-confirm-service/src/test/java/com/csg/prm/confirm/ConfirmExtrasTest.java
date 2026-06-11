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
