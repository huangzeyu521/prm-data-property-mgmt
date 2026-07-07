package com.csg.prm.confirm.dto;

/**
 * P2.2 变更健康度:季度重确权闭环 + 版本链完整性的量化视图(给管控小组"账实相符程度"抓手)。
 */
public class RecheckHealthVO {

    /** 待处置工单数 */
    private long open;
    /** 逾期未处置工单数(待处置且已过处置期限) */
    private long overdue;
    /** 变更申请中 */
    private long changing;
    /** 已复核无变化 */
    private long noChange;
    /** 已完成 */
    private long done;
    /** 90天内到期/已到期的正常权益卡片数(下季度扫描预告) */
    private long dueSoonCards;
    /** 版本链完整率(%):v2+ 权益卡片中带被取代回链(supersededCardNo)的占比;无 v2+ 卡片时=100 */
    private int chainIntegrityRate;
    /** 按期处置率(%):已销号工单中 handleTime<=dueDate 的占比;无已销号工单时=100 */
    private int onTimeRate;

    public long getOpen() {
        return open;
    }

    public void setOpen(long open) {
        this.open = open;
    }

    public long getOverdue() {
        return overdue;
    }

    public void setOverdue(long overdue) {
        this.overdue = overdue;
    }

    public long getChanging() {
        return changing;
    }

    public void setChanging(long changing) {
        this.changing = changing;
    }

    public long getNoChange() {
        return noChange;
    }

    public void setNoChange(long noChange) {
        this.noChange = noChange;
    }

    public long getDone() {
        return done;
    }

    public void setDone(long done) {
        this.done = done;
    }

    public long getDueSoonCards() {
        return dueSoonCards;
    }

    public void setDueSoonCards(long dueSoonCards) {
        this.dueSoonCards = dueSoonCards;
    }

    public int getChainIntegrityRate() {
        return chainIntegrityRate;
    }

    public void setChainIntegrityRate(int chainIntegrityRate) {
        this.chainIntegrityRate = chainIntegrityRate;
    }

    public int getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(int onTimeRate) {
        this.onTimeRate = onTimeRate;
    }
}
