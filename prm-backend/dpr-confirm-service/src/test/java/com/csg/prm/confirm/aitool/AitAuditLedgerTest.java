package com.csg.prm.confirm.aitool;

import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.aitool.entity.AitAuditResult;
import com.csg.prm.confirm.aitool.entity.AitEvidence;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.service.AitAuditAgentService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 3.2 审核结果生成与台账管理(#1~#5)能力测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitAuditLedgerTest {

    @Autowired private AitAuditAgentService agent;
    @Autowired private AitMaterialService materialService;
    @Autowired private ConfirmApplyService applyService;

    private String newApply(String asset) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(asset);
        a.setAssetName(asset + "数据集");
        a.setRightType("数据持有权");
        a.setRightHolder("广东电网");
        return applyService.saveDraft(a);
    }

    private void sealedMaterial(String applyId, String name) {
        AitMaterial m = new AitMaterial();
        m.setFileName(name);
        m.setApplyId(applyId);
        m.setContent("数据持有权,广东电网,自行生产,有效期3年,已盖章");
        materialService.parse(materialService.upload(m));
    }

    /** #1 批量审核形成台账;#2 授权级别等结构化内容。 */
    @Test
    void batch_audit_builds_ledger() {
        String a1 = "DA-LED-A-" + System.nanoTime();
        String a2 = "DA-LED-B-" + System.nanoTime();
        String id1 = newApply(a1);
        String id2 = newApply(a2);
        sealedMaterial(id1, a1 + "-盖章.pdf");
        sealedMaterial(id2, a2 + "-盖章.pdf");

        List<AitAuditResult> batch = agent.batchAudit(List.of(id1, id2));
        assertEquals(2, batch.size(), "批量审核应产出 2 条");
        assertNotNull(batch.get(0).getAuthLevel(), "应输出授权级别");

        assertTrue(agent.ledgerPage(new PageRequest(), null, null, null, null, null).getTotal() >= 2, "台账应可分页查到");
        // 字段级结论端点可用(可空)
        assertNotNull(agent.fieldLevel(id1));
    }

    /** #4 审核证据链:材料片段 + 规则命中 + 模型理由 + 结论 + SM3 留痕。 */
    @Test
    void evidence_chain_archived() {
        String asset = "DA-LED-EV-" + System.nanoTime();
        String applyId = newApply(asset); // 不传材料 → 深度审核,规则命中(未上传材料)
        agent.audit(applyId);

        AitEvidence ev = agent.getEvidence(applyId);
        assertNotNull(ev.getRuleHitsJson());
        assertTrue(ev.getRuleHitsJson().contains("未上传") || ev.getRuleHitsJson().contains("规则"), "应记录规则命中项");
        assertNotNull(ev.getModelReason(), "应记录模型理由");
        assertNotNull(ev.getConclusion(), "应记录最终结论");
        assertTrue(ev.getSm3Hash() != null && ev.getSm3Hash().startsWith("0x"), "应有 SM3 留痕");
    }

    /** #3 审核报告 / 确权登记辅助 / 法律意见辅助 标准化输出。 */
    @Test
    void standardized_documents_generated() {
        String asset = "DA-LED-DOC-" + System.nanoTime();
        String applyId = newApply(asset);
        sealedMaterial(applyId, asset + "-盖章.pdf");
        agent.audit(applyId);

        assertTrue(agent.exportReportWord(applyId).length > 0, "审核报告应可导出");
        assertTrue(agent.registrationDoc(applyId).length > 0, "确权登记辅助材料应可生成");
        assertTrue(agent.legalOpinion(applyId).length > 0, "法律意见辅助材料应可生成");
    }

    /** #5 汇总统计 + Excel 导出 + 多维筛选。 */
    @Test
    void ledger_stats_filter_and_export() {
        String asset = "DA-LED-ST-" + System.nanoTime();
        String dept = "数字化部-" + System.nanoTime();
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(asset);
        a.setAssetName(asset + "数据集");
        a.setRightType("数据持有权");
        a.setRightHolder("广东电网");
        a.setRespDept(dept); // #5 系统/责任部门维度
        String applyId = applyService.saveDraft(a);
        sealedMaterial(applyId, asset + "-盖章.pdf");
        agent.audit(applyId);

        Map<String, Object> stats = agent.ledgerStats();
        assertTrue(((Number) stats.get("total")).intValue() >= 1);
        assertFalse(((Map<?, ?>) stats.get("byRisk")).isEmpty(), "应有按风险等级汇总");
        assertFalse(((Map<?, ?>) stats.get("byChannel")).isEmpty(), "应有按通道汇总");
        assertFalse(((Map<?, ?>) stats.get("byDataClass")).isEmpty(), "应有按业务域汇总");
        Map<?, ?> byDept = (Map<?, ?>) stats.get("byDept");
        assertNotNull(byDept, "应有按系统/责任部门汇总维度");
        assertTrue(byDept.containsKey(dept), "byDept 应含该申请的责任部门 " + dept);

        assertTrue(agent.exportLedgerExcel(null, null, null, null, null).length > 0, "台账应可导出 Excel");
        // 按通道筛选
        assertTrue(agent.ledgerPage(new PageRequest(), null, null, null, AitAuditResult.CH_FAST, null).getTotal() >= 1,
                "应能按快速通道筛选");
    }
}
