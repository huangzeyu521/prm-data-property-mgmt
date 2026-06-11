package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitDecision;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.service.AitConflictService;
import com.csg.prm.confirm.aitool.service.AitDecisionService;
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
 * 智能确权辅助工具 M3 确权决策支持测试:因子加权 + 结果预测 + 分割 + 证据链(整合 M1/M2)。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitDecisionTest {

    @Autowired
    private AitDecisionService decisionService;
    @Autowired
    private AitMaterialService materialService;
    @Autowired
    private AitConflictService conflictService;
    @Autowired
    private ConfirmApplyService applyService;

    private String newApply(String asset) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(asset);
        a.setAssetName(asset + "表");
        a.setRightType("数据持有权");
        a.setRightHolder("广东电网");
        return applyService.saveDraft(a);
    }

    @Test
    void clean_material_no_conflict_should_suggest_pass() {
        String asset = "DA-DEC-1";
        String applyId = newApply(asset);
        AitMaterial m = new AitMaterial();
        m.setFileName(asset + "-确权证明-盖章.pdf");
        m.setApplyId(applyId);
        m.setContent("数据持有权,广东电网,有效期3年,自行生产,已盖章");
        String mid = materialService.upload(m);
        materialService.parse(mid);

        AitDecision d = decisionService.analyze(applyId);
        assertEquals(AitDecision.PRED_PASS, d.getPrediction(), "材料齐备且无冲突应建议通过");
        assertTrue(d.getScore() >= 80);
        assertNotNull(d.getEvidenceChain());
        assertTrue(d.getFactorsJson().contains("材料完整性"));
        assertTrue(d.getFactorsJson().contains("\"reason\""), "因子应带得分依据说明");
        assertTrue(d.getFactorsJson().contains("已解析1份"), "材料完整性依据应含解析数量");
        assertTrue(d.getFactorsJson().contains("无未处置权属冲突"), "无冲突时应说明依据");
        assertTrue(d.getRagAdvice() != null && !d.getRagAdvice().isEmpty());
    }

    @Test
    void rag_generates_ai_prediction_with_citations() {
        String asset = "DA-DEC-4";
        String applyId = newApply(asset);
        AitMaterial m = new AitMaterial();
        m.setFileName(asset + "-确权证明-盖章.pdf");
        m.setApplyId(applyId);
        m.setContent("数据持有权,广东电网,有效期3年,自行生产,已盖章");
        materialService.parse(materialService.upload(m));

        AitDecision d = decisionService.analyze(applyId);
        assertNotNull(d.getAiPrediction(), "RAG 应产出 AI 预测结论");
        assertEquals(AitDecision.PRED_PASS, d.getAiPrediction(), "干净案例 AI 预测应为建议通过");
        assertEquals(d.getPrediction(), d.getAiPrediction(), "干净案例 AI 预测应与规则预测一致");
        assertNotNull(d.getRagCitations());
        assertTrue(d.getRagCitations().contains("数据二十条"), "引用应含《数据二十条》");
        assertTrue(d.getRagAdvice().contains("建议"), "RAG 建议应含预测结论表述");
    }

    @Test
    void no_material_should_suggest_supplement() {
        String applyId = newApply("DA-DEC-2");
        AitDecision d = decisionService.analyze(applyId);
        assertEquals(AitDecision.PRED_SUPPLEMENT, d.getPrediction(), "无材料应建议补充材料");
        assertTrue(d.getSupplementMaterials().contains("补充"));
    }

    @Test
    void multi_subject_generates_dual_split_plans() {
        String asset = "DA-DEC-5";
        String applyId = newApply(asset);
        AitKgClaim a = new AitKgClaim();
        a.setAssetId(asset); a.setSubject("广东电网"); a.setRightType("数据持有权");
        a.setAuthScope("用电量、负荷"); a.setValidDate(LocalDateTime.now().plusYears(2));
        a.setSourceType(AitKgClaim.SRC_CURRENT);
        conflictService.addClaim(a);
        AitKgClaim b = new AitKgClaim();
        b.setAssetId(asset); b.setSubject("深圳供电局"); b.setRightType("数据加工使用权");
        b.setAuthScope("负荷、电压"); b.setValidDate(LocalDateTime.now().plusYears(1));
        b.setSourceType(AitKgClaim.SRC_HISTORY);
        conflictService.addClaim(b);

        AitDecision d = decisionService.analyze(applyId);
        assertTrue(d.getSplitPlan().contains("推荐"), "摘要应含推荐方案");
        assertTrue(d.getSplitPlan().contains("按比例"), "负荷字段重叠应推荐按比例");
        String plans = d.getSplitPlansJson();
        assertTrue(plans.contains("按业务范围") && plans.contains("按比例"), "应生成双方案");
        assertTrue(plans.contains("广东电网") && plans.contains("深圳供电局"), "应覆盖全部主体");
        assertTrue(plans.contains("重叠:负荷"), "应标注具体重叠字段");
        assertTrue(plans.contains("至2"), "应含使用期限");
        assertTrue(plans.contains("数据质量与安全主体责任") && plans.contains("加工合规"), "应含责任划分");
        assertTrue(plans.contains("%"), "按比例方案应含比例");
    }

    @Test
    void single_subject_needs_no_split() {
        String applyId = newApply("DA-DEC-6");
        AitDecision d = decisionService.analyze(applyId);
        assertTrue(d.getSplitPlan().contains("无需分割"));
        assertEquals("[]", d.getSplitPlansJson());
    }

    @Test
    void high_risk_conflicts_should_lower_decision() {
        String asset = "DA-DEC-3";
        String applyId = newApply(asset);
        AitMaterial m = new AitMaterial();
        m.setFileName(asset + "-盖章.pdf");
        m.setApplyId(applyId);
        m.setContent("数据持有权,已盖章");
        materialService.parse(materialService.upload(m));
        // 制造两个高风险冲突(历史所有权 + 当前持有权矛盾 等)
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

        AitDecision d = decisionService.analyze(applyId);
        assertTrue(d.getPendingConflicts().contains("冲突"), "应反映待处置冲突");
        assertTrue(!AitDecision.PRED_PASS.equals(d.getPrediction()), "存在冲突不应直接建议通过");
    }
}
