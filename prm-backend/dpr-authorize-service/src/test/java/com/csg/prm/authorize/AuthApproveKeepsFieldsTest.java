package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 授权审核台审批不丢申请要素契约锁(防回归)。
 *
 * 审核台详情抽屉渲染 AuthApply 的 表5/表6/§3.4.4 字段。审批通过(approve)推进节点用 updateById 整行回写,
 * 若实现回退为"只更新部分列/用空实体覆盖",会把 schemaName/rightType/利益分配/安全保障 等抹掉 → 审核台详情变空。
 * 本测试锁:submit→approve 推进一节点后,这些申请要素仍原样保留(与"查询带回"的 AuthApplyHistoryFieldsTest 互补,
 * 后者锁查询、本测试锁审批写回)。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthApproveKeepsFieldsTest {

    @Autowired
    private AuthApplyService applyService;

    @Test
    @DisplayName("submit→approve 推进节点后,表5/表6/§3.4.4 字段仍保留(审批不抹申请要素)")
    void approve_preserves_application_elements() {
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
        String id = applyService.saveDraft(a);

        applyService.submit(id);
        String beforeStatus = applyService.getById(id).getStatus();
        applyService.approve(id, "同意");

        AuthApply after = applyService.getById(id);
        assertNotNull(after);
        assertNotEquals(beforeStatus, after.getStatus(), "approve 应推进到下一环节");
        // 申请要素不应因审批推进而丢失
        assertEquals("BILLING", after.getSchemaName(), "审批后 模式名称 仍保留");
        assertEquals("数据加工使用权", after.getRightType(), "审批后 权益类型 仍保留");
        assertEquals("营销域", after.getBusinessDomain(), "审批后 业务域 仍保留");
        assertEquals("综合能源服务", after.getScenario(), "审批后 使用场景 仍保留");
        assertEquals("按调用次数计费,收益 7:3 分成", after.getBenefitAllocation(), "审批后 利益分配 仍保留");
        assertEquals("加密传输 + 最小授权访问控制 + 操作留痕审计", after.getSecurityReq(), "审批后 安全保障 仍保留");
        assertEquals("SYS:营销管理系统", after.getAssetId(), "审批后 assetId(SYS:系统)仍保留");
    }
}
