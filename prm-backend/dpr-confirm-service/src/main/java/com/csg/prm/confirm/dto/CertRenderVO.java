package com.csg.prm.confirm.dto;

import java.time.LocalDateTime;

/** 证书渲染视图:证书 + 一一对应卡片 + 模板内容,供在线预览。 */
public class CertRenderVO {

    private String certNo;
    private String issueUnit;
    private LocalDateTime issueTime;
    private String certStatus;
    private String templateName;
    private String templateContent;
    // 一一对应的权益卡片要素
    private String cardNo;
    private String assetId;
    private String assetName;
    private String rightType;
    private String rightOwner;
    private LocalDateTime validDate;

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public String getIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(String issueUnit) {
        this.issueUnit = issueUnit;
    }

    public LocalDateTime getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(LocalDateTime issueTime) {
        this.issueTime = issueTime;
    }

    public String getCertStatus() {
        return certStatus;
    }

    public void setCertStatus(String certStatus) {
        this.certStatus = certStatus;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public void setTemplateContent(String templateContent) {
        this.templateContent = templateContent;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getRightOwner() {
        return rightOwner;
    }

    public void setRightOwner(String rightOwner) {
        this.rightOwner = rightOwner;
    }

    public LocalDateTime getValidDate() {
        return validDate;
    }

    public void setValidDate(LocalDateTime validDate) {
        this.validDate = validDate;
    }
}
