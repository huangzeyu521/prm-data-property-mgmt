package com.csg.prm.confirm.integration;

import com.csg.prm.confirm.integration.dto.PlatformTableMeta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 选卡片→自动带库表清单(确权粒度到库表)。平台未接入(Stub)时走服务层桩合成,
 * 验证:已知演示资产回放联调样例、通用资产合成多表、空 assetId 返回空。
 */
@SpringBootTest
@ActiveProfiles("test")
class AssetTableMetaServiceTest {

    @Autowired private AssetTableMetaService service;

    /** 已知演示资产 AST-001:回放客户用电信息表样例(实例/schema/表/密级/来源全带出)。 */
    @Test
    void demo_asset_replays_sample_table() {
        List<PlatformTableMeta> tables = service.listTableMeta("AST-001", "客户用电信息表");
        assertEquals(1, tables.size(), "AST-001 应带出 1 张库表");
        PlatformTableMeta t = tables.get(0);
        assertEquals("MKT_DB01", t.instanceName());
        assertEquals("MKT", t.schemaName());
        assertEquals("C_CONS_ELEC_INFO", t.tableCode());
        assertEquals("客户用电信息表", t.tableName());
        assertEquals("敏感信息", t.secretLevel());
        assertEquals("A 自行生产数据", t.sourceType(), "演示资产来源判定已采集");
        assertEquals("广东电网有限责任公司", t.sourceSubject());
        assertTrue(t.existTable());
    }

    /** 通用资产:合成一个系统下多张库表(体现确权粒度到库表),来源判定留空待补全。 */
    @Test
    void generic_asset_synthesizes_multiple_tables() {
        List<PlatformTableMeta> tables = service.listTableMeta("SYS-XYZ", "营销档案");
        assertEquals(2, tables.size(), "通用资产应合成多张库表");
        assertTrue(tables.stream().allMatch(t -> "SYSXYZ_DB01".equals(t.instanceName())), "同系统同实例");
        assertTrue(tables.stream().anyMatch(t -> t.tableCode().endsWith("_MAIN")));
        assertTrue(tables.stream().anyMatch(t -> t.tableCode().endsWith("_DIM")));
        assertTrue(tables.stream().allMatch(t -> t.tableName().contains("营销档案")), "表名含资产名");
        assertTrue(tables.stream().allMatch(PlatformTableMeta::existTable));
        assertTrue(tables.stream().allMatch(t -> t.sourceType() == null), "通用合成来源判定留空待用户/AI补全");
    }

    /** 空 assetId:无源可带,返回空清单(前端回退批量导入兜底)。 */
    @Test
    void blank_asset_returns_empty() {
        assertTrue(service.listTableMeta("", "任意").isEmpty());
        assertTrue(service.listTableMeta(null, null).isEmpty());
    }
}
