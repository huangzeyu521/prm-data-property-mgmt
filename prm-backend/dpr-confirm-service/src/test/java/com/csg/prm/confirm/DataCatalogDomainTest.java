package com.csg.prm.confirm;

import com.csg.prm.confirm.integration.DataCatalogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 系统→业务域解析单测(纯单元):支撑授权资源池「所属业务域」按系统逐表带出(表5/表6)。
 * 不硬编码具体目录内容,改为自洽校验(map 与单查一致、未知/空返回空),抗 CATALOG 变更。
 */
class DataCatalogDomainTest {

    private final DataCatalogService svc = new DataCatalogService();

    @Test
    @DisplayName("systemDomainMap 非空,且 domainOfSystem 与之逐项一致")
    void systemDomainMap_consistentWithDomainOfSystem() {
        Map<String, String> map = svc.systemDomainMap();
        assertFalse(map.isEmpty(), "目录应至少含一个系统→业务域映射");
        for (Map.Entry<String, String> e : map.entrySet()) {
            assertTrue(e.getValue() != null && !e.getValue().isBlank(), "每个系统都应有业务域");
            assertEquals(e.getValue(), svc.domainOfSystem(e.getKey()), "单查应与映射表一致:" + e.getKey());
        }
    }

    @Test
    @DisplayName("未知系统 / 空入参 → 业务域为空串(不抛异常)")
    void domainOfSystem_unknownOrBlank_returnsEmpty() {
        assertEquals("", svc.domainOfSystem("不存在的系统XYZ"));
        assertEquals("", svc.domainOfSystem(null));
        assertEquals("", svc.domainOfSystem("  "));
    }
}
