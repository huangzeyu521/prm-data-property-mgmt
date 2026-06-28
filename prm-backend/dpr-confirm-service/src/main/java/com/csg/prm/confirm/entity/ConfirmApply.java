package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 数据确权申请单(对应界面 IM-DAM-DPR-02-001-001-003)。对应物理表 IM_CONFIRM_APPLY。
 * 审批流(对齐附录F 4.1 八节点):草稿 -> 合规审核中(50,合规管控小组,生成表3/表4+认定意见)
 *   -> 主管复核中(60,数字化部主管) -> 经理终审中(70,经理/高级经理) -> 已完成(80,制卡);任一环节可驳回。
 */
@TableName("IM_CONFIRM_APPLY")
public class ConfirmApply extends BaseEntity {

    public static final String STATUS_DRAFT = "草稿";
    public static final String STATUS_PRECHECK = "人工预审中";     // 节点40 人工预审(复核AI校验结果)
    public static final String STATUS_COMPLIANCE = "合规审核中";   // 节点50 合规管控小组
    public static final String STATUS_MANAGER = "主管复核中";      // 节点60 数字化部主管
    public static final String STATUS_DIRECTOR = "经理终审中";     // 节点70 经理/高级经理
    public static final String STATUS_DONE = "已完成";            // 节点80 制卡归集
    public static final String STATUS_REJECTED = "已驳回";
    public static final String STATUS_WITHDRAWN = "已撤回";        // 申请人审批中主动撤回(中间态,可重新编辑提交)

    /** 当前节点编号(附录F 流程步骤编号 40 人工预审/50/60/70/80) */
    public static final int NODE_PRECHECK = 40;
    public static final int NODE_COMPLIANCE = 50;
    public static final int NODE_MANAGER = 60;
    public static final int NODE_DIRECTOR = 70;
    public static final int NODE_DONE = 80;

    /** 人工预审依据:提交时固化的 AI 校验结果快照(JSON;材料AI校验+规则完整性+权益归集),供预审人完整复核·可追溯 */
    @TableField("CEC_AI_SNAPSHOT")
    private String aiSnapshot;

    @TableId(value = "CEC_APPLY_ID", type = IdType.ASSIGN_UUID)
    private String applyId;

    @TableField("CEC_APPLY_NO")
    private String applyNo;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_ASSET_NAME")
    private String assetName;

    /** 权属类型:持有权/加工使用权/产品经营权 */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    /** 用途说明 */
    @TableField("CEC_PURPOSE")
    private String purpose;

    /** 权属人/权利主体 */
    @TableField("CEC_RIGHT_HOLDER")
    private String rightHolder;

    @TableField("CEC_RESP_DEPT")
    private String respDept;

    /** 主体层级(表1 公司主体口径:公司总部/分省公司/专业子公司),来自 SYS_ORGANIZATION.ORG_TYPE */
    @TableField("CEC_SUBJECT_LEVEL")
    private String subjectLevel;

    @TableField("CEC_VALID_DATE")
    private LocalDateTime validDate;

    /** 申请状态 */
    @TableField("CEC_STATUS")
    private String status;

    /** 驳回原因 */
    @TableField("CEC_REJECT_REASON")
    private String rejectReason;

    /** 当前节点编号(50/60/70/80) */
    @TableField("CEC_CURRENT_NODE")
    private Integer currentNode;

    /** 表1 来源权益识别(多选,逗号分隔):A自行生产/B公开采集/C公共数据授权/D共同生产/E交易采购/F其他 */
    @TableField("CEC_SOURCE_IDENT")
    private String sourceIdentification;

    /** 表1 信息关联识别(多选,逗号分隔):G行政监管/H个人家庭隐私/I第三方商业机密/J其他第三方协议 */
    @TableField("CEC_RELATION_IDENT")
    private String relationIdentification;

    /** 是否涉及第三方权益(表2) */
    @TableField("CEC_INVOLVES_THIRD_PARTY")
    private Boolean involvesThirdParty;

    /** 第三方权益信息(表2:第三方机构、权益转移约定、许可凭证) */
    @TableField("CEC_THIRD_PARTY_INFO")
    private String thirdPartyInfo;

    /** 个人/家庭隐私(H)关联主体说明(工单 M02:涉及个人隐私时必填) */
    @TableField("CEC_PRIVACY_INFO")
    private String privacyInfo;

    /** 权益认定意见(合规管控小组/经理在节点50/70形成) */
    @TableField("CEC_RECOGNITION_OPINION")
    private String recognitionOpinion;

    /** 是否重确权工单(由权益动态监测识别数据新增/来源变更/到期联动派生,附录F 3.3.2) */
    @TableField("CEC_RE_CONFIRM")
    private Boolean reConfirm;

    /** 重确权来源依据(触发原因/来源预警ID) */
    @TableField("CEC_SOURCE_REF")
    private String sourceRef;

    /** 系统负责人姓名(附录F 表1) */
    @TableField("CEC_SYSTEM_OWNER")
    private String systemOwner;

    /** 系统负责人联系方式(附录F 表1) */
    @TableField("CEC_CONTACT")
    private String contactInfo;

    /** 登记类型:初始确权 / 确权变更(附录F 表1) */
    @TableField("CEC_REGISTER_TYPE")
    private String registerType;

