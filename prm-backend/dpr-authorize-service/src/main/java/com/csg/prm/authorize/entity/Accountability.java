package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 违规追责记录(对应附录F 3.4.5 动态跟踪与违规处置)。
 * 监测联动熔断暂停授权证书时自动生成,经合规小组/责任单位追责闭环。
 * 对应物理表 IM_AUTH_ACCOUNTABILITY。
 */
@TableName("IM_AUTH_ACCOUNTABILITY")
public class Accountability extends BaseEntity {

    public static final String STATUS_PENDING = "待追责";
    public static final String STATUS_HANDLING = "追责中";
    public static final String STATUS_DONE = "已追责";

    @TableId(value = "CEC_ACCOUNT_ID", type = IdType.ASSIGN_UUID)
    private String accountId;

    /** 关联被暂停的授权证书 */
    @TableField("CEC_CERT_ID")
    private String certId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_GRANTEE_ORG")
    private String granteeOrg;

    /** 违规类型:越权调用/违规使用/到期未续/超范围 */
    @TableField("CEC_VIOLATION_TYPE")
    private String violationType;

    /** 来源预警ID(监测侧) */
    @TableField("CEC_SOURCE_ALERT_ID")
    private String sourceAlertId;

    @TableField("CEC_REASON")
    private String reason;

    /** 追责状态:待追责/追责中/已追责 */
    @TableField("CEC_HANDLE_STATUS")
    private String handleStatus;

    /** 责任主体 */
    @TableField("CEC_RESPONSIBLE_PARTY")
    private String responsibleParty;

    @TableField("CEC_HANDLE_FEEDBACK")
    private String handleFeedback;

    @TableField("CEC_HANDLE_TIME")
    private LocalDateTime handleTime;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getGranteeOrg() {
        return granteeOrg;
    }

    public void setGranteeOrg(String granteeOrg) {
        this.granteeOrg = granteeOrg;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getSourceAlertId() {
        return sourceAlertId;
    }

    public void setSourceAlertId(String sourceAlertId) {
        this.sourceAlertId = sourceAlertId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getHandleStatus() {
        return handleStatus;
    }

    public void setHandleStatus(String handleStatus) {
        this.handleStatus = handleStatus;
    }

    public String getResponsibleParty() {
        return responsibleParty;
    }

    public void setResponsibleParty(String responsibleParty) {
        this.responsibleParty = responsibleParty;
    }

    public String getHandleFeedback() {
        return handleFeedback;
    }

    public void setHandleFeedback(String handleFeedback) {
        this.handleFeedback = handleFeedback;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }
}
