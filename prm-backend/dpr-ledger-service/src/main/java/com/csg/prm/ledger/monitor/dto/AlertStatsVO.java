package com.csg.prm.ledger.monitor.dto;

import java.util.Map;

/**
 * 风险预警统计(权益状态监控大盘 + KPI:整改闭环率)。
 */
public class AlertStatsVO {

    private long total;
    private long pending;
    private long processing;
    private long closed;
    /** 整改闭环率(%) */
    private double closureRate;
    /** 按预警级别分布 */
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

    public long getProcessing() {
        return processing;
    }

    public void setProcessing(long processing) {
        this.processing = processing;
    }

    public long getClosed() {
        return closed;
    }

    public void setClosed(long closed) {
        this.closed = closed;
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
