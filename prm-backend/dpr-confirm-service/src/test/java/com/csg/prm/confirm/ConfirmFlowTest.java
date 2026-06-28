package com.csg.prm.confirm;

import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmSummaryService;
import com.csg.prm.confirm.service.EquityCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据确权全流程集成测试(对齐附录F 八节点):草稿 -> 提交 -> 合规审核(生成表3/表4) -> 主管复核
 *   -> 经理终审 -> 自动生成权益卡片;以及驳回与校验。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmFlowTest {

    @Autowired
    private ConfirmApplyService applyService;
    @Autowired
    private EquityCardService cardService;
    @Autowired
    private ConfirmSummaryService summaryService;

    private ConfirmApply draft(String assetId, String name) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(assetId);
        a.setAssetName(name);
        a.setRightType("数据资源持有权");
        a.setSourceIdentification("A自行生产数据");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化管理部门");
        a.setInvolvesThirdParty(Boolean.TRUE);
        a.setThirdPartyInfo("第三方:某征信机构;权益转移已约定");
        return a;
    }

    @Test
    void full_flow_eight_nodes_should_generate_summaries_and_card() {
        String id = applyService.saveDraft(draft("DA-CFM-001", "客户用电信息表"));
        assertEquals(ConfirmApply.STATUS_DRAFT, applyService.getById(id).getStatus());
        assertNotNull(applyService.getById(id).getApplyNo(), "应自动生成申请编号");

        applyService.submit(id);
        assertEquals(ConfirmApply.STATUS_PRECHECK, applyService.getById(id).getStatus(), "提交后先进人工预审");

        // 节点40 人工预审通过(复核 AI 校验结果)-> 合规审核中(尚未生成表3/表4)
        assertNull(applyService.approve(id));
        assertEquals(ConfirmApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());
        assertEquals(0, summaryService.listByApply(id).size(), "人工预审阶段尚未生成汇总表");

        // 节点50 合规审核通过 -> 主管复核中,生成表3/表4 + 认定意见(尚未制卡)
        assertNull(applyService.approve(id));
        assertEquals(ConfirmApply.STATUS_MANAGER, applyService.getById(id).getStatus());
        assertNotNull(applyService.getById(id).getRecognitionOpinion(), "合规审核应形成认定意见");
        assertEquals(2, summaryService.listByApply(id).size(), "应生成表3、表4 两张汇总表");

        // 节点60 主管复核 -> 经理终审中
        assertNull(applyService.approve(id));
        assertEquals(ConfirmApply.STATUS_DIRECTOR, applyService.getById(id).getStatus());

        // 节点70 经理终审 -> 节点80 已完成 + 自动生成权益卡片
        String cardId = applyService.approve(id);
        assertNotNull(cardId, "终审通过应自动生成权益卡片");
        assertEquals(ConfirmApply.STATUS_DONE, applyService.getById(id).getStatus());
        assertEquals(Integer.valueOf(ConfirmApply.NODE_DONE), applyService.getById(id).getCurrentNode());

        EquityCard card = cardService.getById(cardId);
        assertNotNull(card.getCardNo(), "卡片应有全局唯一编码");
        assertEquals("DA-CFM-001", card.getAssetId());
        assertEquals(EquityCard.STATUS_NORMAL, card.getCardStatus());
        assertEquals(id, card.getApplyId());
        assertEquals("中国南方电网有限责任公司", card.getConsolidatedUnit(), "确权制卡应归口网级(权益归集原则)");
        assertTrue(summaryService.listByApply(id).stream()
                .anyMatch(s -> s.getSummaryType().contains("表3")), "应含表3汇总");
    }

    @Test
    void reject_should_set_rejected_status() {
        String id = applyService.saveDraft(draft("DA-CFM-002", "设备台账表"));
        applyService.submit(id);
        applyService.reject(id, "材料不齐,缺采购协议");
        ConfirmApply a = applyService.getById(id);
        assertEquals(ConfirmApply.STATUS_REJECTED, a.getStatus());
        assertEquals("材料不齐,缺采购协议", a.getRejectReason());
    }

    @Test
    void draft_should_reject_missing_right_type() {
        ConfirmApply bad = new ConfirmApply();
        bad.setAssetId("DA-CFM-003");
        bad.setAssetName("缺权属类型表");
        assertThrows(BusinessException.class, () -> applyService.saveDraft(bad));
    }

    @Test
    void cannot_approve_draft_directly() {
        String id = applyService.saveDraft(draft("DA-CFM-004", "未提交表"));
        assertThrows(BusinessException.class, () -> applyService.approve(id));
    }

    /** 确权变更(附录F §3.3.2 重新确权)须填变更触发类型,否则提交被拦。 */
    @Test
    void change_registration_requires_change_trigger() {
        ConfirmApply a = draft("DA-CFM-CHG", "确权变更校验");
        a.setRegisterType("确权变更"); // 未填 changeTrigger
        String id = applyService.saveDraft(a);
        BusinessException ex = assertThrows(BusinessException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("变更触发类型"), "确权变更未填触发类型应被拦:" + ex.getMessage());
    }
}
