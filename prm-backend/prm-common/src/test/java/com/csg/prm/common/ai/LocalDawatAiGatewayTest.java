package com.csg.prm.common.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 大瓦特 AI 网关本地桩契约测试:四项能力返回结构完整、置信度合法、规则确定。
 */
class LocalDawatAiGatewayTest {

    private final DawatAiGateway ai = new LocalDawatAiGateway();

    @Test
    void ocr_ownership_extracts_structured_fields() {
        DawatAiGateway.OcrOwnership r = ai.recognizeOwnership("https://oss/材料/客户用电信息表-经营.pdf");
        assertNotNull(r.assetName());
        assertNotNull(r.rightHolder());
        assertEquals("经营权", r.rightType(), "文件名含'经营'应识别为产品经营权");
        assertTrue(r.confidence() > 0 && r.confidence() <= 1);
    }

    @Test
    void conflict_detection_is_deterministic() {
        DawatAiGateway.ConflictResult conflict =
                ai.detectConflict("CONFLICT-DA-1", "甲公司", "持有权");
        assertTrue(conflict.hasConflict());
        assertEquals("高", conflict.riskLevel());
        assertFalse(conflict.conflicts().isEmpty());

        DawatAiGateway.ConflictResult clean =
                ai.detectConflict("DA-NORMAL-1", "甲公司", "持有权");
        assertFalse(clean.hasConflict());
        assertEquals("低", clean.riskLevel());
    }

    @Test
    void auth_intent_extracts_elements() {
        DawatAiGateway.AuthIntent r =
                ai.recognizeAuthIntent("拟批量向广州供电局开放对外经营,用于电力金融征信全字段");
        assertEquals("经营权", r.rightType(), "含'经营/对外'应为产品经营权");
        assertEquals("批量", r.mode());
        assertEquals("电力金融征信", r.scenario());
        assertEquals("全字段", r.scope());
        assertTrue(r.granteeOrg().contains("供电局"));
        assertNotNull(r.suggestion());
    }

    @Test
    void rag_answer_hits_knowledge_and_cites() {
        DawatAiGateway.RagAnswer a = ai.ask("数据确权的流程节点有哪些?");
        assertTrue(a.answer().contains("八节点") || a.answer().contains("确权"));
        assertFalse(a.citations().isEmpty());
        assertTrue(a.confidence() >= 0.8);

        DawatAiGateway.RagAnswer fallback = ai.ask("今天天气如何");
        assertTrue(fallback.confidence() < 0.8, "未命中知识应低置信度并提示细化");
    }
}
