package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 授权审批逐节点角色门禁(对齐架构 AA-10/BA-05 与工作指引授权流程):
 *   单位初审->unit、合规->review、业务->business、主管->manager、经理->director、副总->gm、领导小组->leadership;
 *   每节点仅对应角色(及 all/admin)可审批/驳回。无上下文(内部/单测)放行,故其余流程测试不受影响。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthNodeRoleTest {

    @Autowired
    private AuthApplyService applyService;

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

    private AuthApply draft(String mode, String assetId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(mode);
        a.setAssetId(assetId);
        a.setAssetName("角色门禁授权表");
        a.setEquityCardId("EC-PRA-VALID01");
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScenario("电力金融征信");
        a.setScope("全字段");
        return a;
    }

    @Test
    void special_flow_each_node_gated_by_role() {
        String id = applyService.saveDraft(draft(AuthApply.MODE_SPECIAL, "DA-ROLE-SP-" + System.nanoTime()));
        applyService.submit(id); // 无上下文 -> 单位初审中
        assertEquals(AuthApply.STATUS_UNIT, applyService.getById(id).getStatus());

        // 单位初审节点:合规角色越权 -> 拒绝;unit(申报单位分管领导) -> 放行
        actAs("review");
        assertThrows(BusinessException.class, () -> applyService.approve(id, null), "review 不应处理单位初审节点");
        actAs("unit");
        applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());

        // 合规节点:业务角色越权 -> 拒绝;review -> 放行
        actAs("business");
        assertThrows(BusinessException.class, () -> applyService.approve(id, null), "business 不应处理合规节点");
        actAs("review");
        applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_BUSINESS, applyService.getById(id).getStatus());

        // 业务节点:review越权 -> 拒绝;business -> 放行
        actAs("review");
        assertThrows(BusinessException.class, () -> applyService.approve(id, null), "review 不应处理业务节点");
        actAs("business");
        applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_MANAGER, applyService.getById(id).getStatus());

        // 主管节点:business -> 拒绝;manager -> 放行
        actAs("business");
        assertThrows(BusinessException.class, () -> applyService.approve(id, null), "business 不应处理主管节点");
        actAs("manager");
        applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_DIRECTOR, applyService.getById(id).getStatus());

        // 经理节点:manager -> 拒绝;director -> 放行
        actAs("manager");
        assertThrows(BusinessException.class, () -> applyService.approve(id, null), "manager 不应处理经理节点");
        actAs("director");
        applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_VP, applyService.getById(id).getStatus());

        // 副总节点:director -> 拒绝;gm -> 放行(终审=批准待双签,生效移至协议归档)
        actAs("director");
        assertThrows(BusinessException.class, () -> applyService.approve(id, null), "director 不应处理副总节点");
        actAs("gm");
        String certId = applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_APPROVED, applyService.getById(id).getStatus());
        assertNull(certId, "专项终审=批准(待双签),不即时登记生效记录");
    }

    @Test
    void batch_leadership_node_gated_and_admin_override() {
        String id = applyService.saveDraft(draft(AuthApply.MODE_BATCH, "DA-ROLE-BA-" + System.nanoTime()));
        applyService.submit(id);
        // admin 可越节点:合规 -> 主管 -> 经理 -> 副总 -> 领导小组
        actAs("admin");
        applyService.approve(id, null);
        applyService.approve(id, null);
        applyService.approve(id, null);
        applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_LEADERSHIP, applyService.getById(id).getStatus());

        // 领导小组节点:gm越权 -> 拒绝;leadership -> 放行(发证)
        actAs("gm");
        assertThrows(BusinessException.class, () -> applyService.approve(id, null), "gm 不应处理领导小组节点");
        actAs("leadership");
        String certId = applyService.approve(id, null);
        assertEquals(AuthApply.STATUS_EFFECTIVE, applyService.getById(id).getStatus());
        assertNotNull(certId, "领导小组批准应自动发证");
    }
}
