package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权 Agent 审核结果(可研 3.1):多阶段链路 + 双通道 + 多级机制的结构化结论。对应 IM_AIT_AUDIT_RESULT。
 */
@TableName("IM_AIT_AUDIT_RESULT")
public class AitAuditResult extends BaseEntity {

    public static final String CH_FAST = "快速通道";
    public static final String CH_DEEP = "深度审核";

    @TableId(value = "CEC_AUDIT_ID", type = IdType.ASSIGN_UUID)
    private String auditId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_CHANNEL")
    private String channel;

    @TableField("CEC_DATA_CLASS")
    private String dataClass;

    @TableField("CEC_DATA_GRADE")
    private String dataGrade;

    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    @TableField("CEC_AUTH_ADVICE")
    private String authAdvice;

    @TableField("CEC_AUTH_LEVEL")
    private String authLevel;

    @TableField("CEC_RESTRICTIONS")
    private String restrictions;

    @TableField("CEC_RISK_LEVEL")
    private String riskLevel;

    @TableField("CEC_SUPPLEMENT")
    private String supplement;

    @TableField("CEC_REASON")
    private String reason;

    @TableField("CEC_CITATIONS")
    private String citations;

    @TableField("CEC_ACTION")
    private String action;

    @TableField("CEC_SCORE")
    private Double score;

    @TableField("CEC_STAGE_TRACE_JSON")
    private String stageTraceJson;

    @TableField("CEC_TOOL_TRACE_JSON")
    private String toolTraceJson;

    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getDataClass() { return dataClass; }
    public void setDataClass(String dataClass) { this.dataClass = dataClass; }
    public String getDataGrade() { return dataGrade; }
    public void setDataGrade(String dataGrade) { this.dataGrade = dataGrade; }
    public String getRightType() { return rightType; }
    public void setRightType(String rightType) { this.rightType = rightType; }
    public String getAuthAdvice() { return authAdvice; }
    public void setAuthAdvice(String authAdvice) { this.authAdvice = authAdvice; }
    public String getAuthLevel() { return authLevel; }
    public void setAuthLevel(String authLevel) { this.authLevel = authLevel; }
    public String getRestrictions() { return restrictions; }
    public void setRestrictions(String restrictions) { this.restrictions = restrictions; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getSupplement() { return supplement; }
    public void setSupplement(String supplement) { this.supplement = supplement; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getCitations() { return citations; }
    public void setCitations(String citations) { this.citations = citations; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getStageTraceJson() { return stageTraceJson; }
    public void setStageTraceJson(String stageTraceJson) { this.stageTraceJson = stageTraceJson; }
    public String getToolTraceJson() { return toolTraceJson; }
    public void setToolTraceJson(String toolTraceJson) { this.toolTraceJson = toolTraceJson; }
}
