package com.csg.prm.confirm;

import com.csg.prm.confirm.dto.ConfirmDashboardVO;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 确权看板(F-04)集成测试:走完一笔确权后,看板应统计到完成量、通过率与权益卡片数。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmDashboardTest {

    @Autowired
    private ConfirmApplyService applyService;
    @Autowired
    private ConfirmDashboardService dashboardService;

    @Test
    void dashboard_should_reflect_completed_confirmation() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-DASH-001");
        a.setAssetName("看板测试表");
        a.setRightType("持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        String id = applyService.saveDraft(a);
        applyService.submit(id);
        applyService.approve(id); // 节点40 人工预审通过 -> 合规
        applyService.approve(id); // 节点50 合规审核通过
        applyService.approve(id); // 节点60 主管复核通过
        applyService.approve(id); // 节点70 经理终审通过 -> 制卡

        // 另起一笔仅提交(停在人工预审中)-> 验证新节点纳入审批积压统计
        ConfirmApply b = new ConfirmApply();
        b.setAssetId("DA-DASH-002");
        b.setAssetName("人工预审积压表");
        b.setRightType("持有权");
        b.setSourceIdentification("A自行生产数据");
        b.setRightHolder("广东电网有限责任公司");
        String pid = applyService.saveDraft(b);
        applyService.submit(pid);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(pid).getStatus());

        ConfirmDashboardVO vo = dashboardService.dashboard(null, null, null);
        assertTrue(vo.getTotalApply() >= 1);
        assertTrue(vo.getDone() >= 1, "应统计到已完成确权");
        assertTrue(vo.getCardCount() >= 1, "应统计到权益卡片");
        assertTrue(vo.getPassRate() > 0);
        assertNotNull(vo.getStatusDistribution());
        assertTrue(vo.getRightTypeDistribution().containsKey("持有权"));
        assertTrue(vo.getNodeBacklog().containsKey(ConfirmApply.STATUS_PRECHECK),
                "人工预审中应纳入审批节点积压统计(修复漏统计新节点)");
    }
}
