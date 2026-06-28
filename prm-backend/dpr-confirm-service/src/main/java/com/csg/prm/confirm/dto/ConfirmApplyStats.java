package com.csg.prm.confirm.dto;

/**
 * 确权申请概览统计(查询页顶部概览条)。
 * <p>按当前过滤条件(资产名/权属人/时间/登记类型/变更触发)聚合,但<b>忽略 status 过滤</b>——
 * 让各状态分布(在途/已完成/已驳回)真实可见,供概览卡片点选快速下钻。
 */
public class ConfirmApplyStats {

    private long total;          // 总申请
    private long draft;          // 草稿
    private long inReview;       // 在途(人工预审/合规/主管/终审)
    private long done;           // 已完成
    private long rejected;       // 已驳回
    private long withdrawn;      // 已撤回
    private long initialCount;   // 登记类型=初始确权
    private long changeCount;    // 登记类型=确权变更

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getDraft() {
        return draft;
    }

    public void setDraft(long draft) {
        this.draft = draft;
    }

    public long getInReview() {
        return inReview;
    }

    public void setInReview(long inReview) {
        this.inReview = inReview;
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

    public long getWithdrawn() {
        return withdrawn;
    }

    public void setWithdrawn(long withdrawn) {
        this.withdrawn = withdrawn;
    }

    public long getInitialCount() {
        return initialCount;
    }

    public void setInitialCount(long initialCount) {
        this.initialCount = initialCount;
    }

    public long getChangeCount() {
        return changeCount;
    }

    public void setChangeCount(long changeCount) {
        this.changeCount = changeCount;
    }
}
