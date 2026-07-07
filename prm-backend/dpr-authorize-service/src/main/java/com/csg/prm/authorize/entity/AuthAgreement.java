package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 数据运营授权协议(覆盖 签章上传/审核/存档/下载 全生命周期)。
 * 对应界面 IM-DAM-DPR-03-001-003-*。对应物理表 IM_AUTH_AGREEMENT。
 */
@TableName("IM_AUTH_AGREEMENT")
public class AuthAgreement extends BaseEntity {

    public static final String SEAL_PENDING = "待双方签章";
    public static final String SEAL_PARTIAL = "待对方签章";
    public static final String SEAL_SIGNED = "已双签";
    public static final String REVIEW_PENDING = "待审核";
    public static final String REVIEW_PASS = "审核通过";
    public static final String REVIEW_REJECT = "驳回重签";
    public static final String ARCHIVE_NO = "未归档";
    public static final String ARCHIVE_YES = "已归档";
    /** 协议文本状态:草案(要素未落定,可改)→正式稿(要素完备锁定+正文快照,才可签章) */
    public static final String DOC_DRAFT = "草案";
    public static final String DOC_FINAL = "正式稿";
    public static final String STATUS_TERMINATED = "已终止";

    @TableId(value = "CEC_AGREEMENT_ID", type = IdType.ASSIGN_UUID)
    private String agreementId;

    @TableField("CEC_AGREEMENT_NO")
    private String agreementNo;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 批量授权清单ID(批量授权:一清单一协议,清单各项=协议附件《数据授权清单》;专项为空) */
    @TableField("CEC_BATCH_LIST_ID")
    private String batchListId;

    @TableField("CEC_TEMPLATE_ID")
    private String templateId;

    @TableField("CEC_GRANTEE_ORG")
    private String granteeOrg;

    @TableField("CEC_FILE_URL")
    private String fileUrl;

    /** 签章状态:待签章/已签章 */
    @TableField("CEC_SEAL_STATUS")
    private String sealStatus;

    /** 签章有效性验证结果 */
    @TableField("CEC_SEAL_VERIFY")
    private String sealVerify;

    /** 审核状态:待审核/审核通过/驳回重签 */
    @TableField("CEC_REVIEW_STATUS")
    private String reviewStatus;

    /** 归档状态:未归档/已归档 */
    @TableField("CEC_ARCHIVE_STATUS")
    private String archiveStatus;

    /** 协议类型(授权权益类型,供存档检索) */
    @TableField("CEC_AGREEMENT_TYPE")
    private String agreementType;

    /** 所属部门/业务域(供存档检索) */
    @TableField("CEC_DEPT_NAME")
    private String deptName;

    /** 归档时间 */
    @TableField("CEC_ARCHIVE_TIME")
    private LocalDateTime archiveTime;

    @TableField("CEC_SIGN_TIME")
    private LocalDateTime signTime;

    /** 授权方(甲方)是否已签署 */
    @TableField("CEC_GRANTOR_SIGNED")
    private Boolean grantorSigned;

    @TableField("CEC_GRANTOR_SIGN_TIME")
    private LocalDateTime grantorSignTime;

    /** 被授权方(乙方)是否已签署 */
    @TableField("CEC_GRANTEE_SIGNED")
    private Boolean granteeSigned;

    @TableField("CEC_GRANTEE_SIGN_TIME")
    private LocalDateTime granteeSignTime;

    // ===== 协议要素落定(附录D 协商项,批准后·双签前填空;正式稿锁定) =====

    /** 文本状态:草案/正式稿(空按草案处理,兼容存量) */
    @TableField("CEC_DOC_STATUS")
    private String docStatus;

    /** 授权有效期止日(附录D 表1:自签订日一般3年,最长不超过5年;须≥各明细授权时效) */
    @TableField("CEC_VALID_UNTIL")
    private LocalDateTime validUntil;

    /** 数据使用地理范围(附录D 表1) */
    @TableField("CEC_GEO_SCOPE")
    private String geoScope;

    /** 附录D 表2 数据安全要求:数据加密/访问控制/操作审计 三行 */
    @TableField("CEC_SECURITY_ENCRYPT")
    private String securityEncrypt;

    @TableField("CEC_SECURITY_ACCESS")
    private String securityAccess;

    @TableField("CEC_SECURITY_AUDIT")
    private String securityAudit;

    /** 收益分配补充约定(附录D 第六章;批量清单级统一约定) */
    @TableField("CEC_BENEFIT_ALLOCATION")
    private String benefitAllocation;

    /** 违约金金额(万元,附录D 第九章) */
    @TableField("CEC_PENALTY_AMOUNT")
    private String penaltyAmount;

    /** 争议解决方式(附录D 第十章:甲方所在地法院/仲裁(含仲裁委·地点)) */
    @TableField("CEC_DISPUTE_METHOD")
    private String disputeMethod;

    /** 乙方送达信息(附录D 第十章(二):手机/邮箱/邮寄地址,至少其一) */
    @TableField("CEC_SERVICE_DELIVERY")
    private String serviceDelivery;

    /** 协议正本份数(附录D 第十一章,双方各半) */
    @TableField("CEC_COPIES_COUNT")
    private Integer copiesCount;

