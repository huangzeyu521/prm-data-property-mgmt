package com.csg.prm.confirm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;
import com.csg.prm.confirm.mapper.EquityCardLogMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.EquityCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 变更全流程闭环(附录F"确权变更"登记类型 + 权益变更):
 * 1) 确权变更终审生成新权益卡片时,前序"正常"卡被取代(失效),新卡版本+1、回链前序 → 当前有效权益唯一;
 * 2) 重确权工单对齐"确权变更"登记类型 + 变更触发类型,并从前序有效卡"基于现状预填"。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmChangeLifecycleTest {

    @Autowired private EquityCardService cardService;
    @Autowired private EquityCardMapper cardMapper;
    @Autowired private EquityCardLogMapper logMapper;
    @Autowired private ConfirmApplyService applyService;

    private ConfirmApply apply(String assetId, String holder, boolean change) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName("变更测试-" + assetId);
        a.setRightType("数据持有权");
        a.setRightHolder(holder);
        if (change) {
            a.setReConfirm(Boolean.TRUE);
            a.setRegisterType("确权变更");
        }
        return a;
    }

    private long normalCardCount(String assetId) {
        return cardMapper.selectCount(new LambdaQueryWrapper<EquityCard>()
                .eq(EquityCard::getAssetId, assetId)
                .eq(EquityCard::getCardStatus, EquityCard.STATUS_NORMAL));
    }

    @Test
    void confirmChange_supersedes_priorCard_and_keeps_single_valid() {
        String assetId = "CHG-" + System.nanoTime();

        // 初始确权 → v1 正常
        String cardId1 = cardService.generateFromApply(apply(assetId, "广东电网", false));
        EquityCard c1 = cardService.getById(cardId1);
        assertEquals(1, c1.getVersion(), "初始确权版本应为1");
        assertNull(c1.getSupersededCardNo(), "初始确权无前序");
        assertEquals(EquityCard.STATUS_NORMAL, c1.getCardStatus());

        // 确权变更 → v2 正常,取代 v1
        String cardId2 = cardService.generateFromApply(apply(assetId, "中国南方电网有限责任公司", true));
        EquityCard c2 = cardService.getById(cardId2);
        assertEquals(2, c2.getVersion(), "确权变更版本应+1");
        assertEquals(c1.getCardNo(), c2.getSupersededCardNo(), "新卡应回链前序卡号");
        assertEquals(EquityCard.STATUS_NORMAL, c2.getCardStatus());

        // 前序卡被取代 → 失效
        EquityCard c1Reloaded = cardService.getById(cardId1);
        assertEquals(EquityCard.STATUS_INVALID, c1Reloaded.getCardStatus(), "前序卡应被取代为失效");

        // 当前有效权益唯一,且 findCurrentValid 命中最新版
        assertEquals(1, normalCardCount(assetId), "同资产+权利仅一张正常卡");
        EquityCard cur = cardService.findCurrentValid(assetId, "数据持有权");
        assertEquals(cardId2, cur.getCardId(), "当前有效卡应为最新版");

        // 前序卡留痕"被取代"(防篡改证据链)
        List<EquityCardLog> logs = cardService.listLogs(cardId1);
        assertTrue(logs.stream().anyMatch(l -> "被取代".equals(l.getAction())),
                "前序卡应有'被取代'变更留痕");
    }

    @Test
    void reConfirm_aligns_registerType_trigger_and_prefills_from_current_card() {
        String assetId = "RCF-" + System.nanoTime();

        // 预置一张当前有效卡(模拟已确权现状)
        EquityCard prior = new EquityCard();
        prior.setCardNo("EC-PRIOR-" + System.nanoTime());
        prior.setAssetId(assetId);
        prior.setAssetName("预填测试");
        prior.setRightType("数据持有权");
        prior.setRightOwner("贵州电网");
        prior.setCardStatus(EquityCard.STATUS_NORMAL);
        prior.setVersion(1);
        prior.setValidDate(LocalDateTime.now().plusYears(1));
        cardMapper.insert(prior);

        String applyId = applyService.createReConfirm(assetId, null, null,
                "数据来源系统迁移", "ALERT-9", "数据来源变更");
        ConfirmApply created = applyService.getById(applyId);

        assertEquals("确权变更", created.getRegisterType(), "重确权应对齐'确权变更'登记类型");
        assertEquals("数据来源变更", created.getChangeTrigger(), "应记录变更触发类型");
        assertEquals(Boolean.TRUE, created.getReConfirm(), "应标记重确权");
        assertEquals("贵州电网", created.getRightHolder(), "应从前序有效卡预填权属主体");
        assertNotNull(created.getApplyNo(), "应生成申请编号");
    }
}
