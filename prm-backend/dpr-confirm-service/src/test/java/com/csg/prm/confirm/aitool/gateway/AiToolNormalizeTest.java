package com.csg.prm.confirm.aitool.gateway;

import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway.ParsedElements;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** #1/#7 抽取结果保真归一:权利主体以正文标注为准,并对模型损坏输出兜底。 */
class AiToolNormalizeTest {

    private static ParsedElements of(String subject) {
        return new ParsedElements(subject, "obj", "数据持有权", "3年", "约定字段",
                "自行生产", "电网生产数据", "未检出", "", 0.95);
    }

    @Test
    @DisplayName("正文标注'权利主体:X' → 以标注覆盖模型主体")
    void labeledSubjectOverrides() {
        ParsedElements e = AiToolParseGateway.normalize(of("中国南方电网有限责任公司"),
                "权利主体:广州供电局。数据加工使用权授权材料。");
        assertEquals("广州供电局", e.rightSubject());
    }

    @Test
    @DisplayName("全角冒号同样识别标注主体")
    void labeledSubjectFullwidthColon() {
        ParsedElements e = AiToolParseGateway.normalize(of("x"), "权属主体：深圳供电局，其余……");
        assertEquals("深圳供电局", e.rightSubject());
    }

    @Test
    @DisplayName("模型主体损坏(孤立代理对)且无标注 → 退回安全默认")
    void garbledSubjectFallsBack() {
        ParsedElements e = AiToolParseGateway.normalize(of("驞垮窞\udc80"), "本材料为确权证明,数据来源自行生产。");
        assertEquals("中国南方电网有限责任公司", e.rightSubject());
    }

    @Test
    @DisplayName("无标注且模型主体正常 → 原样保留")
    void cleanSubjectKept() {
        ParsedElements e = AiToolParseGateway.normalize(of("广东电网有限责任公司"), "数据持有权证明,自行生产。");
        assertEquals("广东电网有限责任公司", e.rightSubject());
    }
}
