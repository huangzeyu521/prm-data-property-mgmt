package com.csg.prm.confirm;

import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0.3 变更单守卫(线性版本链不变量):
 * 1) 在途互斥 —— 同资产同一时刻仅一张在途确权变更单(并发变更会丢失更新);
 * 2) 基线乐观锁 —— baselineRef 引用的上一版确权须仍是当前最新,过期拦截提交。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmChangeGuardTest {

    @Autowired private ConfirmApplyService applyService;

    private ConfirmApply draft(String assetId, boolean change, String trigger) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName("守卫测试-" + assetId);
        a.setRightType("持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化管理部门");
        if (change) {
            a.setRegisterType("确权变更");
            a.setReConfirm(Boolean.TRUE);
            a.setChangeTrigger(trigger);
        }
        return a;
    }

    /** 推完整审批链至已完成(预审→合规→主管→经理终审制卡)。 */
    private void driveToDone(String applyId) {
        applyService.submit(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
    }

    @Test
    void inflight_change_blocks_second_change_submit_on_same_asset() {
        String assetId = "GRD-INF-" + System.nanoTime();
        String first = applyService.saveDraft(draft(assetId, true, "数据来源变更"));
        applyService.submit(first);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(first).getStatus());

        String second = applyService.saveDraft(draft(assetId, true, "管理要求变更"));
        BusinessException ex = assertThrows(BusinessException.class, () -> applyService.submit(second));
        assertTrue(ex.getMessage().contains("在途确权变更单"),
                "第二张同资产变更单应被在途互斥拦截:" + ex.getMessage());

        // 第一单撤回后,第二单可正常提交(互斥解除)
        applyService.withdraw(first, "让路测试");
        applyService.submit(second);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(second).getStatus());
    }

    @Test
    void stale_baselineRef_blocks_submit_and_fresh_passes() {
        String assetId = "GRD-OPT-" + System.nanoTime();
        // 先形成一笔"已完成"确权(当前最新基线)
        String prior = applyService.saveDraft(draft(assetId, false, null));
        driveToDone(prior);
        assertEquals(ConfirmApply.STATUS_DONE, applyService.getById(prior).getStatus());

        // 引用过期基线(伪造的上一版applyId)→ 提交被乐观锁拦截
        ConfirmApply stale = draft(assetId, true, "数据来源变更");
        stale.setBaselineRef("守卫测试#v1@STALE-APPLY-ID");
        String staleId = applyService.saveDraft(stale);
        BusinessException ex = assertThrows(BusinessException.class, () -> applyService.submit(staleId));
        assertTrue(ex.getMessage().contains("基线已过期"),
                "过期基线引用应被拦截:" + ex.getMessage());

        // 引用当前最新基线 → 正常提交
        ConfirmApply fresh = draft(assetId, true, "数据来源变更");
        fresh.setBaselineRef("守卫测试#v1@" + prior);
        String freshId = applyService.saveDraft(fresh);
        applyService.submit(freshId);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(freshId).getStatus());
    }

    @Test
    void data_add_change_skips_baseline_lock() {
        String assetId = "GRD-ADD-" + System.nanoTime();
        String prior = applyService.saveDraft(draft(assetId, false, null));
        driveToDone(prior);

        // 数据新增(insert 型):不修订既有结论,过期引用也不拦(新表首次登记与基线无冲突)
        ConfirmApply add = draft(assetId, true, "数据新增");
        add.setBaselineRef("守卫测试#v1@STALE-APPLY-ID");
        String addId = applyService.saveDraft(add);
        applyService.submit(addId);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(addId).getStatus());
    }
}
