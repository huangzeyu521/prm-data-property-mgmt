package com.csg.prm.confirm.integration;

import com.csg.prm.confirm.integration.dto.PlatformTableMeta;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 库表元数据服务:确权"选卡片→自动带库表清单"的数据来源。
 *
 * <p>对齐附录F《数据确权信息汇总表》表2/表3——确权粒度到库表(一个系统多张表,每表一行)。
 * 平台已接入则取平台元数据(TW_DATA_CARD + AU_TABLE_META_DATA);未接入则按 assetId/assetName
 * 合成可演示的库表清单(桩),保证"选卡片即带表"的体验,接入后只换 PlatformCardClient 实现,本服务不变。
 */
@Service
public class AssetTableMetaService {

    private final PlatformCardClient platform;

    public AssetTableMetaService(PlatformCardClient platform) {
        this.platform = platform;
    }

    /**
     * 按资产卡片取库表清单。
     *
     * @param assetId   平台卡片ID(关联键)
     * @param assetName 卡片名称(平台未接入时用于合成可读表名)
     */
    public List<PlatformTableMeta> listTableMeta(String assetId, String assetName) {
        if (platform.platformAvailable()) {
            return platform.listTableMeta(assetId);
        }
        return stubTables(assetId, assetName);
    }

    /**
     * 取平台已上传材料原件字节(供"平台同步材料"在线预览);平台未接入时由桩用随包样例兜底。
     * 返回 null=无可取原件。
     */
    public byte[] fetchAttachment(String assetId, String fileName) {
        return platform.fetchAttachment(assetId, fileName);
    }

    /** 平台未接入:合成库表清单(模拟 TW_DATA_CARD + AU_TABLE_META_DATA)。 */
    private List<PlatformTableMeta> stubTables(String assetId, String assetName) {
        String id = StringUtils.hasText(assetId) ? assetId.trim() : "";
        if (!StringUtils.hasText(id)) {
            return List.of();
        }
        // 已知演示资产:回放联调样例(对齐 test/确权申请 手册;来源判定已采集,涉行政监管G+个人隐私H)
        // 平台元数据已上传材料附件:来源说明/行政监管/个人隐私已在平台上传(供"先同步、后补全");I/J 未涉及为 null。
        if ("AST-001".equalsIgnoreCase(id)
                || (assetName != null && assetName.contains("客户用电"))) {
            return List.of(new PlatformTableMeta(
                    "MKT_DB01", "MKT", "C_CONS_ELEC_INFO", "客户用电信息表", "客户用电信息(营销域)",
                    "敏感信息", "A 自行生产数据", "广东电网有限责任公司",
                    true, true, false, false, true,
                    "客户用电信息_数据来源与系统建设投入说明.docx", "营销数据行政监管要求补充说明.docx",
                    "用户入网协议(个人信息授权).docx", null, null));
        }
        // 通用合成:一个系统下多张库表(体现"确权粒度到库表"),来源判定留空待用户/AI补全;平台附件均未采集(null)
        String base = id.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (base.isEmpty()) {
            base = "SYS";
        }
        String inst = base + "_DB01";
        String schema = base.length() > 6 ? base.substring(0, 6) : base;
        String name = StringUtils.hasText(assetName) ? assetName : id;
        return List.of(
                new PlatformTableMeta(inst, schema, base + "_MAIN", name + "主表", name + "主数据",
                        "普通商密", null, null, false, false, false, false, true,
                        null, null, null, null, null),
                new PlatformTableMeta(inst, schema, base + "_DIM", name + "维表", name + "维度数据",
                        "不涉密", null, null, false, false, false, false, true,
                        null, null, null, null, null));
    }
}
