package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import com.csg.prm.confirm.aitool.gateway.QwenAiToolParseGateway;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * qwen 解析网关 JSON 映射离线单测(不联网):校验 qwen 返回到确权要素的解析与容错。
 */
class QwenParseMapTest {

    @Test
    void map_qwen_json_with_code_fence() {
        String out = "```json\n{\"rightSubject\":\"广东电网\",\"rightObject\":\"客户用电信息\","
                + "\"rightType\":\"数据持有权\",\"rightTerm\":\"3年\",\"authScope\":\"约定字段\","
                + "\"dataSource\":\"自行生产\",\"sensitiveType\":\"个人信息\",\"sealValid\":\"有效\","
                + "\"sealDesc\":\"检出公章\",\"confidence\":0.95}\n```";
        AiToolParseGateway.ParsedElements e = QwenAiToolParseGateway.mapElements(out);
        assertEquals("广东电网", e.rightSubject());
        assertEquals("数据持有权", e.rightType());
        assertEquals("个人信息", e.sensitiveType());
        assertEquals("有效", e.sealValid());
        assertEquals(0.95, e.confidence(), 0.001);
    }

    @Test
    void confidence_clamped_and_defaults_applied() {
        AiToolParseGateway.ParsedElements e = QwenAiToolParseGateway.mapElements(
                "{\"rightSubject\":\"南网\",\"confidence\":1.8}");
        assertEquals(1.0, e.confidence(), 0.001);
        assertEquals("数据持有权", e.rightType(), "缺省字段应取默认");
    }

    @Test
    void non_json_throws_for_fallback() {
        assertThrows(IllegalArgumentException.class, () -> QwenAiToolParseGateway.mapElements("抱歉无法解析"));
    }
}
