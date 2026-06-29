package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.authorize.entity.Accountability;
import com.csg.prm.authorize.mapper.AccountabilityMapper;
import com.csg.prm.authorize.service.AccountabilityService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.query.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AccountabilityServiceImpl implements AccountabilityService {

    private final AccountabilityMapper mapper;

    public AccountabilityServiceImpl(AccountabilityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String openForSuspension(Accountability record) {
        if (record == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR);
        }
        record.setHandleStatus(Accountability.STATUS_PENDING);
        mapper.insert(record);
        return record.getAccountId();
    }

    @Override
    @Transactional
    public void handle(String accountId, String responsibleParty, String feedback) {
        Accountability r = require(accountId);
        if (Accountability.STATUS_DONE.equals(r.getHandleStatus())) {
            throw new BusinessException("该追责已闭环");
        }
        Accountability upd = new Accountability();
        upd.setAccountId(accountId);
        upd.setHandleStatus(Accountability.STATUS_HANDLING);
        upd.setResponsibleParty(responsibleParty);
        upd.setHandleFeedback(feedback);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void close(String accountId, String feedback) {
        Accountability r = require(accountId);
        Accountability upd = new Accountability();
        upd.setAccountId(accountId);
        upd.setHandleStatus(Accountability.STATUS_DONE);
        upd.setHandleFeedback(StringUtils.hasText(feedback) ? feedback : r.getHandleFeedback());
        upd.setHandleTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    @Override
    public PageResult<Accountability> page(PageRequest query, String handleStatus, String assetId) {
        LambdaQueryWrapper<Accountability> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(handleStatus), Accountability::getHandleStatus, handleStatus)
                .eq(StringUtils.hasText(assetId), Accountability::getAssetId, assetId)
                .orderByDesc(Accountability::getCreateTime);
        IPage<Accountability> p = mapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    private Accountability require(String accountId) {
        if (!StringUtils.hasText(accountId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "追责ID不能为空");
        }
        Accountability r = mapper.selectById(accountId);
        if (r == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "追责记录不存在");
        }
        return r;
    }
}
