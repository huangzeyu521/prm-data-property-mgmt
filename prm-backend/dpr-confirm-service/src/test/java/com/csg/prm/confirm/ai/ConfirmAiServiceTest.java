package com.csg.prm.confirm.ai;

import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.ConfirmMaterialMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 确权内生 AI 能力:智能解析/决策研判/权属冲突识别,统一走 prm-common 共享网关(Local 桩确定性),不依赖独立工具。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmAiServiceTest {

    @Autowired private ConfirmAiService aiService;
    @Autowired private ConfirmApplyMapper applyMapper;
    @Autowired private ConfirmMaterialMapper materialMapper;

    private String seedApply(int materialCount) {
        ConfirmApply a = new ConfirmApply();
        a.setApplyNo("AP-" + System.nanoTime());
        a.setAssetId("AI-" + System.nanoTime());
        a.setAssetName("AI测试资产");
        a.setRightHolder("鼎和保险");
        a.setRightType("持有权");
        a.setStatus("草稿");
        applyMapper.insert(a);
        for (int i = 0; i < materialCount; i++) {
            ConfirmMaterial m = new ConfirmMaterial();
            m.setApplyId(a.getApplyId());
            m.setMaterialName("证明材料-" + i);
            m.setMaterialType("说明");
            m.setFileName("proof-" + i + ".docx");
            materialMapper.insert(m);
        }
        return a.getApplyId();
    }

    /** 智能解析:逐份材料产出要素行;summary 含份数。 */
    @Test
    void parse_returns_element_per_material() {
        String applyId = seedApply(2);
        ConfirmAiService.ParseResult r = aiService.parse(applyId);
        assertEquals(2, r.total());
        assertEquals(2, r.items().size());
        assertNotNull(r.summary());
        assertTrue(r.summary().contains("2"));
    }

    /** 智能解析:无材料抛业务异常。 */
    @Test
    void parse_without_material_throws() {
        String applyId = seedApply(0);
        assertThrows(BizException.class, () -> aiService.parse(applyId));
    }

    /** 权属冲突识别:返回结构化冲突结果(Local 桩确定性)。 */
    @Test
    void conflict_returns_result() {
        String applyId = seedApply(1);
        DawatAiGateway.ConflictResult c = aiService.conflict(applyId);
        assertNotNull(c, "应返回冲突检测结果");
        assertNotNull(c.riskLevel());
    }

    /** 决策研判:无材料 → 需补材料含提示且预测为"建议补充";有评分与依据。 */
    @Test
    void decision_without_material_suggests_supplement() {
        String applyId = seedApply(0);
        ConfirmAiService.DecisionResult d = aiService.decision(applyId);
        assertEquals("建议补充", d.prediction());
        assertTrue(d.supplementMaterials().stream().anyMatch(s -> s.contains("尚未上传")));
        assertTrue(d.score() >= 0 && d.score() <= 100);
        assertNotNull(d.basis());
    }

    /** 决策研判:有材料 → 预测为两种之一;依据含已上传份数。 */
    @Test
    void decision_with_material_produces_prediction() {
        String applyId = seedApply(1);
        ConfirmAiService.DecisionResult d = aiService.decision(applyId);
        assertTrue("建议通过".equals(d.prediction()) || "建议补充".equals(d.prediction()));
        assertNotNull(d.aiPrediction());
        assertTrue(d.basis().contains("已上传材料:1"));
    }

    /** 确权申请不存在 → 抛异常。 */
    @Test
    void missing_apply_throws() {
        assertThrows(BizException.class, () -> aiService.decision("NO-SUCH-APPLY"));
    }
}
