package com.csg.prm.confirm;

import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmFlowLog;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmFlowLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 申请人主动撤回(已撤回中间态,与已驳回对称):
 * 仅审批链活动态可撤回;草稿/已完成/已驳回 不可撤回;撤回留痕;撤回后状态为已撤回(可重新编辑提交)。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmWithdrawTest {

    @Autowired private ConfirmApplyService applyService;
    @Autowired private ConfirmFlowLogService flowLogService;

    private ConfirmApply draft(String assetId, String name) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName(name);
        a.setRightType("数据资源持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化管理部门");
        return a;
    }

    @Test
    void withdraw_from_precheck_sets_withdrawn_and_logs() {
        String id = applyService.saveDraft(draft("DA-WD-001", "撤回测试-人工预审"));
        applyService.submit(id);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(id).getStatus());

        applyService.withdraw(id, "信息有误,先撤回修改");
        assertEquals(ConfirmApply.STATUS_WITHDRAWN, applyService.getById(id).getStatus(), "撤回后应为已撤回中间态");

        boolean logged = flowLogService.listByApply(id).stream()
                .anyMatch(l -> ConfirmApply.STATUS_WITHDRAWN.equals(l.getToStatus()));
        assertTrue(logged, "撤回应留痕(流转记录含 已撤回)");
    }

    @Test
    void withdraw_from_advanced_state_works() {
        String id = applyService.saveDraft(draft("DA-WD-002", "撤回测试-审批中"));
        applyService.submit(id);
        applyService.approve(id); // 人工预审 -> 合规审核中
        assertEquals(ConfirmApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());

        applyService.withdraw(id, null);
        assertEquals(ConfirmApply.STATUS_WITHDRAWN, applyService.getById(id).getStatus(), "审批链中途亦可撤回");
    }

    @Test
    void cannot_withdraw_draft() {
        String id = applyService.saveDraft(draft("DA-WD-003", "撤回测试-草稿"));
        BizException ex = assertThrows(BizException.class, () -> applyService.withdraw(id, "x"));
        assertTrue(ex.getMessage().contains("不可撤回"), "草稿不可撤回(应删除):" + ex.getMessage());
    }

    @Test
    void cannot_withdraw_after_done_or_rejected() {
        // 走完整链到已完成
        String id = applyService.saveDraft(draft("DA-WD-004", "撤回测试-已完成"));
        applyService.submit(id);
        applyService.approve(id); // 预审->合规
        applyService.approve(id); // 合规->主管
        applyService.approve(id); // 主管->经理
        applyService.approve(id); // 经理->已完成(制卡)
        assertEquals(ConfirmApply.STATUS_DONE, applyService.getById(id).getStatus());
        assertThrows(BizException.class, () -> applyService.withdraw(id, "x"), "已完成不可撤回");

        // 已驳回不可撤回
        String id2 = applyService.saveDraft(draft("DA-WD-005", "撤回测试-已驳回"));
        applyService.submit(id2);
        applyService.reject(id2, "材料不全");
        assertEquals(ConfirmApply.STATUS_REJECTED, applyService.getById(id2).getStatus());
        assertThrows(BizException.class, () -> applyService.withdraw(id2, "x"), "已驳回不可撤回");
    }
}
