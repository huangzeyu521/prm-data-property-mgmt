package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权精化项测试:P3 授权范围/期限 ⊆ 确权边界强校验;P7 授权时效默认两年。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthBoundaryTest {

    @Autowired
    private AuthApplyService applyService;

    private AuthApply draft(String assetId, String cardId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId(assetId);
        a.setAssetName("边界测试表");
        a.setEquityCardId(cardId);
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScenario("内部分析");
        a.setScope("全字段");
        return a;
    }

    @Test
    void p3_scope_exceeding_confirm_boundary_is_rejected() {
        // 卡片号含 NARROW -> 确权范围"约定字段";授权申请"全字段"超出 -> 拦截
        AuthApply a = draft("DA-BND-1", "EC-NARROW-1");
        String id = applyService.saveDraft(a);
        BusinessException ex = assertThrows(BusinessException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("授权范围超出确权边界"));
    }

    @Test
    void p3_validity_exceeding_confirm_validity_is_rejected() {
        // 卡片号含 SHORT -> 确权有效期仅 10 天;授权期限设为 1 年超出 -> 拦截
        AuthApply a = draft("DA-BND-2", "EC-SHORT-1");
        a.setValidDate(LocalDateTime.now().plusYears(1));
        String id = applyService.saveDraft(a);
        BusinessException ex = assertThrows(BusinessException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("授权期限超出确权有效期"));
    }

    @Test
    void p7_validity_defaults_to_two_years_when_blank() {
        AuthApply a = draft("DA-BND-3", "EC-OK-1");
        String id = applyService.saveDraft(a);
        applyService.submit(id);
        LocalDateTime vd = applyService.getById(id).getValidDate();
        assertNotNull(vd, "提交后应自动设置默认授权期限");
        assertTrue(vd.isAfter(LocalDateTime.now().plusMonths(23))
                && vd.isBefore(LocalDateTime.now().plusMonths(25)), "默认应约为两年");
    }
}
