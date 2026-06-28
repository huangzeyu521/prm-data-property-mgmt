package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AuthCertRenderVO;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCertTemplate;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.authorize.service.AuthCertTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 证书模板「出证按模板自动填充」契约锁(防回归)。
 *
 * 授权权益证书模板管理承诺"出证时按模板自动填充"。render(certId) 须把证书正文模板中的占位符
 * ({所属系统}/{数据表}/{使用场景及目的}/{权益类型}…)替换为该证书富化字段;否则模板形同摆设。
 * 本测试锁:有占位符模板 → render 后正文含实际值、无 {占位符} 残留。
 */
@SpringBootTest
@ActiveProfiles("test")
class CertTemplatePlaceholderFillTest {

    @Autowired private AuthApplyService applyService;
    @Autowired private AuthCertService certService;
    @Autowired private AuthCertTemplateService certTemplateService;

    @Test
    @DisplayName("render 把证书模板占位符替换为富化字段(所属系统/数据表/使用场景及目的/权益)")
    void render_fillsTemplatePlaceholders() {
        // 1) 生效证书模板(专项·使用权),正文含占位符
        AuthCertTemplate tpl = new AuthCertTemplate();
        tpl.setTemplateName("占位符填充测试模板-" + System.nanoTime());
        tpl.setCertType(AuthCertTemplate.TYPE_SPECIAL);
        tpl.setRightType("数据加工使用权");
        tpl.setTemplateContent("被授权方 {被授权方} 对 {所属系统}/{数据表} 享有 {权益类型};场景 {使用场景及目的};编号 {证书编号}。");
        certTemplateService.create(tpl);

        // 2) 专项授权申请 → 出证(matchTemplate 按 专项+使用权+生效 命中上面的模板)
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId("SYS:营销管理系统");
        a.setAssetName("用户用电信息表");
        a.setEquityCardId("EC-OK-1");
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        a.setScenario("综合能源服务");
        a.setScope("全字段");
        String applyId = applyService.saveDraft(a);
        String certId = certService.generateFromApply(applyService.getById(applyId));

        // 3) render → 占位符已替换为富化字段
        AuthCertRenderVO vo = certService.render(certId);
        assertNotNull(vo.getTemplateContent(), "应有证书正文");
        String body = vo.getTemplateContent();
        assertTrue(body.contains("营销管理系统"), "{所属系统} 应替换为 营销管理系统:" + body);
        assertTrue(body.contains("用户用电信息表"), "{数据表} 应替换为 库表名");
        assertTrue(body.contains("综合能源服务"), "{使用场景及目的} 应替换为 场景");
        assertTrue(body.contains("数据加工使用权"), "{权益类型} 应替换");
        assertFalse(body.contains("{所属系统}"), "不应残留占位符 {所属系统}");
        assertFalse(body.contains("{使用场景及目的}"), "不应残留占位符 {使用场景及目的}");
    }
}
