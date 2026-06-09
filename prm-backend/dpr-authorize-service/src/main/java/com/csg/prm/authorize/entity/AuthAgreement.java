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

    @TableId(value = "CEC_AGREEMENT_ID", type = IdType.ASSIGN_UUID)
    private String agreementId;

    @TableField("CEC_AGREEMENT_NO")
    private String agreementNo;

    @TableField("CEC_APPLY_ID")
    private String applyId;

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
}
