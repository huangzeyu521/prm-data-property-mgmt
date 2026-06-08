package com.csg.prm.ledger.monitor.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.monitor.dto.MonitorRuleQuery;
import com.csg.prm.ledger.monitor.entity.MonitorRule;

/**
 * 监测规则服务。生效中/历史规则禁物理删除,仅可停用 + 新版本。
 */
public interface MonitorRuleService {

    String create(MonitorRule rule);

    /** 修改生成新版本(保留历史可追溯) */
    void update(MonitorRule rule);

    void enable(String ruleId);

    void disable(String ruleId);

    /** 物理删除:仅允许删除草稿规则;生效中/历史规则请改用停用(守可审计红线) */
    void delete(String ruleId);

    MonitorRule getById(String ruleId);

    PageResult<MonitorRule> page(MonitorRuleQuery query);
}