    /** 保密承诺函(附录E)文件地址——乙方必签,归档开权限的前置条件(附录D 第八章) */
    @TableField("CEC_CONFIDENTIALITY_FILE")
    private String confidentialityFile;

    /** 正式稿正文快照(生成正式稿时渲染落库,签署与存证以此为准,防签后改稿) */
    @TableField("CEC_DOC_SNAPSHOT")
    private String docSnapshot;

    // ===== 期限管理(动态跟踪:续期/终止,附录D 表1·第七章) =====

    /** 是否已终止 */
    @TableField("CEC_TERMINATED")
    private Boolean terminated;

    /** 终止原因(第七章情形) */
    @TableField("CEC_TERMINATE_REASON")
    private String terminateReason;

    @TableField("CEC_TERMINATE_TIME")
    private LocalDateTime terminateTime;

    public Boolean getGrantorSigned() { return grantorSigned; }
    public void setGrantorSigned(Boolean grantorSigned) { this.grantorSigned = grantorSigned; }
    public LocalDateTime getGrantorSignTime() { return grantorSignTime; }
    public void setGrantorSignTime(LocalDateTime grantorSignTime) { this.grantorSignTime = grantorSignTime; }
    public Boolean getGranteeSigned() { return granteeSigned; }
    public void setGranteeSigned(Boolean granteeSigned) { this.granteeSigned = granteeSigned; }
    public LocalDateTime getGranteeSignTime() { return granteeSignTime; }
    public void setGranteeSignTime(LocalDateTime granteeSignTime) { this.granteeSignTime = granteeSignTime; }

    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getAgreementNo() { return agreementNo; }
    public void setAgreementNo(String agreementNo) { this.agreementNo = agreementNo; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getBatchListId() { return batchListId; }
    public void setBatchListId(String batchListId) { this.batchListId = batchListId; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getGranteeOrg() { return granteeOrg; }
    public void setGranteeOrg(String granteeOrg) { this.granteeOrg = granteeOrg; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getSealStatus() { return sealStatus; }
    public void setSealStatus(String sealStatus) { this.sealStatus = sealStatus; }
    public String getSealVerify() { return sealVerify; }
    public void setSealVerify(String sealVerify) { this.sealVerify = sealVerify; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    public String getArchiveStatus() { return archiveStatus; }
    public void setArchiveStatus(String archiveStatus) { this.archiveStatus = archiveStatus; }
    public String getAgreementType() { return agreementType; }
    public void setAgreementType(String agreementType) { this.agreementType = agreementType; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public java.time.LocalDateTime getArchiveTime() { return archiveTime; }
    public void setArchiveTime(java.time.LocalDateTime archiveTime) { this.archiveTime = archiveTime; }
    public LocalDateTime getSignTime() { return signTime; }
    public void setSignTime(LocalDateTime signTime) { this.signTime = signTime; }
    public String getDocStatus() { return docStatus; }
    public void setDocStatus(String docStatus) { this.docStatus = docStatus; }
    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }
    public String getGeoScope() { return geoScope; }
    public void setGeoScope(String geoScope) { this.geoScope = geoScope; }
    public String getSecurityEncrypt() { return securityEncrypt; }
    public void setSecurityEncrypt(String securityEncrypt) { this.securityEncrypt = securityEncrypt; }
    public String getSecurityAccess() { return securityAccess; }
    public void setSecurityAccess(String securityAccess) { this.securityAccess = securityAccess; }
    public String getSecurityAudit() { return securityAudit; }
    public void setSecurityAudit(String securityAudit) { this.securityAudit = securityAudit; }
    public String getBenefitAllocation() { return benefitAllocation; }
    public void setBenefitAllocation(String benefitAllocation) { this.benefitAllocation = benefitAllocation; }
    public String getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(String penaltyAmount) { this.penaltyAmount = penaltyAmount; }
    public String getDisputeMethod() { return disputeMethod; }
    public void setDisputeMethod(String disputeMethod) { this.disputeMethod = disputeMethod; }
    public String getServiceDelivery() { return serviceDelivery; }
    public void setServiceDelivery(String serviceDelivery) { this.serviceDelivery = serviceDelivery; }
    public Integer getCopiesCount() { return copiesCount; }
    public void setCopiesCount(Integer copiesCount) { this.copiesCount = copiesCount; }
    public String getConfidentialityFile() { return confidentialityFile; }
    public void setConfidentialityFile(String confidentialityFile) { this.confidentialityFile = confidentialityFile; }
    public String getDocSnapshot() { return docSnapshot; }
    public void setDocSnapshot(String docSnapshot) { this.docSnapshot = docSnapshot; }
    public Boolean getTerminated() { return terminated; }
    public void setTerminated(Boolean terminated) { this.terminated = terminated; }
    public String getTerminateReason() { return terminateReason; }
    public void setTerminateReason(String terminateReason) { this.terminateReason = terminateReason; }
    public LocalDateTime getTerminateTime() { return terminateTime; }
    public void setTerminateTime(LocalDateTime terminateTime) { this.terminateTime = terminateTime; }
}
