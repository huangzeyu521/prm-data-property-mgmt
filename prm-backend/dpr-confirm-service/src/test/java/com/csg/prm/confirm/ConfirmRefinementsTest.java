package com.csg.prm.confirm;

import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.EquityCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 确权精化项测试:P1 元数据质量门禁+自动填充、P2 表2 第三方权益维度、P4 权益卡片生命周期。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmRefinementsTest {

    @Autowired
    private ConfirmApplyService applyService;
    @Autowired
    private EquityCardService cardService;

    private ConfirmApply base(String assetId) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName("精化测试表");
        a.setRightType("数据资源持有权");
        a.setRightHolder("广东电网");
        return a;
    }

    @Test
    void p1_quality_gate_auto_rejects_low_metadata_score() {
        String id = applyService.saveDraft(base("DA-LOWQ-001"));
        applyService.submit(id); // 质量<80 自动驳回(持久化,不抛异常)
        ConfirmApply a = applyService.getById(id);
        assertEquals(ConfirmApply.STATUS_REJECTED, a.getStatus(), "元数据质量<80 应自动驳回");
        assertTrue(a.getRejectReason() != null && a.getRejectReason().contains("元数据质量"), "应记录驳回原因");
    }

    @Test
    void p1_autofill_returns_quality_score() {
        assertEquals(60, applyService.autofill("DA-LOWQ-x").qualityScore());
        assertEquals(92, applyService.autofill("DA-GOOD-x").qualityScore());
    }

    @Test
    void p2_table2_required_when_third_party_relation_selected() {
        // 关联识别含 H(个人隐私)-> 须填表2(thirdPartyInfo);缺失则提交被拦截
        ConfirmApply a = base("DA-T2-001");
        a.setRelationIdentification("H");
        String id = applyService.saveDraft(a);
        BizException ex = assertThrows(BizException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("表2"), "涉第三方/敏感须填表2");

        // 补充表2信息后可正常进入合规审核
        ConfirmApply ok = base("DA-T2-002");
        ok.setRelationIdentification("H");
        ok.setThirdPartyInfo("涉用户隐私,已取得数据主体授权");
        String id2 = applyService.saveDraft(ok);
        applyService.submit(id2);
        assertEquals(ConfirmApply.STATUS_COMPLIANCE, applyService.getById(id2).getStatus());
    }

    @Test
    void p4_equity_card_lifecycle_with_logs() {
        ConfirmApply a = base("DA-CARD-LC");
        a.setApplyId("AP-CARD-LC");
        String cardId = cardService.generateFromApply(a);
        assertEquals(EquityCard.STATUS_NORMAL, cardService.getById(cardId).getCardStatus());

        cardService.freeze(cardId);
        assertEquals(EquityCard.STATUS_FROZEN, cardService.getById(cardId).getCardStatus());
        cardService.unfreeze(cardId);
        assertEquals(EquityCard.STATUS_NORMAL, cardService.getById(cardId).getCardStatus());
        cardService.revoke(cardId, "确权撤销");
        assertEquals(EquityCard.STATUS_INVALID, cardService.getById(cardId).getCardStatus());
        // 注销后不可再注销
        assertThrows(BizException.class, () -> cardService.revoke(cardId, "x"));

        List<EquityCardLog> logs = cardService.listLogs(cardId);
        assertTrue(logs.size() >= 4, "应记录 生成/冻结/解冻/注销 全程");
    }
}
