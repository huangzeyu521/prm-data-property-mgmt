package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AuthDashboardVO;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权看板(F-04)集成测试:走完一笔授权后,看板应统计到生效量、证书数与模式分布。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthDashboardTest {

    @Autowired
    private AuthApplyService applyService;
    @Autowired
    private AuthDashboardService dashboardService;

    @Test
    void dashboard_should_reflect_effective_authorization() {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId("DA-AUTHDASH-001");
        a.setAssetName("授权看板测试表");
        a.setEquityCardId("EC-PRA-DASH01");
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        String id = applyService.saveDraft(a);
        applyService.submit(id);
        // 专项五级审批:合规->业务->主管->经理->副总->已生效
        applyService.approve(id);
        applyService.approve(id);
        applyService.approve(id);
        applyService.approve(id);
        applyService.approve(id); // 生效 + 生成证书

        AuthDashboardVO vo = dashboardService.dashboard();
        assertTrue(vo.getTotalApply() >= 1);
        assertTrue(vo.getEffective() >= 1, "应统计到已生效授权");
        assertTrue(vo.getCertCount() >= 1, "应统计到授权证书");
        assertTrue(vo.getEffectiveRate() > 0);
        assertNotNull(vo.getModeDistribution());
        assertTrue(vo.getModeDistribution().containsKey(AuthApply.MODE_SPECIAL));
        assertTrue(vo.getRightTypeDistribution().containsKey("数据加工使用权"));
    }
}
