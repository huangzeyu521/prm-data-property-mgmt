package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.common.api.PageResult;

/**
 * 对外数据经营权授权备案服务(附录G)。仅经营权对外授权需备案;待备案 -> 已备案。
 */
public interface AuthFilingService {

    String create(AuthFiling filing);

    /** 完成备案:待备案 -> 已备案,记录备案时间 */
    void file(String filingId);

    PageResult<AuthFiling> page(long current, long size, String filingStatus);
}
