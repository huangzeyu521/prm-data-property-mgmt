package com.csg.prm.confirm.integration.dto;

/**
 * 数据资产卡片引用(供"按名称/编码/系统·表 搜索选取卡片"用)。
 * assetId = 平台卡片主键 TW_DATA_CARD.ID(稳定关联键);其余为可读展示字段(选卡后只读带出)。
 */
public record PlatformCardRef(
        String assetId,        // = TW_DATA_CARD.ID(关联键)
        String assetName,      // = BSC_NAME(卡片名称)
        String cardCode,       // = BSC_CODE(卡片编码)
        String systemName,     // = MGT_SYS_NAME(所属业务系统)
        String instanceName,   // = INSTANCE_NAME(实例)
        String schemaName,     // = SCHEMA_NAME(模式)
        String tableName       // = 库表名(可同 assetName)
) {
}
