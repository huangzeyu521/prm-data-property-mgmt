package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 数据授权权益证书(对应界面 IM-DAM-DPR-03-001-004 授权权益管理)。
 * 授权审批通过后自动生成。对应物理表 IM_AUTH_CERT。
 */
@TableName("IM_AUTH_CERT")
public class AuthCert extends BaseEntity {

    public static final String STATUS_EFFECTIVE = "生效";
    public static final String STATUS_REVOKED = "已撤销";
    /** 监测联动熔断:违规/越权暂停,可在整改后续签恢复 */
    public static final String STATUS_SUSPENDED = "已暂停";

    @TableId(value = "CEC_CERT_ID", type = IdType.ASSIGN_UUID)
    private String certId;

    /** 全局唯一证书编号 */
    @TableField("CEC_CERT_NO")
    private String certNo;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_GRANTEE_ORG")
    private String granteeOrg;

    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    @TableField("CEC_SCOPE")
    private String scope;

    @TableField("CEC_VALID_DATE")
    private LocalDateTime validDate;

    @TableField("CEC_CERT_STATUS")
    private String certStatus;

    /** 熔断/暂停原因(监测联动) */
    @TableField("CEC_SUSPEND_REASON")
    private String suspendReason;

    /** 出证所用证书模板ID(按授权类型自选/匹配) */
    @TableField("CEC_TEMPLATE_ID")
    private String templateId;

    @TableField("CEC_TEMPLATE_NAME")
    private String templateName;

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getSuspendReason() {
        return suspendReason;
    }

    public void setSuspendReason(String suspendReason) {
        this.suspendReason = suspendReason;
    }

    public String getCertId() {
        return certId;
    }

    public void setCertId(String certId) {
        this.certId = certId;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
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

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public LocalDateTime getValidDate() {
        return validDate;
    }

    public void setValidDate(LocalDateTime validDate) {
        this.validDate = validDate;
    }

    public String getCertStatus() {
        return certStatus;
    }

    public void setCertStatus(String certStatus) {
        this.certStatus = certStatus;
    }
}
