package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 审核证据链档案(3.2#4):关联 输入材料片段 / 知识库命中片段 / 规则命中项 / 大模型判断理由 / 最终结论,
 * 含 SM3 留痕,形成可复核·可留痕·可审计的审核档案。对应 IM_AIT_EVIDENCE。
 */
@TableName("IM_AIT_EVIDENCE")
public class AitEvidence extends BaseEntity {

    @TableId(value = "CEC_EVIDENCE_ID", type = IdType.ASSIGN_UUID)
    private String evidenceId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_MATERIAL_SNIPPETS_JSON")
    private String materialSnippetsJson;

    @TableField("CEC_KB_HITS_JSON")
    private String kbHitsJson;

    @TableField("CEC_RULE_HITS_JSON")
    private String ruleHitsJson;

    @TableField("CEC_MODEL_REASON")
    private String modelReason;

    @TableField("CEC_CONCLUSION")
    private String conclusion;

    @TableField("CEC_SM3_HASH")
    private String sm3Hash;

    public String getEvidenceId() { return evidenceId; }
    public void setEvidenceId(String evidenceId) { this.evidenceId = evidenceId; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public String getMaterialSnippetsJson() { return materialSnippetsJson; }
    public void setMaterialSnippetsJson(String materialSnippetsJson) { this.materialSnippetsJson = materialSnippetsJson; }
    public String getKbHitsJson() { return kbHitsJson; }
    public void setKbHitsJson(String kbHitsJson) { this.kbHitsJson = kbHitsJson; }
    public String getRuleHitsJson() { return ruleHitsJson; }
    public void setRuleHitsJson(String ruleHitsJson) { this.ruleHitsJson = ruleHitsJson; }
    public String getModelReason() { return modelReason; }
    public void setModelReason(String modelReason) { this.modelReason = modelReason; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public String getSm3Hash() { return sm3Hash; }
    public void setSm3Hash(String sm3Hash) { this.sm3Hash = sm3Hash; }
}
