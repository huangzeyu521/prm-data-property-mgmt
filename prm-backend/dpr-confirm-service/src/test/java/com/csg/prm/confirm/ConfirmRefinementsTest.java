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
        a.setSourceIdentification("A自行生产数据");
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
        // 关联识别含 H(个人隐私)-> 须填隐私关联主体说明;缺失则提交被拦截
        ConfirmApply a = base("DA-T2-001");
        a.setRelationIdentification("H");
        String id = applyService.saveDraft(a);
        BizException ex = assertThrows(BizException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("隐私"), "涉个人隐私须填说明:" + ex.getMessage());

        // 补充隐私关联主体说明后可正常进入审批链(首节点:人工预审)
        ConfirmApply ok = base("DA-T2-002");
        ok.setRelationIdentification("H");
        ok.setPrivacyInfo("涉用户隐私,已取得数据主体授权");
        String id2 = applyService.saveDraft(ok);
        applyService.submit(id2);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(id2).getStatus());
    }

    /** 工单规则:来源方式 A–F 至少选一;来源含 B–F 须填来源主体(资料完整性归集审查)。 */
    @Test
    void p5_source_method_completeness_required() {
        // 未选来源方式 -> 拦截
        ConfirmApply none = base("DA-SRC-NONE");
        none.setSourceIdentification(null);
        String idN = applyService.saveDraft(none);
        BizException e1 = assertThrows(BizException.class, () -> applyService.submit(idN));
        assertTrue(e1.getMessage().contains("来源方式"), "未选来源方式应拦截:" + e1.getMessage());

        // 含 B–F 但缺来源主体 -> 拦截
        ConfirmApply bf = base("DA-SRC-BF");
        bf.setSourceIdentification("B公开采集数据");
        String idB = applyService.saveDraft(bf);
        BizException e2 = assertThrows(BizException.class, () -> applyService.submit(idB));
        assertTrue(e2.getMessage().contains("来源主体"), "B–F 缺来源主体应拦截:" + e2.getMessage());

        // 含 B–F 且填来源主体(+表2)-> 通过进入审批链(首节点:人工预审)
        ConfirmApply ok = base("DA-SRC-OK");
        ok.setSourceIdentification("B公开采集数据");
        ok.setSourceSubject("某政府信息中心");
        ok.setThirdPartyInfo("公开采集,已履行免责声明");
        String idOk = applyService.saveDraft(ok);
        applyService.submit(idOk);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(idOk).getStatus());
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
