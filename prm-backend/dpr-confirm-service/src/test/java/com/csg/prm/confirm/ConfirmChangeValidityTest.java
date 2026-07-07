package com.csg.prm.confirm;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.EquityCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P0′ 权益期限维(35号文三类触发之"权益到期"):
 * 到期型确权变更申报的新权益有效期,须在终审制卡时落到新版权益卡片 validDate(旧卡被取代),
 * 使"变更前期限 → 变更后期限"在卡片链上真实生效、可追溯。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmChangeValidityTest {

    @Autowired private EquityCardService cardService;
    @Autowired private ConfirmApplyService applyService;

    @Test
    void expiry_change_lands_new_validDate_on_superseding_card() {
        String assetId = "VLD-" + System.nanoTime();
        LocalDateTime oldDate = LocalDateTime.of(2026, 7, 18, 0, 0);
        LocalDateTime newDate = LocalDateTime.of(2028, 7, 18, 0, 0);

        // 初始确权 v1:原有效期
        ConfirmApply initial = new ConfirmApply();
        initial.setAssetId(assetId);
        initial.setAssetName("期限测试-" + assetId);
        initial.setRightType("持有权");
        initial.setRightHolder("广东电网");
        initial.setValidDate(oldDate);
        String cardId1 = cardService.generateFromApply(initial);
        assertEquals(oldDate, cardService.getById(cardId1).getValidDate(), "v1 卡应带初始有效期");

        // 权益到期变更 v2:申报新有效期 → 新卡落新期限,旧卡被取代
        ConfirmApply change = new ConfirmApply();
        change.setAssetId(assetId);
        change.setAssetName("期限测试-" + assetId);
        change.setRightType("持有权");
        change.setRightHolder("广东电网");
        change.setRegisterType("确权变更");
        change.setReConfirm(Boolean.TRUE);
        change.setChangeTrigger("权益到期");
        change.setValidDate(newDate);
        String cardId2 = cardService.generateFromApply(change);

        EquityCard c2 = cardService.getById(cardId2);
        assertEquals(newDate, c2.getValidDate(), "到期变更新卡应落申报的新有效期");
        assertEquals(2, c2.getVersion(), "变更卡版本+1");
        assertNotNull(c2.getSupersededCardNo(), "应回链被取代的 v1 卡");
        assertEquals(EquityCard.STATUS_INVALID, cardService.getById(cardId1).getCardStatus(),
                "旧期限卡应被取代失效(变更前期限转历史)");

        // 当前有效卡=新期限卡(findCurrentValid 口径)
        EquityCard cur = cardService.findCurrentValid(assetId, "持有权");
        assertEquals(newDate, cur.getValidDate(), "当前有效权益期限应为变更后新期限");
    }

    @Test
    void expiry_change_via_full_flow_keeps_validDate_through_approval() {
        String assetId = "VLD-FLOW-" + System.nanoTime();
        LocalDateTime newDate = LocalDateTime.of(2029, 1, 1, 0, 0);

        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName("期限流程测试");
        a.setRightType("持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRegisterType("确权变更");
        a.setReConfirm(Boolean.TRUE);
        a.setChangeTrigger("权益到期");
        a.setValidDate(newDate);
        String applyId = applyService.saveDraft(a);
        applyService.submit(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        applyService.approve(applyId);
        String cardId = applyService.approve(applyId);

        assertNotNull(cardId, "终审应制卡");
        assertEquals(newDate, cardService.getById(cardId).getValidDate(),
                "申报的新有效期应经全审批链落到权益卡片");
    }
}
