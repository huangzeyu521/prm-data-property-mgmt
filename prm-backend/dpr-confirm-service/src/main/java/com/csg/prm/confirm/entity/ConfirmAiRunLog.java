package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 大模型校验操作留痕(南网"全流程留痕追溯"):确权内生 AI 每次调用逐条落库——
 * 能力类型、模型、输入摘要、输出、耗时、触发人、SM3 指纹(防篡改)+ 关键节点存证ID。
 * 供人工预审与审计「AI 校验过程回放」与可复盘、可审计。物理表 IM_DPR_AI_RUNLOG。
 */
@TableName("IM_DPR_AI_RUNLOG")
public class ConfirmAiRunLog extends BaseEntity {

    /** 能力类型常量 */
    public static final String CAP_PARSE = "智能解析";
    public static final String CAP_DECISION = "决策研判";
    public static final String CAP_CONFLICT = "冲突识别";
    public static final String CAP_MATERIAL_CHECK = "材料校验";

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 能力类型(智能解析/决策研判/冲突识别/材料校验) */
    @TableField("CEC_CAPABILITY")
    private String capability;

    /** 模型标识(qwen3-max / local-rule-stub …) */
    @TableField("CEC_MODEL")
    private String model;

    /** 输入摘要(申请要素/材料概要,便于复盘,避免存全量) */
    @TableField("CEC_INPUT_SUMMARY")
    private String inputSummary;

    /** 模型输出(原样留痕,供回放) */
    @TableField("CEC_OUTPUT")
    private String output;

    /** 调用耗时(毫秒) */
    @TableField("CEC_DURATION_MS")
    private Long durationMs;

    /** 输出 SM3 指纹(防篡改:回放时重算比对) */
    @TableField("CEC_SM3_HASH")
    private String sm3Hash;

    /** 触发人(操作留痕) */
    @TableField("CEC_TRIGGER_USER")
    private String triggerUser;

    /** 关键节点上链存证ID(可选,链上锚定) */
    @TableField("CEC_EVIDENCE_ID")
    private String evidenceId;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInputSummary() {
        return inputSummary;
    }

    public void setInputSummary(String inputSummary) {
        this.inputSummary = inputSummary;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getSm3Hash() {
        return sm3Hash;
    }

    public void setSm3Hash(String sm3Hash) {
        this.sm3Hash = sm3Hash;
    }

    public String getTriggerUser() {
        return triggerUser;
    }

    public void setTriggerUser(String triggerUser) {
        this.triggerUser = triggerUser;
    }

    public String getEvidenceId() {
        return evidenceId;
    }

    public void setEvidenceId(String evidenceId) {
        this.evidenceId = evidenceId;
    }
}
