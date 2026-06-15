package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-权属冲突(M2 / SW-006)。
 * 冲突类型:主体冲突/范围冲突/时效冲突/历史记录冲突。对应物理表 IM_AIT_CONFLICT。
 */
@TableName("IM_AIT_CONFLICT")
public class AitConflict extends BaseEntity {

    public static final String TYPE_SUBJECT = "主体冲突";
    public static final String TYPE_SCOPE = "范围冲突";
    public static final String TYPE_VALIDITY = "时效冲突";
    public static final String TYPE_HISTORY = "历史记录冲突";
    public static final String TYPE_RIGHTTYPE = "类型冲突";

    public static final String STATUS_OPEN = "待处置";
    public static final String STATUS_RESOLVED = "已处置";

    @TableId(value = "CEC_CONFLICT_ID", type = IdType.ASSIGN_UUID)
    private String conflictId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 冲突类型 */
    @TableField("CEC_CONFLICT_TYPE")
    private String conflictType;

    /** 冲突来源:证明材料矛盾/历史授权未注销/申请信息错误 */
    @TableField("CEC_CONFLICT_SOURCE")
    private String conflictSource;

    @TableField("CEC_CONFLICT_DESC")
    private String conflictDesc;

    /** 影响范围 */
    @TableField("CEC_IMPACT_SCOPE")
    private String impactScope;

    /** 风险等级:低/中/高 */
    @TableField("CEC_RISK_LEVEL")
    private String riskLevel;

    /** 处置建议 */
    @TableField("CEC_SUGGESTION")
    private String suggestion;

    @TableField("CEC_STATUS")
    private String status;

    /** #3 追溯:关联历史确权记录/主张编号 */
    @TableField("CEC_RELATED_RECORD_NO")
    private String relatedRecordNo;

    /** #3 追溯:涉及的具体冲突字段 */
    @TableField("CEC_CONFLICT_FIELDS")
    private String conflictFields;

    /** #3 追溯:相关协议条款/法规依据引用 */
    @TableField("CEC_CLAUSE_REF")
    private String clauseRef;

    /** #4 影响:法律风险等级 */
    @TableField("CEC_LEGAL_RISK")
    private String legalRisk;

    public String getRelatedRecordNo() { return relatedRecordNo; }
    public void setRelatedRecordNo(String relatedRecordNo) { this.relatedRecordNo = relatedRecordNo; }
    public String getConflictFields() { return conflictFields; }
    public void setConflictFields(String conflictFields) { this.conflictFields = conflictFields; }
    public String getClauseRef() { return clauseRef; }
    public void setClauseRef(String clauseRef) { this.clauseRef = clauseRef; }
    public String getLegalRisk() { return legalRisk; }
    public void setLegalRisk(String legalRisk) { this.legalRisk = legalRisk; }

    public String getConflictId() {
        return conflictId;
    }

    public void setConflictId(String conflictId) {
        this.conflictId = conflictId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public String getConflictSource() {
        return conflictSource;
    }

    public void setConflictSource(String conflictSource) {
        this.conflictSource = conflictSource;
    }

    public String getConflictDesc() {
        return conflictDesc;
    }

    public void setConflictDesc(String conflictDesc) {
        this.conflictDesc = conflictDesc;
    }

    public String getImpactScope() {
        return impactScope;
    }

    public void setImpactScope(String impactScope) {
        this.impactScope = impactScope;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
