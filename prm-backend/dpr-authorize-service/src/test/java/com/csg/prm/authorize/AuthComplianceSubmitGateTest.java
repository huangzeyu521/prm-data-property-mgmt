package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AuthComplianceReport;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthComplianceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合规校验须覆盖 submit 硬门禁(先确后授isUsable/经营权目录/范围·期限⊆确权边界),
 * 杜绝"合规过了却提交被拒"的死路。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthComplianceSubmitGateTest {

    @Autowired private AuthComplianceService compliance;
    @Autowired private AuthApplyService applyService;

    private AuthApply base(String assetId, String cardId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId(assetId);
        a.setAssetName("门禁测试表");
        a.setEquityCardId(cardId);
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScope("全字段");
        return a;
    }

    /** 授权范围超确权边界(EC-NARROW=约定字段)→ 合规校验现在就拦(过去只在 submit 拦)。 */
    @Test
    void compliance_catches_scope_over_boundary() {
        String id = applyService.saveDraft(base("DA-CMP-1", "EC-NARROW-1"));
        AuthComplianceReport r = compliance.runCheck(id);
        assertEquals("不通过", r.getCheckResult());
        assertTrue(r.getItems().stream().anyMatch(i ->
                        i.getItem().contains("提交硬门禁") && !i.isPass() && i.getMessage().contains("授权范围超出确权边界")),
                "合规报告应含失败的提交硬门禁项(范围超边界):" + r.getProblemDesc());
    }

    /** 合规明细确含"提交硬门禁"项(与 submit 同源)。 */
    @Test
    void compliance_report_includes_submit_hard_gate_item() {
        String id = applyService.saveDraft(base("DA-CMP-2", "EC-OK-1"));
        AuthComplianceReport r = compliance.runCheck(id);
        assertTrue(r.getItems().stream().anyMatch(i -> i.getItem().contains("提交硬门禁")),
                "合规报告应包含提交硬门禁项");
    }
}
