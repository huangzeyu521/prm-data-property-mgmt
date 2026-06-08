package com.csg.prm.authorize.gateway;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 对外开放目录网关本地桩:资产ID 非空且非"NONOPEN"前缀视为在对外开放目录中。
 * 用于本地联调与单测验证"经营权仅限对外开放目录"规则;
 * 生产环境提供基于 Feign 调用数据资产管理平台的实现,并以 @Primary 覆盖此桩。
 */
@Component
public class LocalOpenCatalogGateway implements OpenCatalogGateway {

    @Override
    public boolean isInOpenCatalog(String assetId) {
        return StringUtils.hasText(assetId) && !assetId.startsWith("NONOPEN");
    }
}
