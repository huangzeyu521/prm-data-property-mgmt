package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.common.api.PageResult;

/**
 * 备案中心两类:① 对外数据经营权授权备案(附录G);② 数据产品备案(附录D 附件2,协议第四章(三))。
 * 仅经营权相关需备案;待备案 -> 已备案。
 */
public interface AuthFilingService {

    String create(AuthFiling filing);

    /** 完成备案:待备案 -> 已备案,记录备案时间。产品备案须先上传《安全合规评审意见》附件。 */
    void file(String filingId);

    PageResult<AuthFiling> page(long current, long size, String filingStatus, String filingType);
}
