package com.csg.prm.ledger.dto;

import java.util.Map;

/**
 * 产权总体概览指标(对应界面 IM-DAM-DPR-01-001-001-001 产权总体概览)。
 */
public class LedgerOverviewVO {

    /** 资产总数 */
    private long totalAssets;
    /** 已确权资产数 */
    private long confirmedAssets;
    /** 未确权资产数 */
    private long unconfirmedAssets;
    /** 确权覆盖率(%) */
    private double confirmRate;
    /** 产权类型构成 */
    private Map<String, Long> rightTypeDistribution;
    /** 组织(子公司)资产分布 */
    private Map<String, Long> subsidiaryDistribution;

    public long getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(long totalAssets) {
        this.totalAssets = totalAssets;
    }

    public long getConfirmedAssets() {
        return confirmedAssets;
    }

    public void setConfirmedAssets(long confirmedAssets) {
        this.confirmedAssets = confirmedAssets;
    }

    public long getUnconfirmedAssets() {
        return unconfirmedAssets;
    }

    public void setUnconfirmedAssets(long unconfirmedAssets) {
        this.unconfirmedAssets = unconfirmedAssets;
    }

    public double getConfirmRate() {
        return confirmRate;
    }

    public void setConfirmRate(double confirmRate) {
        this.confirmRate = confirmRate;
    }

    public Map<String, Long> getRightTypeDistribution() {
        return rightTypeDistribution;
    }

    public void setRightTypeDistribution(Map<String, Long> rightTypeDistribution) {
        this.rightTypeDistribution = rightTypeDistribution;
    }

    public Map<String, Long> getSubsidiaryDistribution() {
        return subsidiaryDistribution;
    }

    public void setSubsidiaryDistribution(Map<String, Long> subsidiaryDistribution) {
        this.subsidiaryDistribution = subsidiaryDistribution;
    }
}
