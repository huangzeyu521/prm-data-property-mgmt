package com.csg.prm.confirm.dto;

import java.util.Map;

/**
 * 确权看板指标(对应界面 IM-DAM-DPR-04-002-001-001 确权看板)。
 */
public class ConfirmDashboardVO {

    /** 确权申请总量 */
    private long totalApply;
    /** 待处理数量(流转中) */
    private long pending;
    /** 已完成确权数量 */
    private long done;
    /** 已驳回数量 */
    private long rejected;
    /** 确权通过率(%) = 已完成 /(已完成+已驳回) */
    private double passRate;
    /** 已生成权益卡片数量 */
    private long cardCount;
    /** 重确权工单数量(监测联动派生,附录F 3.3.2) */
    private long reConfirmCount;
    /** 各状态分布 */
    private Map<String, Long> statusDistribution;
    /** 权益类型分布(基于权益卡片) */
    private Map<String, Long> rightTypeDistribution;

    public long getTotalApply() {
        return totalApply;
    }

    public void setTotalApply(long totalApply) {
        this.totalApply = totalApply;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getDone() {
        return done;
    }

    public void setDone(long done) {
        this.done = done;
    }

    public long getRejected() {
        return rejected;
    }

    public void setRejected(long rejected) {
        this.rejected = rejected;
    }

    public double getPassRate() {
        return passRate;
    }

    public void setPassRate(double passRate) {
        this.passRate = passRate;
    }

    public long getCardCount() {
        return cardCount;
    }

    public void setCardCount(long cardCount) {
        this.cardCount = cardCount;
    }

    public long getReConfirmCount() {
        return reConfirmCount;
    }

    public void setReConfirmCount(long reConfirmCount) {
        this.reConfirmCount = reConfirmCount;
    }

    public Map<String, Long> getStatusDistribution() {
        return statusDistribution;
    }

    public void setStatusDistribution(Map<String, Long> statusDistribution) {
        this.statusDistribution = statusDistribution;
    }

    public Map<String, Long> getRightTypeDistribution() {
        return rightTypeDistribution;
    }

    public void setRightTypeDistribution(Map<String, Long> rightTypeDistribution) {
        this.rightTypeDistribution = rightTypeDistribution;
    }
}
