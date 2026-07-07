package com.csg.prm.confirm.ai;

import com.csg.prm.common.aitrace.AiRunLog;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.common.aitrace.AiRunLogService;
import com.csg.prm.confirm.service.ConfirmAiSnapshotService;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 大模型校验机制完善(南网需求)三件套:
 * ① 逐次留痕——每次 AI 调用落库,带模型/输入摘要/输出/耗时/SM3 指纹/触发人;
 * ② 防篡改快照——服务端计 SM3 + 上链存证,verify 可验真,篡改即可检出;
 * ③ 校验规则可视化——逐应交项暴露 校验逻辑 + 规则明细 + AI 判定依据。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmAiTraceTest {

    @Autowired private ConfirmApplyService applyService;
    @Autowired private ConfirmAiService aiService;
    @Autowired private AiRunLogService runLogService;
    @Autowired private ConfirmAiSnapshotService snapshotService;
    @Autowired private ConfirmApplyMapper applyMapper;
    @Autowired private PlatformTransactionManager txManager;

    private String draft() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("AST-TRACE-1");
        a.setAssetName("留痕测试资产");
        a.setRightType("使用权");
        a.setRightHolder("广东电网有限责任公司");
        a.setSourceIdentification("A");
        a.setRelationIdentification("G,H");
        a.setInvolvesThirdParty(Boolean.TRUE);
        return applyService.saveDraft(a);
    }

    @Test
    void ai_calls_are_logged_with_sm3_and_model() {
        String id = draft();
        aiService.decision(id);  // 决策研判
        aiService.conflict(id);  // 冲突识别

        List<AiRunLog> logs = runLogService.listByBiz(id);
        assertTrue(logs.size() >= 2, "每次 AI 调用应逐条留痕,实际 " + logs.size());
        assertTrue(logs.stream().anyMatch(l -> AiRunLog.CAP_DECISION.equals(l.getCapability())), "应含决策研判留痕");
        assertTrue(logs.stream().anyMatch(l -> AiRunLog.CAP_CONFLICT.equals(l.getCapability())), "应含冲突识别留痕");
        assertTrue(logs.stream().allMatch(l -> l.getSm3Hash() != null && !l.getSm3Hash().isBlank()), "每条留痕应有 SM3 指纹");
        assertTrue(logs.stream().allMatch(l -> l.getModel() != null), "每条留痕应记录模型");
        assertTrue(logs.stream().allMatch(l -> l.getDurationMs() != null && l.getDurationMs() >= 0), "每条留痕应记录耗时");
        assertTrue(logs.stream().allMatch(l -> l.getTriggerUser() != null), "每条留痕应记录触发人");
    }

    @Test
    void snapshot_is_server_hashed_and_tamper_evident() {
        String id = draft();
        snapshotService.save(id, "{\"materialCheck\":{\"overall\":\"通过\"},\"ruleReport\":{\"allPass\":true}}");

        Map<String, Object> v = snapshotService.verify(id);
        assertEquals(Boolean.TRUE, v.get("verified"), "刚固化的快照应验真通过");
        assertNotNull(v.get("payloadSm3"), "应有服务端计算的 SM3");
        assertNotNull(v.get("evidenceId"), "应有上链存证ID");

        // 篡改库内快照 payload(通过->不通过),验真应检出被篡改
        ConfirmApply a = applyMapper.selectById(id);
        ConfirmApply upd = new ConfirmApply();
        upd.setApplyId(id);
        upd.setAiSnapshot(a.getAiSnapshot().replace("通过", "不通过"));
        applyMapper.updateById(upd);

        Map<String, Object> v2 = snapshotService.verify(id);
        assertEquals(Boolean.FALSE, v2.get("verified"), "篡改后验真应失败(防篡改)");
    }

    @Test
    void runlog_survives_business_transaction_rollback() {
        // 审计留痕须独立于业务事务:即便业务事务回滚,"AI 调用已发生"的留痕也必须保留(REQUIRES_NEW)。
        String bizId = "ROLLBACK-GUARD-" + draft();
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.execute(status -> {
            runLogService.record(AiRunLog.BIZ_CONFIRM, bizId, AiRunLog.CAP_MATERIAL_CHECK,
                    "local-rule-stub", "in", "{\"overall\":\"通过\"}", 1L);
            status.setRollbackOnly(); // 业务事务回滚
            return null;
        });
        assertEquals(1, runLogService.listByBiz(bizId).size(),
                "REQUIRES_NEW:留痕应独立于业务事务回滚而保留(可审计)");
    }

    @Test
    void check_logic_visualizes_rules_and_basis() {
        String id = draft();
        ConfirmAiService.CheckLogic cl = aiService.checkLogic(id);
        assertFalse(cl.items().isEmpty(), "应交项校验逻辑不应为空");
        assertTrue(cl.items().stream().anyMatch(i -> i.ruleDetail() != null && !i.ruleDetail().isBlank()),
                "应暴露规则明细(detail)");
        assertTrue(cl.items().stream().anyMatch(i -> "A".equals(i.code())), "A 来源识别命中应在校验逻辑中");
        assertTrue(cl.items().stream().anyMatch(i -> i.triggerLabel() != null && i.triggerLabel().contains("命中")),
                "应暴露校验逻辑(触发规则人读标签)");
        assertNotNull(cl.aiModel(), "应标注模型");
    }
}
