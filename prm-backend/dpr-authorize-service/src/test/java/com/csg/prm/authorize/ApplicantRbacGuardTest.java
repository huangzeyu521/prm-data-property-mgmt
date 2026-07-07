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

    /**
     * 清单级终批(申报稿→批准)= BA-03 node90「领导小组办公室」专属:仅 leadership/admin。
     * 数字化部主管/经理/副总在明细 AuthApply 链逐项审核(不在清单级终批),故清单 approve 不得放行 manager/director/gm/review。
     */
    @Test
    void batchList_approve_isLeadershipExclusive() throws Exception {
        RequiresRole rr = BatchAuthListController.class.getMethod("approve", String.class).getAnnotation(RequiresRole.class);
        assertNotNull(rr, "batch-list approve 须标注 @RequiresRole");
        var roles = Arrays.asList(rr.value());
        assertTrue(roles.contains("leadership"), "清单终批须允许领导小组办公室");
        for (String notAllowed : new String[]{"apply", "manager", "director", "gm", "review", "business", "precheck"}) {
            assertFalse(roles.contains(notAllowed),
                    "清单级终批是领导小组专属,不得放行「" + notAllowed + "」(其审核在明细链完成)");
        }
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
        HandlerMethod hm = new HandlerMethod(new AuthApplyController(null, null, null), m);
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

    /**
     * 授权审批 approve/reject 的粗粒度 @RequiresRole 必须放行所有节点审批角色(含 gm 副总/总经理),
     * 否则该角色在到达服务层 assertNodeRole(逐节点门禁)之前就被控制器拦死——多级审批名存实亡。
     * (历史 bug:授权侧曾为 {review,admin},gm/business/manager/director/leadership 全被挡,仅 admin 能审。)
     */
    @Test
    void approveReject_admitAllNodeApprovers_inclGm_excludeApply() throws Exception {
        String[] nodeRoles = {"review", "business", "manager", "director", "gm", "leadership"};
        for (String mn : new String[]{"approve", "reject"}) {
            RequiresRole rr = AuthApplyController.class.getMethod(mn, String.class, String.class).getAnnotation(RequiresRole.class);
            assertNotNull(rr, mn + " 须标注 @RequiresRole");
            var roles = Arrays.asList(rr.value());
            for (String need : nodeRoles) {
                assertTrue(roles.contains(need),
                        mn + " 粗粒度门禁须放行节点角色「" + need + "」(再由 assertNodeRole 校验具体节点)");
            }
            assertFalse(roles.contains("apply"), mn + " 仍不得放行申报人(职责分离)");
        }
    }

    /** 运行时:gm(副总/总经理)能进入授权审批控制器;申报人仍被拦。节点匹配另由 AuthNodeRoleTest 覆盖。 */
    @Test
    void enforcement_gmReachesApprove_applyStillBlocked() throws Exception {
        Method m = AuthApplyController.class.getMethod("approve", String.class, String.class);
        HandlerMethod hm = new HandlerMethod(new AuthApplyController(null, null, null), m);
        RbacInterceptor it = new RbacInterceptor(true);
        try {
            UserContextHolder.set(ctxWithRoles("gm"));
            assertTrue(it.preHandle(null, null, hm), "gm 副总/总经理应能进入授权审批控制器");
            UserContextHolder.set(ctxWithRoles("apply"));
            assertThrows(BusinessException.class, () -> it.preHandle(null, null, hm), "申报人仍被拦");
        } finally {
            UserContextHolder.clear();
        }
    }
}
