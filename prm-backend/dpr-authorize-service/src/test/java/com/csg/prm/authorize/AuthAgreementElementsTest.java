package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 授权申请「表5/表6 模式名称」+「授权协议要素(附录D §3.4.4 利益分配/安全保障)」字段落库往返。
 * 工作指引办数字〔2025〕35号:授权协议须约定 数据范围/使用场景/授权目的/利益分配/安全保障。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthAgreementElementsTest {

    @Autowired
    private AuthApplyService applyService;

    private AuthApply draft(String mode, String assetId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(mode);
        a.setAssetId(assetId);
        a.setAssetName("协议要素测试表");
        a.setEquityCardId("EC-PRA-VALID01");
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        a.setScenario("电力金融征信");
        a.setScope("全字段");
        return a;
    }

    @Test
    @DisplayName("一事一议:模式名称 + 利益分配 + 安全保障 落库并原样取回")
    void special_apply_persists_schema_and_agreement_elements() {
        AuthApply a = draft(AuthApply.MODE_SPECIAL, "DA-AGR-SP-" + System.nanoTime());
        a.setSchemaName("MKT");
        a.setBenefitAllocation("按调用次数计费,收益 7:3 分成");
        a.setSecurityReq("加密传输 + 最小授权访问控制 + 操作留痕审计");
        String id = applyService.saveDraft(a);

        AuthApply got = applyService.getById(id);
        assertEquals("MKT", got.getSchemaName());
        assertEquals("按调用次数计费,收益 7:3 分成", got.getBenefitAllocation());
        assertEquals("加密传输 + 最小授权访问控制 + 操作留痕审计", got.getSecurityReq());
    }

    @Test
    @DisplayName("批量项:表6 模式名称随授权项落库,逐条可追溯")
    void batch_item_persists_schema_name() {
        AuthApply a = draft(AuthApply.MODE_BATCH, "DA-AGR-BA-" + System.nanoTime());
        a.setSchemaName("PROD");
        String id = applyService.saveDraft(a);

        assertEquals("PROD", applyService.getById(id).getSchemaName());
    }
}
