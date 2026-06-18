package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.dto.BatchComplianceResult;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.mapper.BatchAuthListMapper;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchAuthListServiceImpl implements BatchAuthListService {

    private final BatchAuthListMapper mapper;
    private final AuthApplyService applyService;

    public BatchAuthListServiceImpl(BatchAuthListMapper mapper, AuthApplyService applyService) {
        this.mapper = mapper;
        this.applyService = applyService;
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
        // 逐项合规门禁(与一事一议对齐):先拦第三方凭证红线,再逐条提交进批量审批链
        // ——之前 submit 只改清单状态、明细停在草稿从未进链;此处补齐"提交即逐项校验+入链"。
        List<AuthApply> items = applyService.byBatch(batchListId);
        if (items.isEmpty()) {
            throw new BizException("清单为空,不可提交申报稿(请先添加授权项)");
        }
        List<String> blocked = new ArrayList<>();
        for (AuthApply a : items) {
            if (!AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                continue; // 已入链的明细跳过(幂等)
            }
            if (StringUtils.hasText(a.getThirdPartySource()) && !StringUtils.hasText(a.getThirdPartyLicense())) {
                blocked.add(a.getAssetName() + "(涉第三方未提许可凭证)");
            }
        }
        if (!blocked.isEmpty()) {
            throw new BizException("以下明细未通过合规校验,清单不可提交:" + String.join("、", blocked));
        }
        // 逐条提交:复用一事一议同一套硬校验(先确后授+授权⊆确权边界+经营权仅限开放目录+默认2年)
        for (AuthApply a : items) {
            if (AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                applyService.submit(a.getApplyId());
            }
        }
        update(batchListId, BatchAuthList.STATUS_SUBMITTED);
    }

    /** 只读试跑:逐草稿明细做合规校验(第三方凭证红线 + submitBlockReason),返回被拦清单。不改状态。 */
    @Override
    public BatchComplianceResult complianceCheck(String batchListId) {
        require(batchListId);
        List<AuthApply> items = applyService.byBatch(batchListId);
        List<BatchComplianceResult.BlockedItem> blocked = new ArrayList<>();
        int draftTotal = 0;
        for (AuthApply a : items) {
            if (!AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                continue; // 已入链明细跳过(与 submit 幂等一致)
            }
            draftTotal++;
            String reason;
            if (StringUtils.hasText(a.getThirdPartySource()) && !StringUtils.hasText(a.getThirdPartyLicense())) {
                reason = "涉第三方未提许可凭证";
            } else {
                reason = applyService.submitBlockReason(a.getApplyId());
            }
            if (reason != null) {
                blocked.add(new BatchComplianceResult.BlockedItem(a.getApplyId(), a.getAssetName(), reason));
            }
        }
        return new BatchComplianceResult(blocked.isEmpty(), draftTotal, blocked.size(), blocked);
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
