package com.csg.prm.authorize.gateway;

/**
 * 对外开放目录网关(附录F 3.4.3:数据经营权授权范围仅限对外开放目录中的数据资源)。
 * 生产环境由 Feign 调用数据资产管理平台-数据对外开放管理校验资产是否在对外开放目录;
 * 本地/测试用 {@link LocalOpenCatalogGateway} 桩实现。
 */
public interface OpenCatalogGateway {

    /** 资产是否在"对外开放目录"中(经营权授权前置校验) */
    boolean isInOpenCatalog(String assetId);
}
