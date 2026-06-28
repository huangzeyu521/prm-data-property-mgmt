package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthScenario;
import com.csg.prm.authorize.mapper.AuthScenarioMapper;
import com.csg.prm.authorize.service.AuthScenarioService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthScenarioServiceImpl implements AuthScenarioService {

    private final AuthScenarioMapper mapper;

    public AuthScenarioServiceImpl(AuthScenarioMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String create(AuthScenario s) {
        if (!StringUtils.hasText(s.getScenarioName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "场景名称不能为空");
        }
        s.setScenarioStatus(AuthScenario.STATUS_ACTIVE);
        mapper.insert(s);
        return s.getScenarioId();
    }

    @Override
    @Transactional
    public void update(AuthScenario s) {
        require(s.getScenarioId());
        mapper.updateById(s);
    }

    @Override
    @Transactional
    public void delete(String scenarioId) {
        require(scenarioId);
        mapper.deleteById(scenarioId);
    }

    @Override
    @Transactional
    public void enable(String scenarioId) {
        require(scenarioId);
        AuthScenario upd = new AuthScenario();
        upd.setScenarioId(scenarioId);
        upd.setScenarioStatus(AuthScenario.STATUS_ACTIVE);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void disable(String scenarioId) {
        require(scenarioId);
        AuthScenario upd = new AuthScenario();
        upd.setScenarioId(scenarioId);
        upd.setScenarioStatus(AuthScenario.STATUS_DISABLED);
        mapper.updateById(upd);
    }

    @Override
    public AuthScenario getById(String scenarioId) {
        return require(scenarioId);
    }

    @Override
    public PageResult<AuthScenario> page(long current, long size, String keyword, String category, String status, String rightType) {
        LambdaQueryWrapper<AuthScenario> w = new LambdaQueryWrapper<>();
        w.and(StringUtils.hasText(keyword), q -> q
                        .like(AuthScenario::getScenarioName, keyword)
                        .or().like(AuthScenario::getDescription, keyword))
                .eq(StringUtils.hasText(category), AuthScenario::getCategory, category)
                .eq(StringUtils.hasText(status), AuthScenario::getScenarioStatus, status)
                .eq(StringUtils.hasText(rightType), AuthScenario::getRightType, rightType)
                .orderByDesc(AuthScenario::getCreateTime);
        IPage<AuthScenario> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    private AuthScenario require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "场景ID不能为空");
        }
        AuthScenario s = mapper.selectById(id);
        if (s == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "应用场景不存在");
        }
        return s;
    }
}
