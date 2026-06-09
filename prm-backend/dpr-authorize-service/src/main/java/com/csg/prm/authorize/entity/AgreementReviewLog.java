package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 授权协议审核记录(可研 3.2.2.1.1.3.3.3):
 * 记录每次协议审核处理——审核人、结论(通过/驳回重签)、审核意见、时间。
 * 对应物理表 IM_AUTH_AGR_REVIEW_LOG。
 */
@TableName("IM_AUTH_AGR_REVIEW_LOG")
public class AgreementReviewLog extends BaseEntity {

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_AGREEMENT_ID")
    private String agreementId;

    @TableField("CEC_AGREEMENT_NO")
    private String agreementNo;

    /** 审核人 */
    @TableField("CEC_REVIEWER")
    private String reviewer;

    /** 审核结论:审核通过/驳回重签 */
    @TableField("CEC_RESULT")
    private String result;

    /** 审核意见/反馈 */
    @TableField("CEC_OPINION")
    private String opinion;

    @TableField("CEC_REVIEW_TIME")
    private LocalDateTime reviewTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getAgreementNo() { return agreementNo; }
    public void setAgreementNo(String agreementNo) { this.agreementNo = agreementNo; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getOpinion() { return opinion; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
}
