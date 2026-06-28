package com.csg.prm.authorize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议模板库种子对齐契约锁(防回归,资源内容级)。
 *
 * 协议=附录D《运营授权协议》,按授权方式(一事一议/批量)分;协议须约定 §3.4.4 五要素
 * (数据范围/使用场景及目的/利益分配/安全保障)。历史种子按 独占/共享/委托/运营,且内容漏 利益分配/场景。
 * 本测试读 data.sql 锁:协议模板种子=一事一议/批量、不回退旧分类、内容覆盖 §3.4.4 五要素。
 */
class AgreementTemplateSeedAlignmentTest {

    private String seedSection() throws Exception {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/db/h2/data.sql")) {
            assertNotNull(in, "data.sql 应在类路径");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int i = sql.indexOf("INSERT INTO IM_AUTH_AGREEMENT_TEMPLATE");
        assertTrue(i >= 0, "应有协议模板种子");
        int j = sql.indexOf("INSERT INTO", i + 20);
        return j > i ? sql.substring(i, j) : sql.substring(i);
    }

    @Test
    @DisplayName("协议模板种子按 一事一议/批量 分类,不回退 独占/共享/委托")
    void seed_useAuthModeNotLegacy() throws Exception {
        String seg = seedSection();
        assertTrue(seg.contains("'一事一议'"), "应含一事一议(专项)运营协议模板");
        assertTrue(seg.contains("'批量'"), "应含批量(一清单一协议)运营协议模板");
        assertFalse(seg.contains("'独占'"), "不得保留旧分类『独占』");
        assertFalse(seg.contains("'共享'"), "不得保留旧分类『共享』");
        assertFalse(seg.contains("'委托'"), "不得保留旧分类『委托』");
    }

    @Test
    @DisplayName("协议模板内容覆盖附录D §3.4.4 五要素(数据范围/使用场景及目的/利益分配/安全保障)")
    void seed_coversAgreementElements() throws Exception {
        String seg = seedSection();
        assertTrue(seg.contains("数据范围"), "§3.4.4 数据范围");
        assertTrue(seg.contains("使用场景及目的"), "§3.4.4 使用场景及目的");
        assertTrue(seg.contains("利益分配"), "§3.4.4 利益分配");
        assertTrue(seg.contains("安全保障"), "§3.4.4 安全保障");
        assertTrue(seg.contains("备案"), "经营权对外提供须备案(附录G)");
    }
}
