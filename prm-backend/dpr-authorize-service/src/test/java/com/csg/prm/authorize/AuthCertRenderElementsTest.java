package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AuthCertRenderVO;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthCertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 授权权益证书渲染视图 表5/表6/§3.4.4 join 契约锁(防回归)。
 *
 * 授权权益管理页证书预览渲染 render(certId):证书(AuthCert)只存 assetId(无 库表名/模式/场景),
 * 须按 applyId join 申请单带出 数据表(assetName)/模式(schemaName)/使用场景(scenario),并由 assetId 派生所属系统。
 * 任一 join 字段丢失 → 证书预览只剩 raw assetId、丢库表名/场景。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthCertRenderElementsTest {

    @Autowired private AuthApplyService applyService;
    @Autowired private AuthCertService certService;

    @Test
    @DisplayName("render(certId) 由 assetId 派生所属系统,并 join 申请单 数据表/模式/使用场景")
    void render_joinsApplyElementsAndDerivesSystem() {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId("SYS:营销管理系统");
        a.setAssetName("用户用电信息表");
        a.setEquityCardId("EC-OK-1");
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        a.setScenario("综合能源服务");
        a.setScope("全字段");
        a.setSchemaName("BILLING");
        String applyId = applyService.saveDraft(a);

        // 出证(从已落库申请单,applyId 已回填)→ 证书
        String certId = certService.generateFromApply(applyService.getById(applyId));
        AuthCertRenderVO vo = certService.render(certId);

        assertNotNull(vo, "应返回证书渲染视图");
        assertEquals("营销管理系统", vo.getSysName(), "所属系统应由 assetId 去 SYS: 前缀派生(凭证不暴露 raw assetId)");
        assertEquals("用户用电信息表", vo.getAssetName(), "数据表(库表名)应 join 申请单带出");
        assertEquals("BILLING", vo.getSchemaName(), "模式名称应 join 申请单带出");
        assertEquals("综合能源服务", vo.getScenario(), "§3.4.4 使用场景及目的应 join 申请单带出");
    }
}
