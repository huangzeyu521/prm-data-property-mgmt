package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthCatalogItem;
import com.csg.prm.authorize.mapper.AuthCatalogItemMapper;
import com.csg.prm.authorize.service.AuthCatalogService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthCatalogServiceImpl implements AuthCatalogService {

    private final AuthCatalogItemMapper mapper;

    public AuthCatalogServiceImpl(AuthCatalogItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String save(AuthCatalogItem item) {
        if (!StringUtils.hasText(item.getCategory())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "目录类别不能为空");
        }
        if (!StringUtils.hasText(item.getName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "名称不能为空");
        }
        if (StringUtils.hasText(item.getItemId())) {
            mapper.updateById(item);
            return item.getItemId();
        }
        if (!StringUtils.hasText(item.getVersion())) {
            item.setVersion("v1");
        }
        item.setStatus(AuthCatalogItem.STATUS_ACTIVE);
        mapper.insert(item);
        return item.getItemId();
    }

    @Override
    @Transactional
    public void enable(String itemId) {
        update(itemId, AuthCatalogItem.STATUS_ACTIVE);
    }

    @Override
    @Transactional
    public void disable(String itemId) {
        update(itemId, AuthCatalogItem.STATUS_DISABLED);
    }

    private void update(String itemId, String status) {
        if (mapper.selectById(itemId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "目录项不存在");
        }
        AuthCatalogItem upd = new AuthCatalogItem();
        upd.setItemId(itemId);
        upd.setStatus(status);
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AuthCatalogItem> page(long current, long size, String category, String name, String status) {
        LambdaQueryWrapper<AuthCatalogItem> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(category), AuthCatalogItem::getCategory, category)
                .like(StringUtils.hasText(name), AuthCatalogItem::getName, name)
                .eq(StringUtils.hasText(status), AuthCatalogItem::getStatus, status)
                .orderByDesc(AuthCatalogItem::getCreateTime);
        IPage<AuthCatalogItem> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }
}
