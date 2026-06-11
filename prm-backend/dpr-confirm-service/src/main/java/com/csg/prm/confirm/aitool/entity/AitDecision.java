package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-确权决策建议(M3 / SW-007~008)。
 * 关键因子加权 + 结果预测 + 权益分割 + 决策建议 + 证据链。对应物理表 IM_AIT_DECISION。
 */
@TableName("IM_AIT_DECISION")
public class AitDecision extends BaseEntity {

    public static final String PRED_PASS = "建议通过";
    public static final String PRED_SUPPLEMENT = "建议补充材料";
    public static final String PRED_REJECT = "建议驳回";

    @TableId(value = "CEC_DECISION_ID", type = IdType.ASSIGN_UUID)
    private String decisionId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 确权结果预测:建议通过/补充材料/驳回 */
    @TableField("CEC_PREDICTION")
    private String prediction;

    /** 综合确权评分(0-100) */
    @TableField("CEC_SCORE")
    private Double score;

    /** 关键因子明细(JSON:名称/权重/得分) */
    @TableField("CEC_FACTORS_JSON")
    private String factorsJson;

    /** 优势因子 */
    @TableField("CEC_STRENGTH")
    private String strengthFactors;

    /** 短板因子 */
    @TableField("CEC_WEAKNESS")
    private String weakFactors;

    /** 权益分割方案(多主体场景) */
    @TableField("CEC_SPLIT_PLAN")
    private String splitPlan;

    /** 决策理由 */
    @TableField("CEC_REASON")
    private String reason;

    /** 需补充材料 */
    @TableField("CEC_SUPPLEMENT")
    private String supplementMaterials;

    /** 待处置冲突 */
    @TableField("CEC_PENDING_CONFLICTS")
    private String pendingConflicts;

    /** RAG 智能建议(大模型) */
    @TableField("CEC_RAG_ADVICE")
    private String ragAdvice;

    /** AI 预测结论(大模型基于 RAG 上下文给出,与规则预测对照) */
    @TableField("CEC_AI_PREDICTION")
    private String aiPrediction;

    /** RAG 法规/知识引用条目 */
    @TableField("CEC_RAG_CITATIONS")
    private String ragCitations;

    /** 证据链 SM3 指纹(材料片段+知识库+规则+模型理由+结论) */
    @TableField("CEC_EVIDENCE_CHAIN")
    private String evidenceChain;

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
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

    public String getPrediction() {
        return prediction;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getFactorsJson() {
        return factorsJson;
    }

    public void setFactorsJson(String factorsJson) {
        this.factorsJson = factorsJson;
    }

    public String getStrengthFactors() {
        return strengthFactors;
    }

    public void setStrengthFactors(String strengthFactors) {
        this.strengthFactors = strengthFactors;
    }

    public String getWeakFactors() {
        return weakFactors;
    }

    public void setWeakFactors(String weakFactors) {
        this.weakFactors = weakFactors;
    }

    public String getSplitPlan() {
        return splitPlan;
    }

    public void setSplitPlan(String splitPlan) {
        this.splitPlan = splitPlan;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSupplementMaterials() {
        return supplementMaterials;
    }

    public void setSupplementMaterials(String supplementMaterials) {
        this.supplementMaterials = supplementMaterials;
    }

    public String getPendingConflicts() {
        return pendingConflicts;
    }

    public void setPendingConflicts(String pendingConflicts) {
        this.pendingConflicts = pendingConflicts;
    }

    public String getRagAdvice() {
        return ragAdvice;
    }

    public void setRagAdvice(String ragAdvice) {
        this.ragAdvice = ragAdvice;
    }

    public String getAiPrediction() {
        return aiPrediction;
    }

    public void setAiPrediction(String aiPrediction) {
        this.aiPrediction = aiPrediction;
    }

    public String getRagCitations() {
        return ragCitations;
    }

    public void setRagCitations(String ragCitations) {
        this.ragCitations = ragCitations;
    }

    public String getEvidenceChain() {
        return evidenceChain;
    }

    public void setEvidenceChain(String evidenceChain) {
        this.evidenceChain = evidenceChain;
    }
}
