package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitAuditResult;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.service.AitAuditAgentService;
import com.csg.prm.confirm.aitool.service.AitConflictService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 3.1 智能确权审核与推理决策(Agent)#1~#5 能力测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitAuditAgentTest {

    @Autowired private AitAuditAgentService agent;
    @Autowired private AitMaterialService materialService;
    @Autowired private AitConflictService conflictService;
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

    /** #2 低风险走快速通道直出结论;#5 结构化输出齐全。 */
    @Test
    void low_risk_fast_channel() {
        String asset = "DA-AGENT-FAST-" + System.nanoTime();
        String applyId = newApply(asset);
        sealedMaterial(applyId, asset + "-确权证明-盖章.pdf");

        AitAuditResult r = agent.audit(applyId);
        assertEquals(AitAuditResult.CH_FAST, r.getChannel(), "材料齐备无冲突应走快速通道");
        assertEquals("建议通过", r.getAuthAdvice());
        assertEquals("低", r.getRiskLevel());
        // #5 结构化输出
        assertNotNull(r.getDataClass());
        assertNotNull(r.getDataGrade());
        assertNotNull(r.getRestrictions());
        assertNotNull(r.getAction());
        assertNotNull(r.getReason());
        // #1 多阶段链路(结论生成阶段)
        assertTrue(r.getStageTraceJson().contains("数据分类分级"));
        assertTrue(r.getStageTraceJson().contains("结论生成"));
    }

    /** #1 五阶段链路;#3 深度审核(知识召回+模型研判+结果校核);#4 工具编排。 */
    @Test
    void complex_deep_channel_multi_stage() {
        String asset = "DA-AGENT-DEEP-" + System.nanoTime();
        String applyId = newApply(asset);
        sealedMaterial(applyId, asset + "-盖章.pdf");
        // 制造高风险冲突
        AitKgClaim hist = new AitKgClaim();
        hist.setAssetId(asset); hist.setSubject("深圳供电局"); hist.setRightType("所有权");
        hist.setAuthScope("全字段"); hist.setExclusive(true);
        hist.setValidDate(LocalDateTime.now().plusYears(1)); hist.setSourceType(AitKgClaim.SRC_HISTORY);
        conflictService.addClaim(hist);
        AitKgClaim cur = new AitKgClaim();
        cur.setAssetId(asset); cur.setSubject("广东电网"); cur.setRightType("数据持有权");
        cur.setAuthScope("全字段"); cur.setExclusive(true);
        cur.setValidDate(LocalDateTime.now().plusYears(2)); cur.setSourceType(AitKgClaim.SRC_CURRENT);
        conflictService.addClaim(cur);
        conflictService.detect(cur);

        AitAuditResult r = agent.audit(applyId);
        assertEquals(AitAuditResult.CH_DEEP, r.getChannel(), "有高风险冲突应走深度审核");
        assertEquals("高", r.getRiskLevel());
        // #1/#3 阶段链路含 知识召回/模型研判/结果校核
        String st = r.getStageTraceJson();
        assertTrue(st.contains("数据分类分级") && st.contains("合规扫描") && st.contains("授权判断")
                && st.contains("风险识别") && st.contains("结论生成"), "应有五阶段");
        assertTrue(st.contains("知识召回") && st.contains("模型研判") && st.contains("结果校核"), "深度审核应含多级机制");
        // #4 工具编排:知识库检索 + 大模型研判
        assertTrue(r.getToolTraceJson().contains("知识库检索") && r.getToolTraceJson().contains("大模型研判"),
                "应编排知识库与大模型工具");
        assertNotNull(r.getAuthAdvice());
        assertNotNull(r.getCitations());

        // 可回查
        assertEquals(r.getAuditId(), agent.getByApply(applyId).getAuditId());
    }
}
