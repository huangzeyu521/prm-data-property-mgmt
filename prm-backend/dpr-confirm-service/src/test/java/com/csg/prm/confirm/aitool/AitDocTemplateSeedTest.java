package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitDocTemplate;
import com.csg.prm.confirm.aitool.service.AitDocTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.4#3 模板库:校验从南网确权授权业务指导书(附录D/E/F/G)拆出的真实填空式模板已资源驱动幂等种入。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitDocTemplateSeedTest {

    private static final String PROTOCOL = "南方电网数据授权运营协议(示例)";
    private static final String NDA = "保密承诺函(示例)";
    private static final String RISK = "数据权益风险表(模板)";
    private static final String FILING = "分子公司对外数据经营权授权备案表(模板)";

    @Autowired private AitDocTemplateService templates;

    /** 四个指导书内嵌模板均已入库且正文非空。 */
    @Test
    void guidance_templates_seeded() {
        for (String name : List.of(PROTOCOL, NDA, RISK, FILING)) {
            List<AitDocTemplate> vs = templates.versions(name);
            assertFalse(vs.isEmpty(), "应已种入模板:" + name);
            AitDocTemplate t = vs.get(0);
            assertTrue(t.getContent() != null && t.getContent().length() > 20, "模板正文应非空:" + name);
            assertTrue(Boolean.TRUE.equals(t.getIsLatest()), "应为最新版:" + name);
        }
    }

    /** 协议/承诺函正文为指导书真实条款,关键锚点可命中。 */
    @Test
    void protocol_and_nda_carry_real_clauses() {
        String protocol = templates.versions(PROTOCOL).get(0).getContent();
        assertTrue(protocol.contains("授权运营协议"), "协议应含标题");
        assertTrue(protocol.contains("甲") && protocol.contains("乙"), "协议应含甲乙方");
        assertTrue(protocol.contains("授权") && protocol.contains("数据"), "协议应含授权数据条款");

        String nda = templates.versions(NDA).get(0).getContent();
        assertTrue(nda.contains("保密"), "承诺函应含保密");
        assertTrue(nda.contains("承诺"), "承诺函应含承诺");
    }

    /** 既有标准模板(内置)仍在,新增不覆盖旧模板。 */
    @Test
    void builtin_templates_intact() {
        assertFalse(templates.versions("数据确权书(标准模板)").isEmpty(), "内置确权书模板应仍在");
        assertFalse(templates.versions("数据授权函(标准模板)").isEmpty(), "内置授权函模板应仍在");
    }

    /** 二次启动 run 幂等:同名模板不重复种入。 */
    @Test
    void reseed_is_idempotent() {
        int before = templates.versions(PROTOCOL).size();
        templates.run(null);
        assertEquals(before, templates.versions(PROTOCOL).size(), "重复种入不应产生重复模板");
    }
}
