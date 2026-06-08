package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.common.api.PageResult;

/**
 * 数据运营授权协议服务:生成、签章上传、审核、存档、下载(F-03-001-003)。
 */
public interface AuthAgreementService {
    /** 基于审批通过的授权申请生成协议(待双方签章) */
    String generate(String applyId, String templateId, String granteeOrg);
    /** 授权方(甲方)签署 */
    void signByGrantor(String agreementId, String fileUrl);
    /** 被授权方(乙方)签署 */
    void signByGrantee(String agreementId, String fileUrl);
    /** 审核:通过/驳回重签(须双方签署完成) */
    void review(String agreementId, boolean pass);
    /** 存档(仅审核通过可归档) */
    void archive(String agreementId);
    AuthAgreement getById(String agreementId);
    PageResult<AuthAgreement> page(long current, long size, String reviewStatus, String archiveStatus);
}
