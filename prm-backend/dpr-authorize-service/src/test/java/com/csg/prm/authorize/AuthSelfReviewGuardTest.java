package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 职责分离(SoD)自审门禁:一事一议审批链上,审批/驳回人不得是本单发起人(creatorId)。
 * 场景:业务管理部门团队(business)既发起一事一议、又占「业务审核」节点——同一 business 账号
 * 不得审自己发起的单。无用户上下文/admin/all 放行(内部/单测/超级视角)。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthSelfReviewGuardTest {

    @Autowired
    private AuthApplyService applyService;

    private UserContext ctx(String userId, String... roles) {
        UserContext c = new UserContext();
        c.setUserId(userId);
        c.setRoles(Set.of(roles));
        return c;
    }

    /**
     * 推进到「业务审核中」节点。整段用 admin 上下文:admin 放行 assertApplicant(submit)+逐节点门禁+自审门禁,
     * 便于把单据推到目标节点;发起人 creatorId 预置为入参(strictInsertFill 仅填 null,预置得以保留)。
     */
    private String seedAtBusinessNode(String assetId, String creatorId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId(assetId);
        a.setAssetName("自审门禁测试表");
        a.setEquityCardId("EC-PRA-VALID01");
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScenario("电力金融征信");
        a.setScope("全字段");
        a.setCreatorId(creatorId);
        try {
            UserContextHolder.set(ctx("USR-ADMIN", "admin"));
            String id = applyService.saveDraft(a);
            applyService.submit(id);                 // -> 单位初审中
            assertEquals(AuthApply.STATUS_UNIT, applyService.getById(id).getStatus());
            applyService.approve(id, null);          // 单位初审 -> 合规
            applyService.approve(id, null);          // 合规 -> 业务
            assertEquals(AuthApply.STATUS_BUSINESS, applyService.getById(id).getStatus());
            return id;
        } finally {
            UserContextHolder.clear();
        }
    }

    @Test
    void business_cannot_approve_own_special_application_but_other_business_can() {
        String id = seedAtBusinessNode("DA-SOD-001", "USR-BIZ-A");
        try {
            // 发起人本人(business)审自己的单 -> 职责分离拦截,状态不变
            UserContextHolder.set(ctx("USR-BIZ-A", "business"));
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> applyService.approve(id, null), "business 不得审批本人发起的一事一议单");
            assertEquals(ResponseCode.FORBIDDEN.getCode(), ex.getCode(), "应为 403 职责分离");
            assertEquals(AuthApply.STATUS_BUSINESS, applyService.getById(id).getStatus(), "被拦后状态不应推进");

            // 另一个 business 账号(非发起人)可正常业务审核 -> 推进到主管审核中
            UserContextHolder.set(ctx("USR-BIZ-B", "business"));
            assertNull(applyService.approve(id, null));
            assertEquals(AuthApply.STATUS_MANAGER, applyService.getById(id).getStatus(), "他人审核应正常推进");
        } finally {
            UserContextHolder.clear();
        }
    }

    @Test
    void applicant_cannot_reject_own_application() {
        String id = seedAtBusinessNode("DA-SOD-002", "USR-BIZ-A");
        try {
            // 发起人本人在业务审核节点驳回自己的单 -> 同样被职责分离拦截
            UserContextHolder.set(ctx("USR-BIZ-A", "business"));
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> applyService.reject(id, "自审驳回应被拦"), "不得驳回本人发起的单");
            assertEquals(ResponseCode.FORBIDDEN.getCode(), ex.getCode());
            assertEquals(AuthApply.STATUS_BUSINESS, applyService.getById(id).getStatus());
        } finally {
            UserContextHolder.clear();
        }
    }

    @Test
    void no_context_or_admin_bypasses_self_review_guard() {
        String id = seedAtBusinessNode("DA-SOD-003", "USR-BIZ-A");
        // 无上下文:内部/单测放行(既有全流程测试即依赖此放行)
        assertNull(applyService.approve(id, null));
        assertEquals(AuthApply.STATUS_MANAGER, applyService.getById(id).getStatus());
    }
}
