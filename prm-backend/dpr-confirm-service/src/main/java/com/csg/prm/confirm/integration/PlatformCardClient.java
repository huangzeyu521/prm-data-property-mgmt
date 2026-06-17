package com.csg.prm.confirm.integration;

import java.util.List;
import java.util.Map;

/**
 * 数据资产管理平台对接端口(集成边界)。当前平台接口/数据字典未提供 → 用 Stub 实现;
 * 接入后只替换实现 + 在 AssetCardFieldMapper 登记字段/枚举映射,业务代码不变。
 *
 * 方向钉死:① 卡片清单 + 数据权限以平台为准(入站 listVisibleAssetIds);
 *          ② 确权/授权结论 PRM 为真源,单向写回平台(出站 pushPropertyAndEquity)。
 */
public interface PlatformCardClient {

    /** 平台数据源是否已接入(决定档案列表来源:平台清单 / PRM 兜底)。 */
    boolean platformAvailable();

    /** 入站:平台按当前账户数据权限返回可见卡片资产ID集;空=未接入(回退 PRM 本地)。 */
    List<String> listVisibleAssetIds();

    /** 出站:把某资产的产权/权益结论单向写回平台卡片。返回 true=平台已接收。 */
    boolean pushPropertyAndEquity(String assetId, Map<String, Object> property, List<Map<String, Object>> equity);
}
