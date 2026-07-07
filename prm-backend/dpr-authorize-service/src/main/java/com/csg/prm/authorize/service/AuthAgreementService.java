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

    /**
     * 一事一议授权终审「批准」后,系统自动「形成」《运营授权协议》(对齐 35号文 表2 110「形成数据运营授权协议」)。
     * best-effort:仅当 applyId 为「批准」(兼容存量「已生效」)且为「一事一议」(批量走清单级协议)且尚无协议时生成;幂等;失败只记日志、不抛出。
     * 须在 apply.approve 事务提交「之后」调用(generate 自带事务,在自己的新事务里跑,避免 rollback-only 污染)。
     */
    String autoGenerateForApprovedApply(String applyId);
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

    /** 按附录D《南方电网数据授权运营协议》生成协议文档(HTML 字节,作 .doc 下载);正式稿返回锁定快照。 */
    byte[] appendixDDoc(String agreementId);

    // ===== 协议要素落定(附录D 协商项:草案填空→正式稿锁定→才可签章) =====

    /** 取协议协商要素(含承诺函收口状态),供要素落定表单回显。 */
    com.csg.prm.authorize.dto.AgreementNegotiationVO negotiation(String agreementId);

    /** 保存协商要素(仅草案态;只填空不改条款)。 */
    void saveNegotiation(String agreementId, com.csg.prm.authorize.dto.AgreementNegotiationVO dto);

    /**
     * 生成正式稿:校验要素完备(止日≤5年且≥各明细时效/地理范围/安全三行/违约金/争议方式/送达信息/份数)
     * → 置正式稿 + 渲染正文快照落库(签署与存证以快照为准,防签后改稿)。
     */
    void finalizeDoc(String agreementId);

    /** 退回草案(仅任一方未签章前),要素可改,原快照作废。 */
    void revertToDraft(String agreementId);

    /**
     * 上传《保密承诺函》(附录E,乙方必签——附录D 第八章)。
     * 归档开权限的第三前置条件:双签 ✚ 承诺函;上传后若已双签则自动收尾。
     */
    com.csg.prm.authorize.entity.AuthSealUploadLog uploadConfidentiality(String agreementId, String fileName, byte[] data);

    // ===== 期限管理(动态跟踪:附录D 表1 续期·第七章 变更解除) =====

    /** 续期(经甲方书面同意):仅已归档协议,新止日>原止日且≤今日+5年;留痕上链。 */
    void renew(String agreementId, String newValidUntil);

    /** 终止(第七章情形):记录原因 + 回收底层数据权限 + 留痕上链。 */
    void terminate(String agreementId, String reason);

    /** 存档多维检索:审核/归档状态 + 类型/部门 + 归档时间范围。 */
    PageResult<AuthAgreement> page(long current, long size, String reviewStatus, String archiveStatus,
                                   String agreementType, String deptName, String archiveStart, String archiveEnd);
}
