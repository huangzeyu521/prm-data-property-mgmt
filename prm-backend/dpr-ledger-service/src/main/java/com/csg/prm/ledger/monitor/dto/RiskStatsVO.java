package com.csg.prm.ledger.monitor.dto;

import java.util.Map;

/**
 * 数据权益风险统计:总数、各处置状态、风险等级分布、处置闭环率。
 */
public class RiskStatsVO {

    private long total;
    private long pending;
    private long handling;
    private long resolved;
    /** 处置闭环率(%) = (已处置+已规避)/ 总数 */
    private double closureRate;
    /** 风险等级分布(低/中/高) */
    private Map<String, Long> levelDistribution;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getHandling() {
        return handling;
    }

    public void setHandling(long handling) {
        this.handling = handling;
    }

    public long getResolved() {
        return resolved;
    }

    public void setResolved(long resolved) {
        this.resolved = resolved;
    }

    public double getClosureRate() {
        return closureRate;
    }

    public void setClosureRate(double closureRate) {
        this.closureRate = closureRate;
    }

    public Map<String, Long> getLevelDistribution() {
        return levelDistribution;
    }

    public void setLevelDistribution(Map<String, Long> levelDistribution) {
        this.levelDistribution = levelDistribution;
    }
}
