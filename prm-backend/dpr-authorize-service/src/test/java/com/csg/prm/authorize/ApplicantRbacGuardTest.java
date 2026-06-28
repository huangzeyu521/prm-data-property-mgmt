package com.csg.prm.authorize;

import com.csg.prm.authorize.controller.AuthAgreementController;
import com.csg.prm.authorize.controller.AuthApplyController;
import com.csg.prm.authorize.controller.BatchAuthListController;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.common.config.RbacInterceptor;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三权分置/职责分离守卫:申报人(apply)不得自批自审。
 * 这些审批/审核端点的 @RequiresRole 必须存在且不含 "apply"(否则申报人可审批自己提交的申请)。
 * 反射断言锁定注解,防止回退(历史上批量端点漏注解,单条已注解)。
 */
class ApplicantRbacGuardTest {

    private void assertGuardedExcludingApply(Class<?> ctrl, String method, Class<?>... params) throws NoSuchMethodException {
        Method m = ctrl.getMethod(method, params);
        RequiresRole rr = m.getAnnotation(RequiresRole.class);
        assertNotNull(rr, ctrl.getSimpleName() + "." + method + " 必须标注 @RequiresRole(审批/审核动作)");
        assertFalse(Arrays.asList(rr.value()).contains("apply"),
                ctrl.getSimpleName() + "." + method + " 不得允许 apply 角色(职责分离)");
    }

    @Test
    void authApply_batchApproveReject_blockApplicant() throws Exception {
        assertGuardedExcludingApply(AuthApplyController.class, "batchApprove", java.util.List.class);
        assertGuardedExcludingApply(AuthApplyController.class, "batchReject", java.util.List.class, String.class);
        // 单条对照:本就已守卫
        assertGuardedExcludingApply(AuthApplyController.class, "approve", String.class, String.class);
        assertGuardedExcludingApply(AuthApplyController.class, "reject", String.class, String.class);
    }

    @Test
    void batchList_approve_blockApplicant() throws Exception {
        assertGuardedExcludingApply(BatchAuthListController.class, "approve", String.class);
    }

    @Test
    void agreement_review_blockApplicant() throws Exception {
        assertGuardedExcludingApply(AuthAgreementController.class, "review", String.class, boolean.class, String.class);
    }

    /**
     * 运行时强制验证(prm.auth.enabled=true):RbacInterceptor + 真实注解协同,
     * apply 调用 batchApprove 抛 FORBIDDEN;review 放行。证明守卫不止存在、且真正拦截。
     */
    @Test
    void enforcement_applyBlocked_reviewAllowed_onBatchApprove() throws Exception {
        Method m = AuthApplyController.class.getMethod("batchApprove", java.util.List.class);
        HandlerMethod hm = new HandlerMethod(new AuthApplyController(null, null), m);
        RbacInterceptor it = new RbacInterceptor(true);
        try {
            UserContextHolder.set(ctxWithRoles("apply"));
            BusinessException ex = assertThrows(BusinessException.class, () -> it.preHandle(null, null, hm),
                    "申报人调用批量审批应被拦截");
            assertEquals(ResponseCode.FORBIDDEN.getCode(), ex.getCode(), "应为 403 无权限");

            UserContextHolder.set(ctxWithRoles("review"));
            assertTrue(it.preHandle(null, null, hm), "审核角色应放行批量审批");
        } finally {
            UserContextHolder.clear();
        }
    }

    private UserContext ctxWithRoles(String... roles) {
        UserContext c = new UserContext();
        c.setRoles(Set.of(roles));
        return c;
    }
}
