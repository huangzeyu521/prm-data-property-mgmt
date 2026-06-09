package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 授权申请合规校验结果(对应界面 IM-DAM-DPR-03-001-001-006)。
 * 红/黄/绿 风险等级。对应物理表 IM_AUTH_COMPLIANCE。
 */
@TableName("IM_AUTH_COMPLIANCE")
public class AuthCompliance extends BaseEntity {

    public static final String LEVEL_GREEN = "绿";
    public static final String LEVEL_YELLOW = "黄";
    public static final String LEVEL_RED = "红";

    @TableId(value = "CEC_CHECK_ID", type = IdType.ASSIGN_UUID)
    private String checkId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 风险等级:绿/黄/红 */
    @TableField("CEC_RISK_LEVEL")
    private String riskLevel;

    @TableField("CEC_CHECK_RESULT")
    private String checkResult;

    @TableField("CEC_PROBLEM_DESC")
    private String problemDesc;

    /** 多维校验报告 JSON:[{dimension,item,pass,message}] */
    @TableField("CEC_CHECK_REPORT")
    private String checkReport;

    @TableField("CEC_CHECK_TIME")
    private LocalDateTime checkTime;

    public String getCheckReport() { return checkReport; }
    public void setCheckReport(String checkReport) { this.checkReport = checkReport; }
    public String getCheckId() { return checkId; }
    public void setCheckId(String checkId) { this.checkId = checkId; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getCheckResult() { return checkResult; }
    public void setCheckResult(String checkResult) { this.checkResult = checkResult; }
    public String getProblemDesc() { return problemDesc; }
    public void setProblemDesc(String problemDesc) { this.problemDesc = problemDesc; }
    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
}
