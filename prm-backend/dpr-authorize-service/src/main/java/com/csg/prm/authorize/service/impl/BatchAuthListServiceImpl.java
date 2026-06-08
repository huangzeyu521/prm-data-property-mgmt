package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.mapper.BatchAuthListMapper;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class BatchAuthListServiceImpl implements BatchAuthListService {

    private final BatchAuthListMapper mapper;

    public BatchAuthListServiceImpl(BatchAuthListMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String create(BatchAuthList list) {
        if (!StringUtils.hasText(list.getListYear())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "授权年度不能为空");
        }
        if (!StringUtils.hasText(list.getListNo())) {
            list.setListNo("PLQD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        }
        list.setListStatus(BatchAuthList.STATUS_DRAFT);
        if (list.getItemCount() == null) {
            list.setItemCount(0);
        }
        mapper.insert(list);
        return list.getBatchListId();
    }

    @Override
    @Transactional
    public void submit(String batchListId) {
        BatchAuthList l = require(batchListId);
        if (!BatchAuthList.STATUS_DRAFT.equals(l.getListStatus())) {
            throw new BizException("仅草案可提交为申报稿");
        }
        update(batchListId, BatchAuthList.STATUS_SUBMITTED);
    }

    @Override
    @Transactional
    public void approve(String batchListId) {
        BatchAuthList l = require(batchListId);
        if (!BatchAuthList.STATUS_SUBMITTED.equals(l.getListStatus())) {
            throw new BizException("仅申报稿可由领导小组办公室批准");
        }
        update(batchListId, BatchAuthList.STATUS_APPROVED);
    }

    @Override
    public BatchAuthList getById(String batchListId) {
        return require(batchListId);
    }

    @Override
    public PageResult<BatchAuthList> page(long current, long size, String listYear, String listStatus) {
        LambdaQueryWrapper<BatchAuthList> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(listYear), BatchAuthList::getListYear, listYear)
                .eq(StringUtils.hasText(listStatus), BatchAuthList::getListStatus, listStatus)
                .orderByDesc(BatchAuthList::getCreateTime);
        IPage<BatchAuthList> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    private void update(String id, String status) {
        BatchAuthList upd = new BatchAuthList();
        upd.setBatchListId(id);
        upd.setListStatus(status);
        mapper.updateById(upd);
    }

    private BatchAuthList require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "清单ID不能为空");
        }
        BatchAuthList l = mapper.selectById(id);
        if (l == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "批量授权清单不存在");
        }
        return l;
    }
}
