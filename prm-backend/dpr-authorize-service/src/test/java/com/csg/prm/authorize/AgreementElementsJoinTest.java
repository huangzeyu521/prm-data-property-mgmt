package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AgreementElementsVO;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.authorize.service.AuthApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 协议要素核对(附录D §3.4.4)join 契约锁(防回归)。
 *
 * 协议工作台「②协议审核」核对"协议内容与申请单一致":协议(AuthAgreement)本身不含 §3.4.4 要素,
 * 须按 applyId join 来源授权申请单带出 数据范围/场景/利益分配/安全保障 等。本测试锁该 join 正确。
 */
@SpringBootTest
@ActiveProfiles("test")
class AgreementElementsJoinTest {

    @Autowired private AuthApplyService applyService;
    @Autowired private AuthAgreementService agreementService;

    @Test
    @DisplayName("elements(agreementId) 按 applyId join 出申请单的 §3.4.4 + 表5/表6 要素")
    void elements_joinApplyAgreementElements() {
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
        a.setBusinessDomain("营销域");
        a.setBenefitAllocation("按调用次数计费,收益 7:3 分成");
        a.setSecurityReq("加密传输 + 最小授权访问控制 + 操作留痕审计");
        String applyId = applyService.saveDraft(a);

        String agreementId = agreementService.generate(applyId, null, "南网综合能源股份有限公司");
        AgreementElementsVO vo = agreementService.elements(agreementId);

        assertNotNull(vo, "应返回协议要素核对视图");
        assertEquals(applyId, vo.applyId(), "应回链来源申请单");
        assertEquals("营销管理系统", vo.sysName(), "所属系统应由 assetId 去 SYS: 前缀派生");
        assertEquals("用户用电信息表", vo.dataTable(), "数据表=库表名");
        assertEquals("BILLING", vo.schemaName(), "模式名称 join 带出");
        assertEquals("数据加工使用权", vo.rightType(), "权益类型 join 带出");
        assertEquals("综合能源服务", vo.scenario(), "§3.4.4 使用场景及目的 join 带出");
        assertEquals("全字段", vo.scope(), "§3.4.4 数据范围 join 带出");
        assertEquals("按调用次数计费,收益 7:3 分成", vo.benefitAllocation(), "§3.4.4 利益分配 join 带出");
        assertEquals("加密传输 + 最小授权访问控制 + 操作留痕审计", vo.securityReq(), "§3.4.4 安全保障 join 带出");
    }
}
