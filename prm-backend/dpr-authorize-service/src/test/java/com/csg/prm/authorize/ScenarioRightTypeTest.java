package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthScenario;
import com.csg.prm.authorize.service.AuthScenarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应用场景「适用权益类型」契约锁(防回归)。
 *
 * 场景配置加 rightType(使用权/经营权/通用),供一站式向导按授权权益类型过滤场景。
 * 本测试锁:① rightType 往返 + 分页按 rightType 过滤;② 种子场景已按用途打标(资源内容级)。
 */
@SpringBootTest
@ActiveProfiles("test")
class ScenarioRightTypeTest {

    @Autowired private AuthScenarioService scenarioService;

    @Test
    @DisplayName("场景 rightType 往返 + 分页按适用权益类型过滤")
    void scenario_rightType_persistAndFilter() {
        AuthScenario s = new AuthScenario();
        s.setScenarioName("对外征信服务-" + System.nanoTime());
        s.setCategory("对外服务");
        s.setRightType("数据产品经营权");
        s.setReasonTemplate("对外提供征信数据产品服务");
        String id = scenarioService.create(s);
        assertNotNull(id);

        // 按 经营权 过滤应命中;按 使用权 过滤不应命中(同名)
        List<AuthScenario> hitOp = scenarioService.page(1, 500, null, null, null, "数据产品经营权").getRecords();
        assertTrue(hitOp.stream().anyMatch(x -> id.equals(x.getScenarioId()) && "数据产品经营权".equals(x.getRightType())),
                "经营权过滤应命中且 rightType 往返保留");
        List<AuthScenario> hitUse = scenarioService.page(1, 500, null, null, null, "数据加工使用权").getRecords();
        assertTrue(hitUse.stream().noneMatch(x -> id.equals(x.getScenarioId())),
                "使用权过滤不应命中经营权场景(向导据此过滤,防经营权×内部场景错配)");
    }

    @Test
    @DisplayName("种子场景已按适用权益类型打标(对外→经营权、内部→使用权)")
    void seedScenarios_taggedWithRightType() throws Exception {
        String sql;
        try (InputStream in = getClass().getResourceAsStream("/db/h2/data.sql")) {
            assertNotNull(in, "data.sql 应在类路径");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int i = sql.indexOf("INSERT INTO IM_AUTH_SCENARIO");
        assertTrue(i >= 0, "应有应用场景种子");
        int j = sql.indexOf("INSERT INTO", i + 20);
        String seg = j > i ? sql.substring(i, j) : sql.substring(i);
        assertTrue(seg.contains("CEC_RIGHT_TYPE"), "场景种子应含适用权益类型列");
        assertTrue(seg.contains("数据产品经营权"), "应有经营权场景(对外服务)");
        assertTrue(seg.contains("数据加工使用权"), "应有使用权场景(内部/联合建模)");
    }
}
