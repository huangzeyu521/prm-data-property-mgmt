package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AuthComplianceReport;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthComplianceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合规校验第④维「授权协议要素(附录D §3.4.4)」契约锁(防回归)。
 * 指引 §3.4.4:授权协议须约定 数据范围/使用场景/授权目的/利益分配/安全保障。
 * 一事一议逐单录入 利益分配/安全保障(缺→警告);批量在《运营授权协议》(清单级)统一约定(标通过)。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthComplianceAgreementElementsTest {

    private static final String DIM = "授权协议要素";

    @Autowired private AuthComplianceService compliance;
    @Autowired private AuthApplyService applyService;

    private AuthApply base(String mode, String assetId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(mode);
        a.setAssetId(assetId);
        a.setAssetName("协议要素合规测试表");
        a.setEquityCardId("EC-OK-1");
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScenario("综合能源服务");
        a.setScope("全字段");
        return a;
    }

    private AuthComplianceReport.Item item(AuthComplianceReport r, String name) {
        return r.getItems().stream()
                .filter(i -> DIM.equals(i.getDimension()) && name.equals(i.getItem()))
                .findFirst().orElse(null);
    }

    @Test
    @DisplayName("一事一议缺 利益分配/安全保障 → 协议要素维各出一条不符(场景已填则通过)")
    void special_missing_agreement_elements_flagged() {
        String id = applyService.saveDraft(base(AuthApply.MODE_SPECIAL, "DA-CMP-AGR-1-" + System.nanoTime()));
        AuthComplianceReport r = compliance.runCheck(id);

        assertNotNull(item(r, "使用场景及目的"), "应含『使用场景及目的』项");
        assertTrue(item(r, "使用场景及目的").isPass(), "场景已填应通过");
        assertNotNull(item(r, "利益分配约定"), "应含『利益分配约定』项");
        assertFalse(item(r, "利益分配约定").isPass(), "一事一议未填利益分配应不符(警告)");
        assertFalse(item(r, "安全保障要求").isPass(), "一事一议未填安全保障应不符(警告)");
    }

    @Test
    @DisplayName("一事一议补齐 利益分配/安全保障 → 协议要素维全通过")
    void special_with_agreement_elements_passes() {
        AuthApply a = base(AuthApply.MODE_SPECIAL, "DA-CMP-AGR-2-" + System.nanoTime());
        a.setBenefitAllocation("按调用次数计费,收益 7:3 分成");
        a.setSecurityReq("加密传输 + 最小授权访问控制 + 操作留痕审计");
        String id = applyService.saveDraft(a);
        AuthComplianceReport r = compliance.runCheck(id);

        assertTrue(item(r, "利益分配约定").isPass(), "已约定利益分配应通过");
        assertTrue(item(r, "安全保障要求").isPass(), "已约定安全保障应通过");
    }

    @Test
    @DisplayName("批量授权 → 利益分配/安全保障标『清单级协议统一约定』通过,不逐项误报")
    void batch_agreement_elements_deferred_to_list_protocol() {
        String id = applyService.saveDraft(base(AuthApply.MODE_BATCH, "DA-CMP-AGR-3-" + System.nanoTime()));
        AuthComplianceReport r = compliance.runCheck(id);

        AuthComplianceReport.Item benefit = item(r, "利益分配约定");
        assertNotNull(benefit, "批量也应含『利益分配约定』项");
        assertTrue(benefit.isPass(), "批量应标通过(不逐项要求)");
        assertTrue(benefit.getMessage().contains("清单级"), "批量应说明在清单级协议统一约定:" + benefit.getMessage());
        assertTrue(item(r, "安全保障要求").isPass(), "批量安全保障应标通过");
    }
}
