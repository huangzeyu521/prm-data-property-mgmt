package com.csg.prm.confirm.dto;

/**
 * 权益卡片概览统计(生成管理页概览条)。按过滤条件聚合,忽略 status,使各状态分布可见。
 * dueSoon=正常且有效期 ≤ 90 天内到期(权益到期续止预警,联动确权变更触发)。
 */
public class EquityCardStats {

    private long total;     // 总卡片
    private long normal;    // 正常
    private long frozen;    // 冻结
    private long expired;   // 失效
    private long dueSoon;   // 即将到期(正常 + 90 天内到期)

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getNormal() {
        return normal;
    }

    public void setNormal(long normal) {
        this.normal = normal;
    }

    public long getFrozen() {
        return frozen;
    }

    public void setFrozen(long frozen) {
        this.frozen = frozen;
    }

    public long getExpired() {
        return expired;
    }

    public void setExpired(long expired) {
        this.expired = expired;
    }

    public long getDueSoon() {
        return dueSoon;
    }

    public void setDueSoon(long dueSoon) {
        this.dueSoon = dueSoon;
    }
}
