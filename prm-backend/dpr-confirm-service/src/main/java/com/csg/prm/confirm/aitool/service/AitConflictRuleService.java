package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitConflictRule;
import com.csg.prm.confirm.aitool.mapper.AitConflictRuleMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 冲突识别规则配置服务(#1):管理员配置规则启停/优先级/阈值;detect 据此选择性识别。
 * 启动幂等种入五类规则(默认全启用,与既有检测行为一致)。
 */
@Service
public class AitConflictRuleService implements ApplicationRunner {

    private final AitConflictRuleMapper mapper;

    public AitConflictRuleService(AitConflictRuleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed(AitConflict.TYPE_SUBJECT, "主体冲突规则", 10, null, "同一客体多主体声明同类权属");
        seed(AitConflict.TYPE_SCOPE, "权限重叠(范围)规则", 20, null, "授权范围与排他授权重叠/覆盖");
        seed(AitConflict.TYPE_VALIDITY, "期限交叉规则", 30, 0d, "授权有效期超出数据生命周期(阈值=允许超期天数)");
        seed(AitConflict.TYPE_HISTORY, "历史记录冲突规则", 15, null, "与历史确权记录矛盾/变更/重复");
        seed(AitConflict.TYPE_RIGHTTYPE, "类型冲突规则", 25, null, "同一数据集多种权利主张并存");
    }

    private void seed(String type, String name, int priority, Double threshold, String remark) {
        Long n = mapper.selectCount(new LambdaQueryWrapper<AitConflictRule>()
                .eq(AitConflictRule::getRuleType, type));
        if (n != null && n > 0) {
            return;
        }
        AitConflictRule r = new AitConflictRule();
        r.setRuleType(type);
        r.setRuleName(name);
        r.setEnabled(1);
        r.setPriority(priority);
        r.setThreshold(threshold);
        r.setRemark(remark);
        mapper.insert(r);
    }

    /** 规则是否启用(无配置则 fail-open=启用,保证默认全识别)。 */
    public boolean enabled(String ruleType) {
        AitConflictRule r = byType(ruleType);
        return r == null || r.getEnabled() == null || r.getEnabled() == 1;
    }

    /** 期限交叉阈值(允许超期天数),无配置默认 0。 */
    public double threshold(String ruleType) {
        AitConflictRule r = byType(ruleType);
        return (r == null || r.getThreshold() == null) ? 0d : r.getThreshold();
    }

    private AitConflictRule byType(String ruleType) {
        return mapper.selectOne(new LambdaQueryWrapper<AitConflictRule>()
                .eq(AitConflictRule::getRuleType, ruleType)
                .orderByAsc(AitConflictRule::getPriority).last("LIMIT 1"));
    }

    /** 列表(按优先级)。 */
    public List<AitConflictRule> list() {
        return mapper.selectList(new LambdaQueryWrapper<AitConflictRule>()
                .orderByAsc(AitConflictRule::getPriority));
    }

    public String save(AitConflictRule rule) {
        if (!StringUtils.hasText(rule.getRuleType())) {
            throw new BizException("规则类型不能为空");
        }
        if (StringUtils.hasText(rule.getRuleId())) {
            mapper.updateById(rule);
            return rule.getRuleId();
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(100);
        }
        mapper.insert(rule);
        return rule.getRuleId();
    }

    public void toggle(String ruleId, boolean on) {
        AitConflictRule r = mapper.selectById(ruleId);
        if (r == null) {
            throw new BizException("规则不存在");
        }
        r.setEnabled(on ? 1 : 0);
        mapper.updateById(r);
    }
}
