package com.csg.prm.confirm.integration.dto;

/**
 * 平台元数据库表(供确权"选卡片→自动带库表清单"用)。
 * 字段来源(平台接入后映射):
 * - instanceName/schemaName/tableCode/tableName/tableComment/secretLevel ← TW_DATA_CARD(数据资产卡片,一卡≈一库表)
 * - sourceType(A-F)/sourceSubject ← AU_TABLE_META_DATA(产权元数据表信息,已采集则带出,未采集为 null 待补全)
 * - existTable ← TW_DATA_CARD.IS_EXIST_TABLE(不存在库表的不允许确权)
 */
public record PlatformTableMeta(
        String instanceName,   // 实例/TNS
        String schemaName,     // 模式/schema
        String tableCode,      // 表代码
        String tableName,      // 表名称
        String tableComment,   // 表注释
        String secretLevel,    // 密级
        String sourceType,     // 数据来源判定(A-F),平台元数据已采集则带出
        String sourceSubject,  // 来源主体名称
        boolean existTable      // 是否存在库表(false=不可确权)
) {
}
