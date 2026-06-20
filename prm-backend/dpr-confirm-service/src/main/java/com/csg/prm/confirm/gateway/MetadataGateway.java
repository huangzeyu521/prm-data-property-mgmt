package com.csg.prm.confirm.gateway;

/**
 * 元数据管理网关(数据确权 F-02 -> 数据资产管理平台·元数据)。
 * 用于确权表单自动填充与"元数据质量门禁"(质量评分<80 自动驳回确权,需求§5.5 接口③)。
 * 生产环境由 Feign 调用元数据管理模块 + @Primary 覆盖;本地/测试用 {@link LocalMetadataGateway} 桩。
 */
public interface MetadataGateway {

    /**
     * 元数据自动填充信息(用于确权表单预填)。字段对应平台数据资产卡片 TW_DATA_CARD:
     * assetName=BSC_NAME、systemName=MGT_SYS_NAME、rightHolder=MGT_UNIT(管理单位)、
     * respDept=MGT_MNG_DEPT、systemOwner=MGT_USER(责任人)、contactInfo=MGT_USER_PHONE(责任人电话)、
     * secretLevel=SAFE_LEVEL(密级)、region=DD_WSDS/省别。rightType 为确权"建议值"(非卡片事实)。
     */
    record MetadataInfo(String assetName, String systemName, String rightHolder, String respDept,
                        String systemOwner, String contactInfo, String secretLevel, String region,
                        String rightType, int qualityScore) {
    }

    /** 元数据质量评分(0-100);<80 视为不达标应驳回确权 */
    int qualityScore(String assetId);

    /** 拉取元数据自动填充确权表单要素 */
    MetadataInfo autofill(String assetId);
}
