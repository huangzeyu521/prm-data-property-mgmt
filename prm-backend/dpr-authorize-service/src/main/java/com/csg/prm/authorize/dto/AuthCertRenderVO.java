package com.csg.prm.authorize.dto;

import java.time.LocalDateTime;

/** 授权证书渲染视图:证书 + 模板内容 + 合规校验结果,供在线预览。 */
public class AuthCertRenderVO {

    private String certNo;
    private String granteeOrg;
    private String assetId;
    // 库表级 + 表5/表6:由来源申请单 join 带出,凭证自包含、不暴露 raw assetId
    private String sysName;     // 所属系统(assetId 去 SYS: 前缀)
    private String assetName;   // 数据表(库表名)
    private String schemaName;  // 模式名称
    private String scenario;    // 使用场景及目的(§3.4.4)
    private String rightType;
    private String scope;
    private LocalDateTime validDate;
    private String certStatus;
    private String templateName;
    private String certType;
    private String templateContent;
    // 合规校验
    private boolean complianceOk;
    private String complianceResult;

    public String getCertNo() { return certNo; }
    public void setCertNo(String certNo) { this.certNo = certNo; }
    public String getGranteeOrg() { return granteeOrg; }
    public void setGranteeOrg(String granteeOrg) { this.granteeOrg = granteeOrg; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getSysName() { return sysName; }
    public void setSysName(String sysName) { this.sysName = sysName; }
    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getScenario() { return scenario; }
    public void setScenario(String scenario) { this.scenario = scenario; }
    public String getRightType() { return rightType; }
    public void setRightType(String rightType) { this.rightType = rightType; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public LocalDateTime getValidDate() { return validDate; }
    public void setValidDate(LocalDateTime validDate) { this.validDate = validDate; }
    public String getCertStatus() { return certStatus; }
    public void setCertStatus(String certStatus) { this.certStatus = certStatus; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getCertType() { return certType; }
    public void setCertType(String certType) { this.certType = certType; }
    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }
    public boolean isComplianceOk() { return complianceOk; }
    public void setComplianceOk(boolean complianceOk) { this.complianceOk = complianceOk; }
    public String getComplianceResult() { return complianceResult; }
    public void setComplianceResult(String complianceResult) { this.complianceResult = complianceResult; }
}
