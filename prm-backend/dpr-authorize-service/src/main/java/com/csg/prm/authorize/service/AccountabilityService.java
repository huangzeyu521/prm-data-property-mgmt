package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.Accountability;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;

/**
 * 违规追责服务。监测联动熔断暂停授权时自动建账,经责任单位追责闭环。
 */
public interface AccountabilityService {

    /** 暂停授权时自动建追责记录(待追责) */
    String openForSuspension(Accountability record);

    /** 推进追责:待追责->追责中 */
    void handle(String accountId, String responsibleParty, String feedback);

    /** 闭环追责:->已追责 */
    void close(String accountId, String feedback);

    PageResult<Accountability> page(PageQuery query, String handleStatus, String assetId);
}
