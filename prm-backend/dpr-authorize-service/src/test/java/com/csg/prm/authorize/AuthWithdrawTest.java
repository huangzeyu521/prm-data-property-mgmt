package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthFlowLogService;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权(一事一议)申请人主动撤回:审批中 -> 已撤回(与确权域 ConfirmWithdrawTest 对称)。
 * 仅审批链活动态可撤;草稿(应删除)/已驳回 不可撤;撤回留痕;formNo 级整单撤回。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthWithdrawTest {

    @Autowired private AuthApplyService applyService;
    @Autowired private AuthFlowLogService flowLogService;

    private AuthApply special(String assetId, String name) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId(assetId);
        a.setAssetName(name);
        a.setEquityCardId("EC-PRA-" + assetId);
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScope("全字段");
        return a;
    }

    @Test
    void withdraw_from_review_sets_withdrawn_and_logs() {
        String id = applyService.saveDraft(special("DA-AWD-1", "撤回测试-单位初审"));
        applyService.submit(id);
        assertEquals(AuthApply.STATUS_UNIT, applyService.getById(id).getStatus(), "一事一议提交后首环=单位初审中");

        applyService.withdraw(id, "信息有误,先撤回修改");
        assertEquals(AuthApply.STATUS_WITHDRAWN, applyService.getById(id).getStatus(), "撤回后应为已撤回");

        boolean logged = flowLogService.listByApply(id).stream()
                .anyMatch(l -> AuthApply.STATUS_WITHDRAWN.equals(l.getToStatus()));
        assertTrue(logged, "撤回应留痕(流转记录含 已撤回)");
    }

    @Test
    void cannot_withdraw_draft() {
        String id = applyService.saveDraft(special("DA-AWD-2", "撤回测试-草稿"));
        BusinessException ex = assertThrows(BusinessException.class, () -> applyService.withdraw(id, "x"));
        assertTrue(ex.getMessage().contains("不可撤回"), "草稿不可撤回(应删除):" + ex.getMessage());
    }

    @Test
    void cannot_withdraw_after_rejected() {
        String id = applyService.saveDraft(special("DA-AWD-3", "撤回测试-已驳回"));
        applyService.submit(id);
        applyService.reject(id, "材料不全");
        assertEquals(AuthApply.STATUS_REJECTED, applyService.getById(id).getStatus());
        assertThrows(BusinessException.class, () -> applyService.withdraw(id, "x"), "已驳回不可撤回");
    }

    @Test
    void withdrawForm_withdraws_all_inreview_rows() {
        String formNo = applyService.createForm();
        AuthApply a1 = special("DA-AWF-1", "表一"); a1.setFormNo(formNo);
        AuthApply a2 = special("DA-AWF-2", "表二"); a2.setFormNo(formNo);
        String id1 = applyService.saveDraft(a1);
        String id2 = applyService.saveDraft(a2);
        applyService.submitForm(formNo);
        assertEquals(AuthApply.STATUS_UNIT, applyService.getById(id1).getStatus());

        applyService.withdrawForm(formNo);
        assertEquals(AuthApply.STATUS_WITHDRAWN, applyService.getById(id1).getStatus(), "整单撤回应把每张表撤回");
        assertEquals(AuthApply.STATUS_WITHDRAWN, applyService.getById(id2).getStatus());
    }
}
