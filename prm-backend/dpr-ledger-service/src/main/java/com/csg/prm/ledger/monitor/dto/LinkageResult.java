package com.csg.prm.ledger.monitor.dto;

/**
 * 监测联动结果:本次违规上报生成的预警、是否触发熔断、被暂停证书数。
 */
public class LinkageResult {

    private String alertId;
    private boolean circuitBroken;
    private int suspendedCount;
    /** 联动派生的重确权工单ID(重确权联动场景) */
    private String reConfirmId;

    public LinkageResult() {
    }

    public LinkageResult(String alertId, boolean circuitBroken, int suspendedCount) {
        this.alertId = alertId;
        this.circuitBroken = circuitBroken;
        this.suspendedCount = suspendedCount;
    }

    public String getReConfirmId() {
        return reConfirmId;
    }

    public void setReConfirmId(String reConfirmId) {
        this.reConfirmId = reConfirmId;
    }

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public boolean isCircuitBroken() {
        return circuitBroken;
    }

    public void setCircuitBroken(boolean circuitBroken) {
        this.circuitBroken = circuitBroken;
    }

    public int getSuspendedCount() {
        return suspendedCount;
    }

    public void setSuspendedCount(int suspendedCount) {
        this.suspendedCount = suspendedCount;
    }
}
