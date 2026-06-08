package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthCompliance;
import com.csg.prm.common.api.PageResult;

/**
 * 授权申请合规校验服务(F-03-001-001-006)。
 */
public interface AuthComplianceService {
    /** 对授权申请执行合规校验,生成红/黄/绿结果记录 */
    String runCheck(String applyId, String riskLevel, String problemDesc);
    PageResult<AuthCompliance> page(long current, long size, String applyId, String riskLevel);
}
