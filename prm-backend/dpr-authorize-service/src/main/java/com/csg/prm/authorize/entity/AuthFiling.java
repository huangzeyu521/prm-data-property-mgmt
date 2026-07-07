package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 分子公司对外数据经营权授权备案(附录G / 附录F §3.4.6)。
 * 取得经营权的单位对外提供数据产品/服务时,须与被授权单位签协议并在公司数字化部备案。
 * 对应物理表 IM_AUTH_FILING。
 */
@TableName("IM_AUTH_FILING")
public class AuthFiling extends BaseEntity {

    public static final String STATUS_PENDING = "待备案";
    public static final String STATUS_FILED = "已备案";
    /** 备案类型:授权备案(附录G,默认)/产品备案(附录D 附件2 数据产品备案表) */
    public static final String TYPE_AUTH = "授权备案";
    public static final String TYPE_PRODUCT = "产品备案";

    @TableId(value = "CEC_FILING_ID", type = IdType.ASSIGN_UUID)
    private String filingId;

    @TableField("CEC_FILING_NO")
    private String filingNo;

    /** 关联运营授权协议(附录D) */
    @TableField("CEC_AGREEMENT_ID")
    private String agreementId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 备案单位(分子公司) */
    @TableField("CEC_FILING_ORG")
    private String filingOrg;

    /** 被授权方 */
    @TableField("CEC_GRANTEE_ORG")
    private String granteeOrg;

    /** 产权类型(经营权对外授权才需备案) */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    /** 协议编号(附录G 备案表;建档时由关联协议快照) */
    @TableField("CEC_AGREEMENT_NO")
    private String agreementNo;

    /** 授权期限(附录G 备案表;由关联授权申请快照) */
    @TableField("CEC_VALID_DATE")
    private LocalDateTime validDate;

    @TableField("CEC_FILING_STATUS")
    private String filingStatus;

    @TableField("CEC_FILING_TIME")
    private LocalDateTime filingTime;

    @TableField("CEC_REMARK")
    private String remark;

    /** 备案类型:授权备案/产品备案(空按授权备案,兼容存量) */
    @TableField("CEC_FILING_TYPE")
    private String filingType;

    // ===== 数据产品备案(附录D 附件2 表2):协议第四章(三)乙方对外提供数据产品/服务须在甲方处备案 =====

    @TableField("CEC_PRODUCT_NAME")
    private String productName;

    @TableField("CEC_PRODUCT_INTRO")
    private String productIntro;

    @TableField("CEC_APP_SCENARIO")
    private String appScenario;

    @TableField("CEC_SERVICE_TARGET")
    private String serviceTarget;

    /** 涉及授权数据表名称(多选,顿号分隔;须落在协议附件1《数据授权清单》内) */
    @TableField("CEC_INVOLVED_TABLES")
    private String involvedTables;

    public String getFilingType() { return filingType; }
    public void setFilingType(String filingType) { this.filingType = filingType; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductIntro() { return productIntro; }
    public void setProductIntro(String productIntro) { this.productIntro = productIntro; }
    public String getAppScenario() { return appScenario; }
    public void setAppScenario(String appScenario) { this.appScenario = appScenario; }
    public String getServiceTarget() { return serviceTarget; }
    public void setServiceTarget(String serviceTarget) { this.serviceTarget = serviceTarget; }
    public String getInvolvedTables() { return involvedTables; }
    public void setInvolvedTables(String involvedTables) { this.involvedTables = involvedTables; }

    public String getFilingId() { return filingId; }
    public void setFilingId(String filingId) { this.filingId = filingId; }
    public String getFilingNo() { return filingNo; }
    public void setFilingNo(String filingNo) { this.filingNo = filingNo; }
    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getFilingOrg() { return filingOrg; }
    public void setFilingOrg(String filingOrg) { this.filingOrg = filingOrg; }
    public String getGranteeOrg() { return granteeOrg; }
    public void setGranteeOrg(String granteeOrg) { this.granteeOrg = granteeOrg; }
    public String getRightType() { return rightType; }
    public void setRightType(String rightType) { this.rightType = rightType; }
    public String getAgreementNo() { return agreementNo; }
    public void setAgreementNo(String agreementNo) { this.agreementNo = agreementNo; }
    public LocalDateTime getValidDate() { return validDate; }
    public void setValidDate(LocalDateTime validDate) { this.validDate = validDate; }
    public String getFilingStatus() { return filingStatus; }
    public void setFilingStatus(String filingStatus) { this.filingStatus = filingStatus; }
    public LocalDateTime getFilingTime() { return filingTime; }
    public void setFilingTime(LocalDateTime filingTime) { this.filingTime = filingTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
