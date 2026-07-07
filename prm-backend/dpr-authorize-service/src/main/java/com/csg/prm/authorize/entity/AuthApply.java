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
 *   专项一事一议(35号文 附录C表2 10-130):草稿->单位初审->合规审核->业务审核->主管审核->经理审核->副总审批->批准(待双签)
 *     ->协议双签+承诺函归档后置「已生效」(生效副作用:生效记录/台账/卡片回写,先签约后执行授权)
 *   批量:草稿->合规审核->主管审核->经理审核->副总审批->领导小组审批->已生效
 *     (数字化部认定细分为主管/经理/副总三节点,与专项同名同粒度;末节点为领导小组决策)
 *   任一环节可驳回。
 */
@TableName("IM_AUTH_APPLY")
public class AuthApply extends BaseEntity {

    public static final String STATUS_DRAFT = "草稿";
    public static final String STATUS_UNIT = "单位初审中";            // 专项:申报单位内部初审(表2 步骤20-50合并节点)
    public static final String STATUS_COMPLIANCE = "合规审核中";      // 合规管控小组
    public static final String STATUS_BUSINESS = "业务审核中";        // 专项:业务部门经理/高级经理
    public static final String STATUS_MANAGER = "主管审核中";         // 专项:数字化部主管
    public static final String STATUS_DIRECTOR = "经理审核中";        // 专项:数字化部经理/高级经理
    public static final String STATUS_VP = "副总审批中";             // 副总经理/总经理(批量复用同节点)
    public static final String STATUS_LEADERSHIP = "领导小组审批中";   // 批量:领导小组办公室批准
    public static final String STATUS_APPROVED = "批准";              // 专项终审通过(待双签;协议归档后置已生效)
    public static final String STATUS_EFFECTIVE = "已生效";
    public static final String STATUS_REJECTED = "已驳回";
    /** 申请人主动撤回(审批中 -> 已撤回中间态,可修改重提);与确权域 ConfirmApply.STATUS_WITHDRAWN 对称 */
    public static final String STATUS_WITHDRAWN = "已撤回";
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

    /** 所属系统名(向导选表时平台带出;assetId 为库表/卡片级 ID,不含 SYS: 前缀,故需单独落库供审核台/历史/清单页显示) */
    @TableField("CEC_SYSTEM_NAME")
    private String systemName;

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

    /** 使用目的摘要(表5「使用场景及目的摘要」的目的部分;一事一议自由文本,选场景默认带出模板可改) */
    @TableField("CEC_PURPOSE_NOTE")
    private String purposeNote;

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

    /** 申请单号(一事一议多表分组;同号=一份表5申请单的多张数据表)。批量模式空。 */
    @TableField("CEC_FORM_NO")
    private String formNo;

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

    /** 模式名称(附录F 表5/表6;来源数据资产卡片) */
    @TableField("CEC_SCHEMA_NAME")
    private String schemaName;

    /** 利益分配约定(授权协议附录D §3.4.4;如:免费内部共享/按次计费/收益分成) */
    @TableField("CEC_BENEFIT_ALLOC")
    private String benefitAllocation;

    /** 安全保障要求(授权协议附录D §3.4.4;如:加密传输/最小授权访问控制/操作留痕审计) */
    @TableField("CEC_SECURITY_REQ")
    private String securityReq;

    /** 信息授权协议(附录F 表5) */
    @TableField("CEC_INFO_AUTH_AGREEMENT")
    private String infoAuthAgreement;

    /** 申请单位主管(附录F 表5) */
    @TableField("CEC_APPLICANT_MANAGER")
    private String applicantManager;

    /** 联系方式(附录F 表5) */
    @TableField("CEC_CONTACT")
    private String contactInfo;

    /** 提交前 AI 校验结果快照(防篡改包),供人工审核复核·可审计 */
    @TableField("CEC_AI_SNAPSHOT")
    private String aiSnapshot;

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

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getBenefitAllocation() {
        return benefitAllocation;
    }

    public void setBenefitAllocation(String benefitAllocation) {
        this.benefitAllocation = benefitAllocation;
    }

    public String getSecurityReq() {
        return securityReq;
    }

    public void setSecurityReq(String securityReq) {
        this.securityReq = securityReq;
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

    public String getAiSnapshot() {
        return aiSnapshot;
    }

    public void setAiSnapshot(String aiSnapshot) {
        this.aiSnapshot = aiSnapshot;
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

    public String getFormNo() {
        return formNo;
    }

    public void setFormNo(String formNo) {
        this.formNo = formNo;
    }

    public String getPurposeNote() {
        return purposeNote;
    }

    public void setPurposeNote(String purposeNote) {
        this.purposeNote = purposeNote;
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

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
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
