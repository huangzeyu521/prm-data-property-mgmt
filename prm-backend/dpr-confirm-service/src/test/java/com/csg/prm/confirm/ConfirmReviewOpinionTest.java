package com.csg.prm.confirm;

import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmFlowLog;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmFlowLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合规管控小组(review)工作流:节点50 须能录入「权益认定意见」(BA-03 核心产出),且不被下游主管/经理意见覆盖。
 * 历史 bug:confirm approve(applyId) 无 opinion 入参 → 认定意见恒为规范默认,合规小组无法表达专业判定。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmReviewOpinionTest {

    @Autowired
    private ConfirmApplyService applyService;
    @Autowired
    private ConfirmFlowLogService flowLogService;

    @AfterEach
    void clear() {
        UserContextHolder.clear();
    }

    private void actAs(String role) {
        UserContext ctx = new UserContext();
        ctx.setUserId("u-" + role);
        ctx.setRoles(Set.of(role));
        UserContextHolder.set(ctx);
    }

    @Test
    void compliance_opinion_recorded_and_not_clobbered_by_downstream() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-REVOP-" + System.nanoTime());
        a.setAssetName("认定意见测试表");
        a.setRightType("持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化部");
        String id = applyService.saveDraft(a);
        applyService.submit(id);                 // -> 人工预审中
        actAs("precheck");
        applyService.approve(id, "材料完整,予以通过"); // -> 合规审核中
        assertEquals(ConfirmApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());

        // 合规小组录入权益认定意见
        String opinion = "权属来源合法,符合三权分置;持有权认定成立,无第三方权益冲突";
        actAs("review");
        applyService.approve(id, opinion);        // -> 主管复核中
        assertEquals(ConfirmApply.STATUS_MANAGER, applyService.getById(id).getStatus());
        assertEquals(opinion, applyService.getById(id).getRecognitionOpinion(),
                "合规小组录入的权益认定意见应被采用(而非规范默认串)");

        // 主管复核录入意见,不得覆盖合规小组的认定意见
        actAs("manager");
        applyService.approve(id, "主管复核同意");   // -> 经理终审中
        assertEquals(opinion, applyService.getById(id).getRecognitionOpinion(),
                "主管意见不得覆盖合规小组的权益认定意见");

        // 各节点审批人意见均留痕
        List<ConfirmFlowLog> logs = flowLogService.listByApply(id);
        ConfirmFlowLog toManager = logs.stream().filter(l -> ConfirmApply.STATUS_MANAGER.equals(l.getToStatus()))
                .reduce((x, y) -> y).orElse(null);
        assertNotNull(toManager);
        assertTrue(opinion.equals(toManager.getOpinion()), "合规->主管 流程日志应记录认定意见");
        ConfirmFlowLog toDirector = logs.stream().filter(l -> ConfirmApply.STATUS_DIRECTOR.equals(l.getToStatus()))
                .reduce((x, y) -> y).orElse(null);
        assertNotNull(toDirector);
        assertEquals("主管复核同意", toDirector.getOpinion(), "主管->经理 流程日志应记录主管意见");
    }
}
