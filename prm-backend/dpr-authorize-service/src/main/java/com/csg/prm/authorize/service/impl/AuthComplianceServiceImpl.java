package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthCompliance;
import com.csg.prm.authorize.mapper.AuthComplianceMapper;
import com.csg.prm.authorize.service.AuthComplianceService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuthComplianceServiceImpl implements AuthComplianceService {

    private final AuthComplianceMapper mapper;

    public AuthComplianceServiceImpl(AuthComplianceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String runCheck(String applyId, String riskLevel, String problemDesc) {
        if (!StringUtils.hasText(applyId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "申请ID不能为空");
        }
        String level = StringUtils.hasText(riskLevel) ? riskLevel : AuthCompliance.LEVEL_GREEN;
        AuthCompliance c = new AuthCompliance();
        c.setApplyId(applyId);
        c.setRiskLevel(level);
        c.setCheckResult(AuthCompliance.LEVEL_GREEN.equals(level) ? "通过"
                : AuthCompliance.LEVEL_YELLOW.equals(level) ? "警告" : "不通过");
        c.setProblemDesc(problemDesc);
        c.setCheckTime(LocalDateTime.now());
        mapper.insert(c);
        return c.getCheckId();
    }

    @Override
    public PageResult<AuthCompliance> page(long current, long size, String applyId, String riskLevel) {
        LambdaQueryWrapper<AuthCompliance> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(applyId), AuthCompliance::getApplyId, applyId)
                .eq(StringUtils.hasText(riskLevel), AuthCompliance::getRiskLevel, riskLevel)
                .orderByDesc(AuthCompliance::getCheckTime);
        IPage<AuthCompliance> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }
}
