package com.csg.prm.confirm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.dto.RecheckHealthVO;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmRecheckTask;
import com.csg.prm.confirm.entity.ConfirmTableItem;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.mapper.ConfirmRecheckTaskMapper;
import com.csg.prm.confirm.mapper.ConfirmTableItemMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmRecheckTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1 重确权工单闭环(35号文§二(三)2 按季度定期重新确权):
 * 检测(到期扫描/监测联动)→ 工单入池(幂等)→ 处置双出口(派生变更草稿/复核确认无变化留痕)→ 销号;
 * P2.1 变更生效联动:update 型变更终审制卡后,对受影响在用授权生成「授权处置」工单。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmRecheckTaskTest {

    @Autowired private ConfirmRecheckTaskService recheckService;
    @Autowired private ConfirmRecheckTaskMapper taskMapper;
    @Autowired private ConfirmApplyService applyService;
    @Autowired private EquityCardMapper cardMapper;
    @Autowired private ConfirmTableItemMapper tableItemMapper;

    private ConfirmRecheckTask newTask(String assetId) {
        ConfirmRecheckTask t = new ConfirmRecheckTask();
        t.setTaskType(ConfirmRecheckTask.TYPE_RECHECK);
        t.setAssetId(assetId);
        t.setAssetName("工单测试-" + assetId);
        t.setSource(ConfirmRecheckTask.SOURCE_QUARTER_SCAN);
        t.setTriggerType("权益到期");
        t.setReason("测试工单");
        return t;
    }

    @Test
    void scan_creates_task_for_due_card_and_is_idempotent() {
        String assetId = "RCT-SCAN-" + System.nanoTime();
        EquityCard card = new EquityCard();
        card.setCardNo("EC-RCT-" + System.nanoTime());
        card.setAssetId(assetId);
        card.setAssetName("到期扫描测试");
        card.setRightType("持有权");
        card.setRightOwner("广东电网");
        card.setCardStatus(EquityCard.STATUS_NORMAL);
        card.setVersion(1);
        card.setValidDate(LocalDateTime.now().plusDays(10)); // 90天内到期
        cardMapper.insert(card);

        recheckService.scanDueCards(90);
        List<ConfirmRecheckTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getAssetId, assetId));
        assertEquals(1, tasks.size(), "到期卡片应生成重确权工单");
        assertEquals(ConfirmRecheckTask.STATUS_OPEN, tasks.get(0).getStatus());
        assertEquals("权益到期", tasks.get(0).getTriggerType());
        assertNotNull(tasks.get(0).getTaskNo());

        // 再扫幂等:同资产不重复入池
        recheckService.scanDueCards(90);
        assertEquals(1, taskMapper.selectCount(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getAssetId, assetId)), "重复扫描应幂等归并");
    }

    @Test
    void derive_creates_change_draft_and_done_apply_closes_task() {
        String assetId = "RCT-DRV-" + System.nanoTime();
        String taskId = recheckService.createTask(newTask(assetId));

        // 出口①:派生确权变更草稿(sourceRef=RECHECK:工单号 证据链)
        String applyId = recheckService.deriveChange(taskId);
        ConfirmApply apply = applyService.getById(applyId);
        assertEquals("确权变更", apply.getRegisterType());
        assertTrue(apply.getSourceRef() != null && apply.getSourceRef().startsWith("RECHECK:"),
                "派生草稿应带工单证据链 sourceRef:" + apply.getSourceRef());
        ConfirmRecheckTask task = recheckService.getById(taskId);
        assertEquals(ConfirmRecheckTask.STATUS_CHANGING, task.getStatus());
        assertEquals(applyId, task.getApplyId());

        // 补全草稿要素后走完审批链 → 工单自动销号
        apply.setSourceIdentification("A自行生产数据");
        apply.setRightHolder("广东电网有限责任公司");
        apply.setRightType("持有权");
        applyService.saveDraft(apply);
        applyService.submit(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        assertEquals(ConfirmApply.STATUS_DONE, applyService.getById(applyId).getStatus());
        assertEquals(ConfirmRecheckTask.STATUS_DONE, recheckService.getById(taskId).getStatus(),
                "派生变更单终审生效应自动销号工单");
    }

    @Test
    void no_change_review_requires_note_and_leaves_trace() {
        String taskId = recheckService.createTask(newTask("RCT-NCH-" + System.nanoTime()));

        assertThrows(BusinessException.class, () -> recheckService.confirmNoChange(taskId, ""),
                "复核结论必填(留痕销号)");

        recheckService.confirmNoChange(taskId, "季度复核:来源/管理要求/期限均未变动,结论维持");
        ConfirmRecheckTask task = recheckService.getById(taskId);
        assertEquals(ConfirmRecheckTask.STATUS_NO_CHANGE, task.getStatus());
        assertNotNull(task.getHandleTime(), "应留痕复核时间");
        assertNotNull(task.getHandlerName(), "应留痕复核人");
        assertTrue(task.getHandleNote().contains("未变动"));

        // 已销号工单不可再派生
        assertThrows(BusinessException.class, () -> recheckService.deriveChange(taskId));
    }

    @Test
    void monitor_derived_reconfirm_registers_into_pool() {
        String assetId = "RCT-MON-" + System.nanoTime();
        // P1.3 收敛:监测联动直调 createReConfirm(sourceRef=预警ID)也要在工单池可见
        String applyId = applyService.createReConfirm(assetId, "监测派生测试", null,
                "监测识别来源变更", "ALERT-RCT-1", "数据来源变更");
        List<ConfirmRecheckTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getAssetId, assetId));
        assertEquals(1, tasks.size(), "监测派生草稿应登记入工单池");
        assertEquals(ConfirmRecheckTask.STATUS_CHANGING, tasks.get(0).getStatus());
        assertEquals(applyId, tasks.get(0).getApplyId());
        assertEquals(ConfirmRecheckTask.SOURCE_MONITOR, tasks.get(0).getSource());
    }

    @Test
    void change_effective_creates_auth_disposal_tasks_for_affected_auths() {
        // P2.1:update 型变更,涉已对外授权库表 MKT_TRADE_SETTLE(桩) → 终审制卡后生成授权处置工单
        String assetId = "SYS:营销管理系统";
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName("营销管理系统");
        a.setRightType("持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRegisterType("确权变更");
        a.setReConfirm(Boolean.TRUE);
        a.setChangeTrigger("管理要求变更");
        a.setChangeVersion(2);
        String applyId = applyService.saveDraft(a);

        ConfirmTableItem item = new ConfirmTableItem();
        item.setApplyId(applyId);
        item.setTableCode("MKT_TRADE_SETTLE");
        item.setTableName("市场交易结算表");
        item.setSourceType("A自行生产数据");
        tableItemMapper.insert(item);

        applyService.submit(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        assertEquals(ConfirmApply.STATUS_DONE, applyService.getById(applyId).getStatus());

        List<ConfirmRecheckTask> disposals = taskMapper.selectList(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getAssetId, assetId)
                .eq(ConfirmRecheckTask::getTaskType, ConfirmRecheckTask.TYPE_AUTH_DISPOSAL));
        assertFalse(disposals.isEmpty(), "变更生效应对受影响在用授权生成处置工单");
        ConfirmRecheckTask d = disposals.get(0);
        assertEquals("AUTH-MKT_TRADE_SETTLE", d.getRefNo(), "处置工单应回链受影响授权编号");
        assertEquals(ConfirmRecheckTask.SOURCE_CHANGE_EFFECT, d.getSource());
        assertTrue(d.getReason().contains("市场交易结算表"));
    }

    @Test
    void health_reports_rates_within_range() {
        RecheckHealthVO h = recheckService.health();
        assertNotNull(h);
        assertTrue(h.getChainIntegrityRate() >= 0 && h.getChainIntegrityRate() <= 100);
        assertTrue(h.getOnTimeRate() >= 0 && h.getOnTimeRate() <= 100);
    }
}
