package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthScenario;
import com.csg.prm.common.api.PageResult;

/**
 * 授权应用场景配置服务(可研 3.2.2.1.1.3.1.3)。
 */
public interface AuthScenarioService {

    String create(AuthScenario s);

    void update(AuthScenario s);

    void delete(String scenarioId);

    void enable(String scenarioId);

    void disable(String scenarioId);

    AuthScenario getById(String scenarioId);

    /** 分页/搜索:keyword 模糊匹配名称或描述;category/status/rightType(适用权益类型) 精确过滤。 */
    PageResult<AuthScenario> page(long current, long size, String keyword, String category, String status, String rightType);
}
