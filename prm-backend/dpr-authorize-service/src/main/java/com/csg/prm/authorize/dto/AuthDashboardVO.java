package com.csg.prm.authorize.dto;

import java.util.Map;

/**
 * 授权看板指标(对应界面 IM-DAM-DPR-04-003-001-001 授权看板)。
 */
public class AuthDashboardVO {

    /** 授权申请总量 */
    private long totalApply;
    /** 已生效数量 */
    private long effective;
    /** 已驳回数量 */
    private long rejected;
    /** 审核中数量 */
    private long inReview;
    /** 已签发授权证书数量 */
    private long certCount;
    /** 监测联动熔断暂停的证书数量(附录F 3.4.5) */
    private long suspendedCount;
    /** 授权生效率(%) */
    private double effectiveRate;
    /** 授权模式分布(一事一议/批量) */
    private Map<String, Long> modeDistribution;
    /** 授权权益类型分布 */
    private Map<String, Long> rightTypeDistribution;

    public long getTotalApply() {
        return totalApply;
    }

    public void setTotalApply(long totalApply) {
        this.totalApply = totalApply;
    }

    public long getEffective() {
        return effective;
    }

    public void setEffective(long effective) {
        this.effective = effective;
    }

    public long getRejected() {
        return rejected;
    }

    public void setRejected(long rejected) {
        this.rejected = rejected;
    }

    public long getInReview() {
        return inReview;
    }

    public void setInReview(long inReview) {
        this.inReview = inReview;
    }

    public long getCertCount() {
        return certCount;
    }

    public void setCertCount(long certCount) {
        this.certCount = certCount;
    }

    public long getSuspendedCount() {
        return suspendedCount;
    }

    public void setSuspendedCount(long suspendedCount) {
        this.suspendedCount = suspendedCount;
    }

    public double getEffectiveRate() {
        return effectiveRate;
    }

    public void setEffectiveRate(double effectiveRate) {
        this.effectiveRate = effectiveRate;
    }

    public Map<String, Long> getModeDistribution() {
        return modeDistribution;
    }

    public void setModeDistribution(Map<String, Long> modeDistribution) {
        this.modeDistribution = modeDistribution;
    }

    public Map<String, Long> getRightTypeDistribution() {
        return rightTypeDistribution;
    }

    public void setRightTypeDistribution(Map<String, Long> rightTypeDistribution) {
        this.rightTypeDistribution = rightTypeDistribution;
    }
}
