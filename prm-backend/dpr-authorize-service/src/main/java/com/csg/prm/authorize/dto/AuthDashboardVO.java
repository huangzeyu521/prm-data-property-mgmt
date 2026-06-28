package com.csg.prm.authorize.dto;

import java.util.List;
import java.util.Map;

/**
 * 授权看板指标(对应界面 IM-DAM-DPR-04-003-001-001 授权看板)。
 */
public class AuthDashboardVO {

    /** 合规性检查结果分布(红/黄/绿) */
    private Map<String, Long> complianceDist;
    /** 使用场景授权频次 */
    private Map<String, Long> byScenario;
    /** 月度趋势:申请量 + 生效率 */
    private List<TrendPoint> trend;
    /** 授权风险预警 */
    private List<String> riskAlerts;

    public Map<String, Long> getComplianceDist() { return complianceDist; }
    public void setComplianceDist(Map<String, Long> complianceDist) { this.complianceDist = complianceDist; }
    public Map<String, Long> getByScenario() { return byScenario; }
    public void setByScenario(Map<String, Long> byScenario) { this.byScenario = byScenario; }
    public List<TrendPoint> getTrend() { return trend; }
    public void setTrend(List<TrendPoint> trend) { this.trend = trend; }
    public List<String> getRiskAlerts() { return riskAlerts; }
    public void setRiskAlerts(List<String> riskAlerts) { this.riskAlerts = riskAlerts; }

    public static class TrendPoint {
        private final String month;
        private final long applyCount;
        private final double effectiveRate;

        public TrendPoint(String month, long applyCount, double effectiveRate) {
            this.month = month;
            this.applyCount = applyCount;
            this.effectiveRate = effectiveRate;
        }

        public String getMonth() { return month; }
        public long getApplyCount() { return applyCount; }
        public double getEffectiveRate() { return effectiveRate; }
    }

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
    /** 按业务域分布(表5/表6 所属业务域) */
    private Map<String, Long> byBusinessDomain;
    /** 批量授权清单数(表6 一站式产出 IM_BATCH_AUTH_LIST) */
    private long batchListCount;
    /** 跨区域/跨系统域授权数(表5/表6) */
    private long crossRegionCount;
    /** 涉第三方来源授权数(表5,须第三方许可凭证) */
    private long thirdPartyCount;
    /** 涉个人隐私/商业秘密授权数(表5,须信息授权协议) */
    private long sensitiveCount;

    public Map<String, Long> getByBusinessDomain() { return byBusinessDomain; }
    public void setByBusinessDomain(Map<String, Long> byBusinessDomain) { this.byBusinessDomain = byBusinessDomain; }
    public long getBatchListCount() { return batchListCount; }
    public void setBatchListCount(long batchListCount) { this.batchListCount = batchListCount; }
    public long getCrossRegionCount() { return crossRegionCount; }
    public void setCrossRegionCount(long crossRegionCount) { this.crossRegionCount = crossRegionCount; }
    public long getThirdPartyCount() { return thirdPartyCount; }
    public void setThirdPartyCount(long thirdPartyCount) { this.thirdPartyCount = thirdPartyCount; }
    public long getSensitiveCount() { return sensitiveCount; }
    public void setSensitiveCount(long sensitiveCount) { this.sensitiveCount = sensitiveCount; }

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
