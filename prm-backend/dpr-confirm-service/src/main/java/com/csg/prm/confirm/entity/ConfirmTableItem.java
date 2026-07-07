package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 确权申请-表级数据清单(对齐《数据确权信息汇总表》/补录工单 M02 元数据-系统数据表)。
 * 确权粒度为数据库表级:实例/Schema/表 + 密级 + 来源判定(A-F) + G/H/I/J 信息识别。
 */
@TableName("IM_CONFIRM_TABLE_ITEM")
public class ConfirmTableItem extends BaseEntity {

    /** 密级标准值域(汇总表) */
    public static final String[] SECRET_LEVELS = {"不涉密", "核心商密", "普通商密", "工作秘密", "敏感信息"};

    @TableId(value = "CEC_ITEM_ID", type = IdType.ASSIGN_UUID)
    private String itemId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 实例名或TNS */
    @TableField("CEC_INSTANCE_NAME")
    private String instanceName;

    /** schema名称/模式名称 */
    @TableField("CEC_SCHEMA_NAME")
    private String schemaName;

    /** 表代码 */
    @TableField("CEC_TABLE_CODE")
    private String tableCode;

    /** 表名称 */
    @TableField("CEC_TABLE_NAME")
    private String tableName;

    /** 表注释 */
    @TableField("CEC_TABLE_COMMENT")
    private String tableComment;

    /** 密级:不涉密/核心商密/普通商密/工作秘密/敏感信息 */
    @TableField("CEC_SECRET_LEVEL")
    private String secretLevel;

    /** 数据来源信息判定(A自行生产/B公开采集/C公共授权/D公共生产/E交易采购/F其他) */
    @TableField("CEC_SOURCE_TYPE")
    private String sourceType;

    /** 来源主体名称 */
    @TableField("CEC_SOURCE_SUBJECT")
    private String sourceSubject;

    /** 来源说明 */
    @TableField("CEC_SOURCE_DESC")
    private String sourceDesc;

    /** G 是否涉及行政监管要求(是/否) */
    @TableField("CEC_G_FLAG")
    @JsonProperty("gFlag")
    private String gFlag;

    /** G 信息识别关联主体说明(监管机构部门名称) */
    @TableField("CEC_G_SUBJECT")
    @JsonProperty("gSubject")
    private String gSubject;

    /** H 是否涉及用户个人/家庭隐私(是/否) */
    @TableField("CEC_H_FLAG")
    @JsonProperty("hFlag")
    private String hFlag;

    /** H 信息识别关联主体说明 */
    @TableField("CEC_H_SUBJECT")
    @JsonProperty("hSubject")
    private String hSubject;

    /** I 是否涉及第三方商业机密(是/否) */
    @TableField("CEC_I_FLAG")
    @JsonProperty("iFlag")
    private String iFlag;

    /** I 信息识别关联主体说明 */
    @TableField("CEC_I_SUBJECT")
    @JsonProperty("iSubject")
    private String iSubject;

    /** J 是否存在其他数据权益约束协议(是/否) */
    @TableField("CEC_J_FLAG")
    @JsonProperty("jFlag")
    private String jFlag;

    /** J 信息识别关联主体说明 */
    @TableField("CEC_J_SUBJECT")
    @JsonProperty("jSubject")
    private String jSubject;

    /** 表2·权益风险:该表是否存在未清晰约定的潜在侵权风险(逐表) */
    @TableField("CEC_RISK")
    private String riskDesc;

    /** 表2·来源凭证附件(逐表·来源 B–F 的证明材料;逐表凭证单一真源。字段名对齐前端 JSON key) */
    @TableField("CEC_SRC_ATT")
    private String sourceAttachment;

    /** 表2·G 行政监管关联资料附件(逐表) */
    @TableField("CEC_G_ATT")
    private String checkAttachment;

    /** 表2·H 个人/家庭隐私关联资料附件(逐表) */
    @TableField("CEC_H_ATT")
    private String privacyAttachment;

    /** 表2·I 第三方商业机密关联资料附件(逐表) */
    @TableField("CEC_I_ATT")
    private String busSecretAttachment;

    /** 表2·J 其他第三方机构协议关联资料附件(逐表) */
    @TableField("CEC_J_ATT")
    private String equityAttachment;

