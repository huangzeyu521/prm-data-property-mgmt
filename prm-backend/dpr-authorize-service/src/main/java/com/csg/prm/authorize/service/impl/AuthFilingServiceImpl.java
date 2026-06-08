package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.authorize.mapper.AuthFilingMapper;
import com.csg.prm.authorize.service.AuthFilingService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthFilingServiceImpl implements AuthFilingService {

    private final AuthFilingMapper mapper;

    public AuthFilingServiceImpl(AuthFilingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String create(AuthFiling filing) {
        if (!StringUtils.hasText(filing.getGranteeOrg())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "被授权方不能为空");
        }
        // 附录F §3.4.6:仅数据产品经营权对外授权需备案
        if (!"数据产品经营权".equals(filing.getRightType())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "仅数据产品经营权对外授权需备案(附录G)");
        }
        if (!StringUtils.hasText(filing.getFilingNo())) {
            filing.setFilingNo("BA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        filing.setFilingStatus(AuthFiling.STATUS_PENDING);
        mapper.insert(filing);
        return filing.getFilingId();
    }

    @Override
    @Transactional
    public void file(String filingId) {
        AuthFiling f = require(filingId);
        if (!AuthFiling.STATUS_PENDING.equals(f.getFilingStatus())) {
            throw new BizException("仅待备案记录可完成备案");
        }
        AuthFiling upd = new AuthFiling();
        upd.setFilingId(filingId);
        upd.setFilingStatus(AuthFiling.STATUS_FILED);
        upd.setFilingTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AuthFiling> page(long current, long size, String filingStatus) {
        LambdaQueryWrapper<AuthFiling> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(filingStatus), AuthFiling::getFilingStatus, filingStatus)
                .orderByDesc(AuthFiling::getCreateTime);
        IPage<AuthFiling> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    private AuthFiling require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "备案ID不能为空");
        }
        AuthFiling f = mapper.selectById(id);
        if (f == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "备案记录不存在");
        }
        return f;
    }
}
