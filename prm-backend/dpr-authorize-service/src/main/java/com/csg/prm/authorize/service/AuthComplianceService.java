package com.csg.prm.authorize.service;

import com.csg.prm.authorize.dto.AuthComplianceReport;
import com.csg.prm.authorize.entity.AuthCompliance;
import com.csg.prm.common.api.PageResult;

/**
 * 授权申请合规校验服务(F-03-001-001-006)。
 * 依据规则自动校验:材料完整性 / 权限合理性 / 合规性,生成红/黄/绿报告。
 */
public interface AuthComplianceService {

    /** 规则化三维自动校验,生成报告并落库记录。 */
    AuthComplianceReport runCheck(String applyId);

    /** 导出校验记录(CSV,按申请/风险等级过滤)。 */
    byte[] exportRecords(String applyId, String riskLevel);

    PageResult<AuthCompliance> page(long current, long size, String applyId, String riskLevel);
}