    /** 逐表上传件 materialId(供在线预览;字节存于 ConfirmMaterial,此处只存引用) */
    @TableField("CEC_SRC_MID")
    private String sourceMatId;

    @TableField("CEC_G_MID")
    private String checkMatId;

    @TableField("CEC_H_MID")
    private String privacyMatId;

    @TableField("CEC_I_MID")
    private String busSecretMatId;

    @TableField("CEC_J_MID")
    private String equityMatId;

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getTableCode() {
        return tableCode;
    }

    public void setTableCode(String tableCode) {
        this.tableCode = tableCode;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public String getSecretLevel() {
        return secretLevel;
    }

    public void setSecretLevel(String secretLevel) {
        this.secretLevel = secretLevel;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceSubject() {
        return sourceSubject;
    }

    public void setSourceSubject(String sourceSubject) {
        this.sourceSubject = sourceSubject;
    }

    public String getSourceDesc() {
        return sourceDesc;
    }

    public void setSourceDesc(String sourceDesc) {
        this.sourceDesc = sourceDesc;
    }

    public String getGFlag() {
        return gFlag;
    }

    public void setGFlag(String gFlag) {
        this.gFlag = gFlag;
    }

    public String getGSubject() {
        return gSubject;
    }

    public void setGSubject(String gSubject) {
        this.gSubject = gSubject;
    }

    public String getHFlag() {
        return hFlag;
    }

    public void setHFlag(String hFlag) {
        this.hFlag = hFlag;
    }

    public String getHSubject() {
        return hSubject;
    }

    public void setHSubject(String hSubject) {
        this.hSubject = hSubject;
    }

    public String getIFlag() {
        return iFlag;
    }

    public void setIFlag(String iFlag) {
        this.iFlag = iFlag;
    }

    public String getISubject() {
        return iSubject;
    }

    public void setISubject(String iSubject) {
        this.iSubject = iSubject;
    }

    public String getJFlag() {
        return jFlag;
    }

    public void setJFlag(String jFlag) {
        this.jFlag = jFlag;
    }

    public String getJSubject() {
        return jSubject;
    }

    public void setJSubject(String jSubject) {
        this.jSubject = jSubject;
    }

    public String getRiskDesc() {
        return riskDesc;
    }

    public void setRiskDesc(String riskDesc) {
        this.riskDesc = riskDesc;
    }

    public String getSourceAttachment() {
        return sourceAttachment;
    }

    public void setSourceAttachment(String sourceAttachment) {
        this.sourceAttachment = sourceAttachment;
    }

    public String getCheckAttachment() {
        return checkAttachment;
    }

    public void setCheckAttachment(String checkAttachment) {
        this.checkAttachment = checkAttachment;
    }

    public String getPrivacyAttachment() {
        return privacyAttachment;
    }

    public void setPrivacyAttachment(String privacyAttachment) {
        this.privacyAttachment = privacyAttachment;
    }

    public String getBusSecretAttachment() {
        return busSecretAttachment;
    }

    public void setBusSecretAttachment(String busSecretAttachment) {
        this.busSecretAttachment = busSecretAttachment;
    }

    public String getEquityAttachment() {
        return equityAttachment;
    }

    public void setEquityAttachment(String equityAttachment) {
        this.equityAttachment = equityAttachment;
    }

    public String getSourceMatId() {
        return sourceMatId;
    }

    public void setSourceMatId(String sourceMatId) {
        this.sourceMatId = sourceMatId;
    }

    public String getCheckMatId() {
        return checkMatId;
    }

    public void setCheckMatId(String checkMatId) {
        this.checkMatId = checkMatId;
    }

    public String getPrivacyMatId() {
        return privacyMatId;
    }

    public void setPrivacyMatId(String privacyMatId) {
        this.privacyMatId = privacyMatId;
    }

    public String getBusSecretMatId() {
        return busSecretMatId;
    }

    public void setBusSecretMatId(String busSecretMatId) {
        this.busSecretMatId = busSecretMatId;
    }

    public String getEquityMatId() {
        return equityMatId;
    }

    public void setEquityMatId(String equityMatId) {
        this.equityMatId = equityMatId;
    }
}
