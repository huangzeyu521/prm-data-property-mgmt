package com.csg.prm.confirm.dto;

import java.util.List;

/**
 * 确权申请材料校验报告(可研 3.2.2.1.1.2.1.5):
 * 基于预设规则的完整性(应交比对)+合规性(原件/格式)校验结果。
 */
public class MaterialCheckReport {

    private String applyId;
    private int requiredCount;     // 应交项数
    private int uploadedCount;     // 已交材料数
    private int passCount;         // 通过(完整+有原件)
    private int failCount;         // 不合规数
    private List<String> missing;       // 缺失项(应交未交)
    private List<String> nonCompliant;  // 不合规项(已交无真实原件等)
    private boolean allPass;       // 完整且全部合规
    private String summary;

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(int requiredCount) {
        this.requiredCount = requiredCount;
    }

    public int getUploadedCount() {
        return uploadedCount;
    }

    public void setUploadedCount(int uploadedCount) {
        this.uploadedCount = uploadedCount;
    }

    public int getPassCount() {
        return passCount;
    }

    public void setPassCount(int passCount) {
        this.passCount = passCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public List<String> getMissing() {
        return missing;
    }

    public void setMissing(List<String> missing) {
        this.missing = missing;
    }

    public List<String> getNonCompliant() {
        return nonCompliant;
    }

    public void setNonCompliant(List<String> nonCompliant) {
        this.nonCompliant = nonCompliant;
    }

    public boolean isAllPass() {
        return allPass;
    }

    public void setAllPass(boolean allPass) {
        this.allPass = allPass;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
