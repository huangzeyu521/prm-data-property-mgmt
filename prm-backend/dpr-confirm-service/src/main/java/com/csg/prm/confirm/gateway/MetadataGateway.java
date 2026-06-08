package com.csg.prm.confirm.gateway;

/**
 * 元数据管理网关(数据确权 F-02 -> 数据资产管理平台·元数据)。
 * 用于确权表单自动填充与"元数据质量门禁"(质量评分<80 自动驳回确权,需求§5.5 接口③)。
 * 生产环境由 Feign 调用元数据管理模块 + @Primary 覆盖;本地/测试用 {@link LocalMetadataGateway} 桩。
 */
public interface MetadataGateway {

    /** 元数据自动填充信息(用于确权表单预填) */
    record MetadataInfo(String assetName, String rightHolder, String respDept,
                        String rightType, int qualityScore) {
    }

    /** 元数据质量评分(0-100);<80 视为不达标应驳回确权 */
    int qualityScore(String assetId);

    /** 拉取元数据自动填充确权表单要素 */
    MetadataInfo autofill(String assetId);
}
