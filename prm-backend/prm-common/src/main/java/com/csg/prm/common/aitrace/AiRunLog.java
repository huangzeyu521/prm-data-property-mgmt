package com.csg.prm.common.aitrace;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 大模型校验操作留痕(南网"全流程留痕追溯",跨域共享:确权 + 授权)。
 * 每次内生 AI 调用逐条落库——业务域、能力类型、模型、输入摘要、输出、耗时、SM3 指纹(防篡改)、触发人。
 * 供人工审核「AI 校验过程回放」与可复盘、可审计。物理表 IM_DPR_AI_RUNLOG。
 */
@TableName("IM_DPR_AI_RUNLOG")
public class AiRunLog extends BaseEntity {

    /** 业务域 */
    public static final String BIZ_CONFIRM = "确权";
    public static final String BIZ_AUTHORIZE = "授权";

    /** 能力类型常量(确权) */
    public static final String CAP_PARSE = "智能解析";
    public static final String CAP_DECISION = "决策研判";
    public static final String CAP_CONFLICT = "冲突识别";
    public static final String CAP_MATERIAL_CHECK = "材料校验";
    /** 能力类型常量(授权) */
    public static final String CAP_AUTH_MATERIAL_CHECK = "授权材料校验";
    public static final String CAP_AUTH_PRECHECK = "合规预审";
    public static final String CAP_AUTH_BATCH_INTENT = "批量意图解析";

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    /** 业务域(确权/授权) */
    @TableField("CEC_BIZ_TYPE")
    private String bizType;

    /** 业务主键(确权/授权申请ID),物理列沿用 CEC_APPLY_ID */
    @TableField("CEC_APPLY_ID")
    private String bizId;

    /** 能力类型 */
    @TableField("CEC_CAPABILITY")
    private String capability;

    /** 模型标识(qwen3-max / local-rule-stub …) */
    @TableField("CEC_MODEL")
    private String model;

    /** 输入摘要(便于复盘,避免存全量) */
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

    /** 触发人 */
    @TableField("CEC_TRIGGER_USER")
    private String triggerUser;

    /** 关键节点上链存证ID(可选) */
    @TableField("CEC_EVIDENCE_ID")
    private String evidenceId;

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
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
