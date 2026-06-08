package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 确权权益证书(对应界面 IM-DAM-DPR-02-001-003-003)。对应物理表 IM_EQUITY_CERT。
 */
@TableName("IM_EQUITY_CERT")
public class EquityCert extends BaseEntity {

    public static final String STATUS_EFFECTIVE = "生效";
    public static final String STATUS_REVOKED = "已注销";

    @TableId(value = "CEC_CERT_ID", type = IdType.ASSIGN_UUID)
    private String certId;

    /** 全局唯一防伪编号 */
    @TableField("CEC_CERT_NO")
    private String certNo;

    @TableField("CEC_CARD_ID")
    private String cardId;

    @TableField("CEC_ISSUE_UNIT")
    private String issueUnit;

    @TableField("CEC_ISSUE_TIME")
    private LocalDateTime issueTime;

    @TableField("CEC_CERT_STATUS")
    private String certStatus;

    @TableField("CEC_TEMPLATE_ID")
    private String templateId;

    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    public String getCertId() { return certId; }
    public void setCertId(String certId) { this.certId = certId; }
    public String getCertNo() { return certNo; }
    public void setCertNo(String certNo) { this.certNo = certNo; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getIssueUnit() { return issueUnit; }
    public void setIssueUnit(String issueUnit) { this.issueUnit = issueUnit; }
    public LocalDateTime getIssueTime() { return issueTime; }
    public void setIssueTime(LocalDateTime issueTime) { this.issueTime = issueTime; }
    public String getCertStatus() { return certStatus; }
    public void setCertStatus(String certStatus) { this.certStatus = certStatus; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
}
