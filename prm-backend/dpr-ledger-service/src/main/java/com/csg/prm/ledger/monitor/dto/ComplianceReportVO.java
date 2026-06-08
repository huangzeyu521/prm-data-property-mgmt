package com.csg.prm.ledger.monitor.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据资产合规性检查报告(对应可研"生成检查报告")。
 * 汇总一次多维合规检查的命中情况:按维度(有效期/权限范围/申请材料/协议内容)、按结果(警告/不合规)统计。
 */
public class ComplianceReportVO {

    private String reportId;
    private LocalDateTime reportTime;
    private long totalArchives;
    private int hitCount;
    private int warnCount;
    private int failCount;
    /** 各维度命中数 */
    private Map<String, Integer> byDimension;
    /** 在途确权申请数(经聚合网关) */
    private int inflightConfirm;
    /** 在途授权申请数(经聚合网关) */
    private int inflightAuth;
    /** 报告文字摘要 */
    private String summary;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public LocalDateTime getReportTime() {
        return reportTime;
    }

    public void setReportTime(LocalDateTime reportTime) {
        this.reportTime = reportTime;
    }

    public long getTotalArchives() {
        return totalArchives;
    }

    public void setTotalArchives(long totalArchives) {
        this.totalArchives = totalArchives;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public int getWarnCount() {
        return warnCount;
    }

    public void setWarnCount(int warnCount) {
        this.warnCount = warnCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public Map<String, Integer> getByDimension() {
        return byDimension;
    }

    public void setByDimension(Map<String, Integer> byDimension) {
        this.byDimension = byDimension;
    }

    public int getInflightConfirm() {
        return inflightConfirm;
    }

    public void setInflightConfirm(int inflightConfirm) {
        this.inflightConfirm = inflightConfirm;
    }

    public int getInflightAuth() {
        return inflightAuth;
    }

    public void setInflightAuth(int inflightAuth) {
        this.inflightAuth = inflightAuth;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
