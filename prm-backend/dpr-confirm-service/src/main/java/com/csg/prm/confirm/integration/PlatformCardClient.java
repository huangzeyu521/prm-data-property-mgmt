package com.csg.prm.confirm.integration;

import com.csg.prm.confirm.integration.dto.PlatformCardRef;
import com.csg.prm.confirm.integration.dto.PlatformTableMeta;

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

    /** 入站:按关键词(卡片名称/编码/系统·表)在平台卡片中搜索,供"选卡片而非填ID"。空=未接入。 */
    List<PlatformCardRef> searchCards(String keyword, int limit);

    /** 入站:校验某 assetId 是否对应平台真实卡片(引用完整性,杜绝幽灵资产)。 */
    boolean cardExists(String assetId);

    /** 入站:按 assetId 取该卡片/系统下库表清单(实例/schema/表/密级 + 已采集的来源判定)。空=未接入(服务层合成桩)。 */
    List<PlatformTableMeta> listTableMeta(String assetId);

    /**
     * 入站:按 assetId + 平台附件名,从平台元数据(AU_TABLE_META_DATA)取回该已上传材料原件字节,
     * 供确权"平台同步材料"在线预览。返回 null=平台未接入/附件不存在(同步材料则无本地可预览原件)。
     */
    byte[] fetchAttachment(String assetId, String fileName);

    /** 出站:把某资产的产权/权益结论单向写回平台卡片。返回 true=平台已接收。 */
    boolean pushPropertyAndEquity(String assetId, Map<String, Object> property, List<Map<String, Object>> equity);
}