    /** 变更触发类型(仅确权变更):数据新增/数据来源变更/管理要求变更/权益到期/其他(附录F 权益变更四类)。 */
    @TableField("CEC_CHANGE_TRIGGER")
    private String changeTrigger;

    /** P2 确权变更可追溯:基线引用(系统名/版本)、变更后版本号、变更摘要(前后 diff),供版本化留痕 */
    @TableField("CEC_BASELINE_REF")
    private String baselineRef;

    @TableField("CEC_CHANGE_VERSION")
    private Integer changeVersion;

    @TableField("CEC_CHANGE_SUMMARY")
    private String changeSummary;

    /** 申请模式:常规 / 一事一议(特殊事项单独审议) */
    @TableField("CEC_APPLY_MODE")
    private String applyMode;

    /** 管制属性:管制业务 / 非管制(权益归集判定关键输入,管制单位默认没有限经营权) */
    @TableField("CEC_REGULATED")
    private String regulated;

    /** 表2:来源主体名称(涉第三方权益) */
    @TableField("CEC_SOURCE_SUBJECT")
    private String sourceSubject;

    /** 表2:来源权益限制摘要 */
    @TableField("CEC_SOURCE_LIMIT")
    private String sourceLimit;

    /** 表2:信息识别关联主体说明 */
    @TableField("CEC_RELATION_SUBJECT")
    private String relationSubject;

    /** 表2:权益风险说明 */
    @TableField("CEC_EQUITY_RISK")
    private String equityRisk;

    public String getSystemOwner() {
        return systemOwner;
    }

    public void setSystemOwner(String systemOwner) {
        this.systemOwner = systemOwner;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getRegisterType() {
        return registerType;
    }

    public void setRegisterType(String registerType) {
        this.registerType = registerType;
    }

    public String getChangeTrigger() {
        return changeTrigger;
    }

    public void setChangeTrigger(String changeTrigger) {
        this.changeTrigger = changeTrigger;
    }

    public String getBaselineRef() {
        return baselineRef;
    }

    public void setBaselineRef(String baselineRef) {
        this.baselineRef = baselineRef;
    }

    public Integer getChangeVersion() {
        return changeVersion;
    }

    public void setChangeVersion(Integer changeVersion) {
        this.changeVersion = changeVersion;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getApplyMode() {
        return applyMode;
    }

    public void setApplyMode(String applyMode) {
        this.applyMode = applyMode;
    }

    public String getRegulated() {
        return regulated;
    }

    public void setRegulated(String regulated) {
        this.regulated = regulated;
    }

    public String getSourceSubject() {
        return sourceSubject;
    }

    public void setSourceSubject(String sourceSubject) {
        this.sourceSubject = sourceSubject;
    }

    public String getSourceLimit() {
        return sourceLimit;
    }

    public void setSourceLimit(String sourceLimit) {
        this.sourceLimit = sourceLimit;
    }

    public String getRelationSubject() {
        return relationSubject;
    }

    public void setRelationSubject(String relationSubject) {
        this.relationSubject = relationSubject;
    }

    public String getEquityRisk() {
        return equityRisk;
    }

    public void setEquityRisk(String equityRisk) {
        this.equityRisk = equityRisk;
    }

    public Boolean getReConfirm() {
        return reConfirm;
    }

    public void setReConfirm(Boolean reConfirm) {
        this.reConfirm = reConfirm;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public Integer getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(Integer currentNode) {
        this.currentNode = currentNode;
    }

    public String getSourceIdentification() {
        return sourceIdentification;
    }

    public void setSourceIdentification(String sourceIdentification) {
        this.sourceIdentification = sourceIdentification;
    }

    public String getRelationIdentification() {
        return relationIdentification;
    }

    public void setRelationIdentification(String relationIdentification) {
        this.relationIdentification = relationIdentification;
    }

    public Boolean getInvolvesThirdParty() {
        return involvesThirdParty;
    }

    public void setInvolvesThirdParty(Boolean involvesThirdParty) {
        this.involvesThirdParty = involvesThirdParty;
    }

    public String getThirdPartyInfo() {
        return thirdPartyInfo;
    }

    public void setThirdPartyInfo(String thirdPartyInfo) {
        this.thirdPartyInfo = thirdPartyInfo;
    }

    public String getPrivacyInfo() {
        return privacyInfo;
    }

    public void setPrivacyInfo(String privacyInfo) {
        this.privacyInfo = privacyInfo;
    }

    public String getRecognitionOpinion() {
        return recognitionOpinion;
    }

    public void setRecognitionOpinion(String recognitionOpinion) {
        this.recognitionOpinion = recognitionOpinion;
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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getAiSnapshot() {
        return aiSnapshot;
    }

    public void setAiSnapshot(String aiSnapshot) {
        this.aiSnapshot = aiSnapshot;
    }

    public String getRightHolder() {
        return rightHolder;
    }

    public void setRightHolder(String rightHolder) {
        this.rightHolder = rightHolder;
    }

    public String getRespDept() {
        return respDept;
    }

    public void setRespDept(String respDept) {
        this.respDept = respDept;
    }

    public String getSubjectLevel() {
        return subjectLevel;
    }

    public void setSubjectLevel(String subjectLevel) {
        this.subjectLevel = subjectLevel;
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
