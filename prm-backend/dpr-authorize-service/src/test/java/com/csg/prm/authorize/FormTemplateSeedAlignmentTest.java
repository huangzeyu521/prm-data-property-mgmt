package com.csg.prm.authorize;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权申请表单模板种子对齐契约锁(防回归,资源内容级)。
 *
 * 「授权申请表单设计管理」按授权方式(一事一议/批量)配置申请表单,与两个一站式向导同源。
 * 历史:种子模板曾按 独占/共享/委托(泛化分类,非 35号文)→ 与真实申请表单(表5/表6)脱节。
 * 本测试读 data.sql,锁定种子表单模板:授权方式=一事一议/批量、含表5/§3.4.4 关键字段、不得回退到旧分类。
 * (authorize 测试 profile 不灌 data.sql,故走资源内容断言而非 DB 查询。)
 */
class FormTemplateSeedAlignmentTest {

    private String readDataSql() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/h2/data.sql")) {
            assertNotNull(in, "data.sql 应在类路径");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 截取 IM_AUTH_APPLY_TEMPLATE 的 INSERT 段(到下一个 INSERT 或注释块)。 */
    private String templateSeedSection(String sql) {
        int i = sql.indexOf("INSERT INTO IM_AUTH_APPLY_TEMPLATE");
        assertTrue(i >= 0, "应有申请表单模板种子");
        int j = sql.indexOf("INSERT INTO", i + 20);
        return j > i ? sql.substring(i, j) : sql.substring(i);
    }

    @Test
    @DisplayName("种子表单模板按 一事一议/批量 分类,不得回退到 独占/共享/委托")
    void seedTemplates_useAuthModeNotLegacyTaxonomy() {
        String seed = templateSeedSection(readDataSqlQuiet());
        assertTrue(seed.contains("'一事一议'"), "应含一事一议(专项,表5)申请表单模板");
        assertTrue(seed.contains("'批量'"), "应含批量(表6)申请表单模板");
        assertFalse(seed.contains("'独占'"), "不得保留旧泛化分类『独占』(与 35号文授权方式脱节)");
        assertFalse(seed.contains("'共享'"), "不得保留旧泛化分类『共享』");
        assertFalse(seed.contains("'委托'"), "不得保留旧泛化分类『委托』");
    }

    @Test
    @DisplayName("一事一议模板字段覆盖表5/§3.4.4 关键项(模式名称/利益分配/安全保障/生效卡片)")
    void specialTemplate_coversTable5AndAgreementFields() {
        String seed = templateSeedSection(readDataSqlQuiet());
        assertTrue(seed.contains("schemaName"), "表5 模式名称字段");
        assertTrue(seed.contains("equityCardId"), "先确后授 生效卡片字段");
        assertTrue(seed.contains("scenario"), "使用场景及目的字段");
        assertTrue(seed.contains("benefitAllocation"), "§3.4.4 利益分配字段");
        assertTrue(seed.contains("securityReq"), "§3.4.4 安全保障字段");
    }

    private String readDataSqlQuiet() {
        try {
            return readDataSql();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
