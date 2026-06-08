package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthCatalogItem;
import com.csg.prm.common.api.PageResult;

/**
 * 授权域目录项服务(指引/场景/申请表单模板/协议模板库 通用)。
 */
public interface AuthCatalogService {
    String save(AuthCatalogItem item);
    void enable(String itemId);
    void disable(String itemId);
    PageResult<AuthCatalogItem> page(long current, long size, String category, String name, String status);
}
