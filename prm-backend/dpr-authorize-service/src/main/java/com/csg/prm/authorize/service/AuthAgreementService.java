package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AgreementReviewLog;
import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthSealUploadLog;
import com.csg.prm.common.api.PageResult;

import java.util.List;

/**
 * 数据运营授权协议服务:生成、签章上传、审核、存档、下载(F-03-001-003)。
 */
public interface AuthAgreementService {
    /** 基于审批通过的授权申请生成协议(待双方签章) */
    String generate(String applyId, String templateId, String granteeOrg);

    /** 批量授权:一份批量清单生成一份《运营授权协议》(一清单一协议,幂等防重),清单各项=协议附件。 */
    String generateForBatch(String batchListId);
    /** 授权方(甲方)签署 */
    void signByGrantor(String agreementId, String fileUrl);
    /** 被授权方(乙方)签署 */
    void signByGrantee(String agreementId, String fileUrl);

    /** 上传签章文件:格式校验 + 签章有效性检查 + 记录上传记录,返回记录(含验证结果)。 */
    AuthSealUploadLog uploadSeal(String agreementId, String role, String fileName, byte[] data);

    /** 下载某条上传记录的签章文件。 */
    byte[] downloadSeal(String logId);

    AuthSealUploadLog getUploadLog(String logId);

    /** 某协议的签章上传记录(置空 fileData)。 */
    List<AuthSealUploadLog> listUploadLogs(String agreementId);

    /** 审核:通过/驳回重签(须双方签署完成),带审核意见并记录审核日志 */
    void review(String agreementId, boolean pass, String opinion);

    /** 某协议的审核处理记录。 */
    List<AgreementReviewLog> listReviewLogs(String agreementId);
    /** 存档(仅审核通过可归档),记归档时间 + 审计日志 */
    void archive(String agreementId);

    /** 记录存档访问审计(查看/下载)。 */
    void recordAccess(String agreementId, String action, String operator);

    /** 某协议的存档审计日志(归档/查看/下载)。 */
    List<com.csg.prm.authorize.entity.AgreementArchiveLog> listArchiveLogs(String agreementId);

    AuthAgreement getById(String agreementId);

    /** 协议要素核对视图(附录D §3.4.4):协议 + join 来源申请单的协议要素,供协议审核核对内容一致。 */
    com.csg.prm.authorize.dto.AgreementElementsVO elements(String agreementId);

    /** 存档多维检索:审核/归档状态 + 类型/部门 + 归档时间范围。 */
    PageResult<AuthAgreement> page(long current, long size, String reviewStatus, String archiveStatus,
                                   String agreementType, String deptName, String archiveStart, String archiveEnd);
}
