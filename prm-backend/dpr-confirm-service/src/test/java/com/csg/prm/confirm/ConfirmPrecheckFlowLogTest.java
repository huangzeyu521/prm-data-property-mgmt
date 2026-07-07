package com.csg.prm.confirm;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmFlowLog;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmFlowLogService;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
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
 * 归集预审团队(precheck)工作流可见性守卫:确权进度流转/通知必须体现「人工预审」节点。
 * 历史 bug:ConfirmFlowLogServiceImpl 的 notify/nodeOf/nameOf 漏掉 STATUS_PRECHECK →
 * 提交后通知误写"进入合规审核"、流程日志节点号为 null,预审团队的环节在申请人进度流里隐身。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmPrecheckFlowLogTest {

    @Autowired
    private ConfirmApplyService applyService;
    @Autowired
    private ConfirmFlowLogService flowLogService;

    @AfterEach
    void clear() {
        UserContextHolder.clear();
    }

    private ConfirmFlowLog logTo(String applyId, String toStatus) {
        List<ConfirmFlowLog> logs = flowLogService.listByApply(applyId);
        return logs.stream().filter(l -> toStatus.equals(l.getToStatus()))
                .reduce((a, b) -> b).orElse(null);
    }

    @Test
    void precheck_node_visible_in_flowlog_and_notify() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-PRECHK-" + System.nanoTime());
        a.setAssetName("预审进度测试表");
        a.setRightType("持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化部");
        String id = applyService.saveDraft(a);
        applyService.submit(id); // -> 人工预审中

        // 提交后落到人工预审,流程日志须含该环节:节点号=40、节点名=人工预审、通知体现"人工预审"
        ConfirmFlowLog toPrecheck = logTo(id, ConfirmApply.STATUS_PRECHECK);
        assertNotNull(toPrecheck, "提交后应有一条流转到【人工预审中】的流程日志");
        assertEquals(ConfirmApply.NODE_PRECHECK, toPrecheck.getNode(), "人工预审节点号应为 40,而非 null");
        assertNotNull(toPrecheck.getNotifyContent(), "应有进度通知");
        assertTrue(toPrecheck.getNotifyContent().contains("人工预审"),
                "提交通知须体现进入【人工预审】,实际:" + toPrecheck.getNotifyContent());

        // 预审通过 -> 合规;通知须体现"已通过人工预审",而非误写"已提交"
        actAs("precheck");
        applyService.approve(id);
        assertEquals(ConfirmApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());
        ConfirmFlowLog toCompliance = logTo(id, ConfirmApply.STATUS_COMPLIANCE);
        assertNotNull(toCompliance);
        assertTrue(toCompliance.getNotifyContent().contains("人工预审"),
                "预审通过进入合规的通知须体现【已通过人工预审】,实际:" + toCompliance.getNotifyContent());
    }

    private void actAs(String role) {
        UserContext ctx = new UserContext();
        ctx.setUserId("u-" + role);
        ctx.setRoles(Set.of(role));
        UserContextHolder.set(ctx);
    }
}
