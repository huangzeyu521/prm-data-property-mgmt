package com.csg.prm.confirm.gateway;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 元数据网关本地桩:确定性实现,便于联调与单测。
 * 约定:资产ID 含 "LOWQ" 视为元数据质量不达标(评分 60),否则 92。
 * 生产环境另以 Feign 实现 + @Primary 覆盖,真正对接元数据管理模块。
 */
@Component
public class LocalMetadataGateway implements MetadataGateway {

    @Override
    public int qualityScore(String assetId) {
        if (!StringUtils.hasText(assetId)) {
            return 0;
        }
        return assetId.toUpperCase().contains("LOWQ") ? 60 : 92;
    }

    @Override
    public MetadataInfo autofill(String assetId) {
        String id = StringUtils.hasText(assetId) ? assetId.trim() : "";
        // 已知演示卡片:回放平台卡片字段(对齐 test/确权申请 手册 AST-001)
        if ("AST-001".equalsIgnoreCase(id)) {
            return new MetadataInfo("AST-001数据集", "营销业务应用系统", "广东电网有限责任公司", "数字化部",
                    "张工", "020-88886666", "敏感信息", "广东", "数据持有权", qualityScore(assetId));
        }
        // 通用:按卡片字段口径合成(平台接入后由 Feign 实现返回真值,DTO/前端不变)
        String name = StringUtils.hasText(id) ? id.replaceAll(".*[:/]", "") + "数据集" : "未知数据集";
        String base = StringUtils.hasText(id) ? id.replaceAll(".*[:/]", "") : "未知";
        return new MetadataInfo(name, base + "业务系统", "中国南方电网有限责任公司", "数字化部",
                "数据认责岗", "020-00000000", "普通商密", "", "数据持有权", qualityScore(assetId));
    }
}
