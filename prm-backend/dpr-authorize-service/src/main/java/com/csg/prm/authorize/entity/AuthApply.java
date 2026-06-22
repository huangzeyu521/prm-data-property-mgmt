package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 数据授权申请单(表5《数据授权申请单》,对应界面 IM-DAM-DPR-03-001-001)。对应物理表 IM_AUTH_APPLY。
 * 强制"先确后授":授权必须引用有效权益卡片(equityCardId);冻结卡片熔断。
 * 多级审批(对齐附录F 4.2/4.3,审批链以 {@link com.csg.prm.common.workflow.FlowDefinitions} 为单一事实来源):
 *   专项一事一议:草稿->合规审核->业务审核->主管审核->经理审核->副总审批->已生效(发证)
 *   批量:草稿->合规审核->主管审核->经理审核->副总审批->领导小组审批->已生效(发证)
 *     (数字化部认定细分为主管/经理/副总三节点,与专项同名同粒度;末节点为领导小组决策)
 *   任一环节可驳回。
 */
@TableName("IM_AUTH_APPLY")
public class AuthApply extends BaseEntity {

    public static final String STATUS_DRAFT = "草稿";
    public static final String STATUS_COMPLIANCE = "合规审核中";      // 合规管控小组
    public static final String STATUS_BUSINESS = "业务审核中";        // 专项:业务部门经理/高级经理
    public static final String STATUS_MANAGER = "主管审核中";         // 专项:数字化部主管
    public static final String STATUS_DIRECTOR = "经理审核中";        // 专项:数字化部经理/高级经理
    public static final String STATUS_VP = "副总审批中";             // 副总经理/总经理(批量复用同节点)
    public static final String STATUS_LEADERSHIP = "领导小组审批中";   // 批量:领导小组办公室批准
    public static final String STATUS_EFFECTIVE = "已生效";
    public static final String STATUS_REJECTED = "已驳回";
    /** 兼容旧值 */
    public static final String STATUS_REVIEW = "审核中";

    public static final String MODE_SPECIAL = "一事一议";
    public static final String MODE_BATCH = "批量";

    @TableId(value = "CEC_APPLY_ID", type = IdType.ASSIGN_UUID)
    private String applyId;

    @TableField("CEC_APPLY_NO")
    private String applyNo;

    /** 授权模式:一事一议 / 批量 */
    @TableField("CEC_AUTH_MODE")
    private String authMode;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_ASSET_NAME")
    private String assetName;

    /** 引用的权益卡片(先确后授依据) */
    @TableField("CEC_EQUITY_CARD_ID")
    private String equityCardId;

    /** 被授权方 */
    @TableField("CEC_GRANTEE_ORG")
    private String granteeOrg;

    /** 授权权益类型:加工使用权 / 产品经营权 */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    /** 使用场景 */
    @TableField("CEC_SCENARIO")
    private String scenario;

    /** 授权范围 */
    @TableField("CEC_SCOPE")
    private String scope;

    @TableField("CEC_VALID_DATE")
    private LocalDateTime validDate;

    @TableField("CEC_STATUS")
    private String status;

    @TableField("CEC_REJECT_REASON")
    private String rejectReason;

    /** 当前审批节点状态序号(在审批链中的位置) */
    @TableField("CEC_CURRENT_NODE")
    private Integer currentNode;

    /** 关联的批量授权清单(表6)ID,仅批量模式 */
    @TableField("CEC_BATCH_LIST_ID")
    private String batchListId;

    /** 是否需保密承诺函(附录E:涉敏感/第三方数据授权) */
    @TableField("CEC_NEED_CONFIDENTIALITY")
    private Boolean needConfidentiality;

    /** 保密承诺函文件地址(附录E) */
    @TableField("CEC_CONFIDENTIALITY_FILE")
    private String confidentialityFile;

    /** 所属业务域(附录F 表5) */
    @TableField("CEC_BUSINESS_DOMAIN")
    private String businessDomain;

    /** 是否跨区域/跨域(附录F 表5) */
    @TableField("CEC_CROSS_REGION")
    private Boolean crossRegion;

    /** 涉及第三方来源方式(附录F 表5) */
    @TableField("CEC_THIRD_PARTY_SOURCE")
    private String thirdPartySource;

    /** 第三方许可凭证或说明(附录F 表5) */
    @TableField("CEC_THIRD_PARTY_LICENSE")
    private String thirdPartyLicense;

    /** 涉及个人隐私/商业秘密(附录F 表5) */
    @TableField("CEC_SENSITIVE_TYPE")
    private String sensitiveType;

    /** 信息授权协议(附录F 表5) */
    @TableField("CEC_INFO_AUTH_AGREEMENT")
    private String infoAuthAgreement;

    /** 申请单位主管(附录F 表5) */
    @TableField("CEC_APPLICANT_MANAGER")
    private String applicantManager;

    /** 联系方式(附录F 表5) */
    @TableField("CEC_CONTACT")
    private String contactInfo;

    public Boolean getNeedConfidentiality() {
        return needConfidentiality;
    }

    public void setNeedConfidentiality(Boolean needConfidentiality) {
        this.needConfidentiality = needConfidentiality;
    }

    public String getConfidentialityFile() {
        return confidentialityFile;
    }

    public void setConfidentialityFile(String confidentialityFile) {
        this.confidentialityFile = confidentialityFile;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public void setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
    }

    public Boolean getCrossRegion() {
        return crossRegion;
    }

    public void setCrossRegion(Boolean crossRegion) {
        this.crossRegion = crossRegion;
    }

    public String getThirdPartySource() {
        return thirdPartySource;
    }

    public void setThirdPartySource(String thirdPartySource) {
        this.thirdPartySource = thirdPartySource;
    }

    public String getThirdPartyLicense() {
        return thirdPartyLicense;
    }

    public void setThirdPartyLicense(String thirdPartyLicense) {
        this.thirdPartyLicense = thirdPartyLicense;
    }

    public String getSensitiveType() {
        return sensitiveType;
    }

    public void setSensitiveType(String sensitiveType) {
        this.sensitiveType = sensitiveType;
    }

    public String getInfoAuthAgreement() {
        return infoAuthAgreement;
    }

    public void setInfoAuthAgreement(String infoAuthAgreement) {
        this.infoAuthAgreement = infoAuthAgreement;
    }

    public String getApplicantManager() {
        return applicantManager;
    }

    public void setApplicantManager(String applicantManager) {
        this.applicantManager = applicantManager;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public Integer getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Integer currentNode) {
        this.currentNode = currentNode;
    }

    public String getBatchListId() {
        return batchListId;
    }

    public void setBatchListId(String batchListId) {
        this.batchListId = batchListId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getApplyNo() {
        return applyNo;
    }

    public void setApplyNo(String applyNo) {
        this.applyNo = applyNo;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
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

    public String getEquityCardId() {
        return equityCardId;
    }

    public void setEquityCardId(String equityCardId) {
        this.equityCardId = equityCardId;
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

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
