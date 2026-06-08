package com.csg.prm.ledger.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.ledger.monitor.dto.MonitorRuleQuery;
import com.csg.prm.ledger.monitor.entity.MonitorRule;
import com.csg.prm.ledger.monitor.mapper.MonitorRuleMapper;
import com.csg.prm.ledger.monitor.service.MonitorRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MonitorRuleServiceImpl implements MonitorRuleService {

    private final MonitorRuleMapper mapper;

    public MonitorRuleServiceImpl(MonitorRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String create(MonitorRule rule) {
        if (!StringUtils.hasText(rule.getRuleName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "规则名称不能为空");
        }
        rule.setRuleVersion("v1");
        rule.setEffectStatus(MonitorRule.STATUS_DRAFT);
        mapper.insert(rule);
        return rule.getRuleId();
    }

    @Override
    @Transactional
    public void update(MonitorRule rule) {
        MonitorRule exist = require(rule.getRuleId());
        rule.setRuleVersion(nextVersion(exist.getRuleVersion()));
        mapper.updateById(rule);
    }

    @Override
    @Transactional
    public void enable(String ruleId) {
        MonitorRule exist = require(ruleId);
        MonitorRule upd = new MonitorRule();
        upd.setRuleId(ruleId);
        upd.setEffectStatus(MonitorRule.STATUS_ACTIVE);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void disable(String ruleId) {
        require(ruleId);
        MonitorRule upd = new MonitorRule();
        upd.setRuleId(ruleId);
        upd.setEffectStatus(MonitorRule.STATUS_DISABLED);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void delete(String ruleId) {
        MonitorRule exist = require(ruleId);
        if (!MonitorRule.STATUS_DRAFT.equals(exist.getEffectStatus())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(),
                    "仅草稿规则可物理删除;生效中/历史规则请改用停用");
        }
        mapper.deleteById(ruleId);
    }

    @Override
    public MonitorRule getById(String ruleId) {
        return require(ruleId);
    }

    @Override
    public PageResult<MonitorRule> page(MonitorRuleQuery query) {
        LambdaQueryWrapper<MonitorRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getRuleName()), MonitorRule::getRuleName, query.getRuleName())
                .eq(StringUtils.hasText(query.getRuleCategory()), MonitorRule::getRuleCategory, query.getRuleCategory())
                .eq(StringUtils.hasText(query.getEffectStatus()), MonitorRule::getEffectStatus, query.getEffectStatus())
                .orderByDesc(MonitorRule::getCreateTime);
        IPage<MonitorRule> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    private MonitorRule require(String ruleId) {
        if (!StringUtils.hasText(ruleId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "规则ID不能为空");
        }
        MonitorRule r = mapper.selectById(ruleId);
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "监测规则不存在");
        }
        return r;
    }

    private String nextVersion(String current) {
        if (current != null && current.startsWith("v")) {
            try {
                return "v" + (Integer.parseInt(current.substring(1)) + 1);
            } catch (NumberFormatException ignored) {
                // 落到默认
            }
        }
        return "v2";
    }
}
