package com.csg.prm.confirm;

import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 逐节点角色门禁:每个审批节点仅对应角色(及 all/admin)可审批/驳回。
 * 无上下文(内部/单测)放行——故其余流程测试不受影响。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmNodeRoleTest {

    @Autowired
    private ConfirmApplyService applyService;

    @AfterEach
    void clear() {
        UserContextHolder.clear();
    }

    private void actAs(String role) {
        UserContext ctx = new UserContext();
        ctx.setUserId("u-" + role);
        ctx.setRoles(role == null ? Set.of() : Set.of(role));
        UserContextHolder.set(ctx);
    }

    private String submittedApply() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-ROLE-" + System.nanoTime());
        a.setAssetName("角色门禁测试表");
        a.setRightType("持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化部");
        String id = applyService.saveDraft(a);
        applyService.submit(id); // 无上下文提交 -> 人工预审中
        return id;
    }

    @Test
    void precheck_node_only_precheck_role_can_approve() {
        String id = submittedApply();
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(id).getStatus());

        // 合规角色 越权预审 -> 拒绝
        actAs("review");
        assertThrows(BusinessException.class, () -> applyService.approve(id), "review 不应能处理人工预审节点");

        // 人工预审员 -> 放行,推进到合规审核中
        actAs("precheck");
        applyService.approve(id);
        assertEquals(ConfirmApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());

        // 合规节点:人工预审员越权 -> 拒绝;合规角色 -> 放行
        actAs("precheck");
        assertThrows(BusinessException.class, () -> applyService.approve(id), "precheck 不应能处理合规节点");
        actAs("review");
        applyService.approve(id);
        assertEquals(ConfirmApply.STATUS_MANAGER, applyService.getById(id).getStatus());
    }

    @Test
    void admin_and_super_override_any_node() {
        String id = submittedApply();
        actAs("all"); // 超级管理员
        applyService.approve(id);
        assertEquals(ConfirmApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());
        actAs("admin"); // 管理员可越节点
        applyService.approve(id);
        assertEquals(ConfirmApply.STATUS_MANAGER, applyService.getById(id).getStatus());
    }

    @Test
    void manager_and_director_gate_their_nodes() {
        String id = submittedApply();
        actAs("all");
        applyService.approve(id); // -> 合规
        applyService.approve(id); // -> 主管复核中
        assertEquals(ConfirmApply.STATUS_MANAGER, applyService.getById(id).getStatus());

        actAs("director");
        assertThrows(BusinessException.class, () -> applyService.approve(id), "director 不应能处理主管节点");
        actAs("manager");
        applyService.approve(id); // -> 经理终审中
        assertEquals(ConfirmApply.STATUS_DIRECTOR, applyService.getById(id).getStatus());

        actAs("manager");
        assertThrows(BusinessException.class, () -> applyService.approve(id), "manager 不应能处理经理节点");
        actAs("director");
        String cardId = applyService.approve(id); // 终审 -> 制卡
        assertEquals(ConfirmApply.STATUS_DONE, applyService.getById(id).getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(cardId);
    }
}
