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
        String name = StringUtils.hasText(assetId) ? assetId.replaceAll(".*[:/]", "") + "数据集" : "未知数据集";
        return new MetadataInfo(name, "中国南方电网有限责任公司", "数字化部", "数据持有权", qualityScore(assetId));
    }
}
