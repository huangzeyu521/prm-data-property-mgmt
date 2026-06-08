package com.csg.prm.common.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Qwen 返回 JSON 解析器单测(离线):标准 JSON、带代码块包裹、字段归一化、异常兜底。
 */
class QwenResponseParserTest {

    @Test
    void parse_ownership_with_code_fence() {
        String content = "```json\n{\"assetName\":\"客户用电信息表\",\"rightHolder\":\"南网\","
                + "\"rightType\":\"产品经营权\",\"respDept\":\"数字化部\",\"confidence\":0.91,\"rawText\":\"ok\"}\n```";
        DawatAiGateway.OcrOwnership r = QwenResponseParser.ownership(content);
        assertEquals("客户用电信息表", r.assetName());
        assertEquals("数据产品经营权", r.rightType(), "rightType 应归一化为标准权属名");
        assertEquals(0.91, r.confidence(), 0.001);
    }

    @Test
    void parse_conflict_with_prefix_text() {
        String content = "分析如下：{\"hasConflict\":true,\"riskLevel\":\"高\","
                + "\"conflicts\":[\"权属边界重叠\",\"登记不一致\"],\"suggestion\":\"提交合规裁定\"}";
        DawatAiGateway.ConflictResult r = QwenResponseParser.conflict(content);
        assertTrue(r.hasConflict());
        assertEquals("高", r.riskLevel());
        assertEquals(2, r.conflicts().size());
    }

    @Test
    void parse_intent_and_confidence_clamped() {
        String content = "{\"granteeOrg\":\"广州供电局\",\"rightType\":\"加工使用\",\"scenario\":\"征信\","
                + "\"scope\":\"全字段\",\"mode\":\"批量\",\"suggestion\":\"按批量授权\",\"confidence\":1.7}";
        DawatAiGateway.AuthIntent r = QwenResponseParser.intent(content);
        assertEquals("广州供电局", r.granteeOrg());
        assertEquals("数据加工使用权", r.rightType());
        assertEquals("批量", r.mode());
        assertEquals(1.0, r.confidence(), 0.001, "置信度应被夹到 [0,1]");
    }

    @Test
    void parse_rag_answer() {
        String content = "{\"answer\":\"按八节点流程办理\",\"citations\":[\"附录F 4.1\"],\"confidence\":0.9}";
        DawatAiGateway.RagAnswer r = QwenResponseParser.rag(content);
        assertTrue(r.answer().contains("八节点"));
        assertEquals(1, r.citations().size());
    }

    @Test
    void non_json_throws_for_fallback() {
        assertThrows(IllegalArgumentException.class, () -> QwenResponseParser.rag("抱歉我无法回答"));
    }
}
