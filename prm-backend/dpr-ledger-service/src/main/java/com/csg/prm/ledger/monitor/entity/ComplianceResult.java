package com.csg.prm.ledger.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 合规检查结果(对应界面 IM-DAM-DPR-01-001-002-003)。由巡检引擎自动生成。
 * 对应物理表 IM_COMPLIANCE_RESULT。
 */
@TableName("IM_COMPLIANCE_RESULT")
public class ComplianceResult extends BaseEntity {

    public static final String RESULT_PASS = "合规";
    public static final String RESULT_WARN = "警告";
    public static final String RESULT_FAIL = "不合规";

    @TableId(value = "CEC_CHECK_ID", type = IdType.ASSIGN_UUID)
    private String checkId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 数据表(库表名);所属系统由 assetId 去 SYS: 前缀派生 */
    @TableField("CEC_ASSET_NAME")
    private String assetName;

    @TableField("CEC_RULE_ID")
    private String ruleId;

    /** 检查结果:合规/警告/不合规 */
    @TableField("CEC_CHECK_RESULT")
    private String checkResult;

    @TableField("CEC_PROBLEM_DESC")
    private String problemDesc;

    @TableField("CEC_SUGGESTION")
    private String suggestion;

    @TableField("CEC_REPORT_URL")
    private String reportUrl;

    @TableField("CEC_CHECK_TIME")
    private LocalDateTime checkTime;

    @TableField("CEC_DISPOSE_STATUS")
    private String disposeStatus;

    /** 检查维度:有效期 / 权限范围 / 申请材料 / 协议内容 */
    @TableField("CEC_CHECK_DIM")
    private String checkDim;

    public String getCheckDim() {
        return checkDim;
    }

    public void setCheckDim(String checkDim) {
        this.checkDim = checkDim;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getProblemDesc() {
        return problemDesc;
    }

    public void setProblemDesc(String problemDesc) {
        this.problemDesc = problemDesc;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public LocalDateTime getCheckTime() {
        return checkTime;
    }

    public void setCheckTime(LocalDateTime checkTime) {
        this.checkTime = checkTime;
    }

    public String getDisposeStatus() {
        return disposeStatus;
    }

    public void setDisposeStatus(String disposeStatus) {
        this.disposeStatus = disposeStatus;
    }
}
