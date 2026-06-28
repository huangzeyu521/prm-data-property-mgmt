package com.csg.prm.confirm.integration.dto;

/**
 * 数据目录树节点(确权申请左侧范围树):业务域 → 系统 → 一级功能模块 → 库表(卡片)。
 * 对齐平台 TW_DATA_CARD 的 DOMAIN_2024 / MGT_SYS_NAME / FUN_LEVEL1 / 卡片 四层。
 *
 * @param id    节点唯一标识(domain=域名;system=系统名;module=系统名/模块名;table=表代码)
 * @param name  显示名
 * @param type  节点类型:domain | system | module | table
 * @param count 该节点下库表(卡片)数量
 * @param leaf  是否叶子(table=true)
 */
public record CatalogNode(
        String id,
        String name,
        String type,
        int count,
        boolean leaf,
        boolean authorized,    // 该库表是否已对外授权(table 叶子;确权变更「已授权」角标)
        boolean confirmed,     // 该库表是否已完成初始确权(table 叶子;确权变更树区分「已确权/新增」)
        int confirmedCount,    // 该系统已确权库表数(system 节点;确权进度徽标)
        int totalCount         // 该系统库表总数(system 节点)
) {
    /** 非叶/通用节点便捷构造(扩展字段默认 0/false)。 */
    public CatalogNode(String id, String name, String type, int count, boolean leaf) {
        this(id, name, type, count, leaf, false, false, 0, 0);
    }

    /** 库表叶子:authorized + confirmed。 */
    public static CatalogNode table(String id, String name, boolean authorized, boolean confirmed) {
        return new CatalogNode(id, name, "table", 1, true, authorized, confirmed, 0, 0);
    }

    /** 系统节点:带确权进度(已确权 confirmedCount / 总 totalCount)。 */
    public static CatalogNode system(String name, int visibleCount, int confirmedCount, int totalCount) {
        return new CatalogNode(name, name, "system", visibleCount, false, false, false, confirmedCount, totalCount);
    }
}
