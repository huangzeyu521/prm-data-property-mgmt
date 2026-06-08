package com.csg.prm.common.writeback;

/**
 * 产权事件(确权/授权关键节点 -> 回写台账)。
 * 由确权制卡、授权生效等节点产生,经 {@link LedgerWritebackGateway} 回写产权档案并留变更记录,
 * 使台账(一资产一档/概览/KPI)由真实业务流实时驱动。
 */
public class RightsEvent {

    public static final String TYPE_CONFIRMED = "确权制卡";
    public static final String TYPE_AUTHORIZED = "授权生效";

    private String assetId;
    private String assetName;
    private String eventType;
    /** 确权状态(确权事件):已确权 */
    private String confirmStatus;
    /** 授权状态(授权事件):已授权 */
    private String authStatus;
    private String rightType;
    /** 权利主体/被授权方 */
    private String rightHolder;
    private String equityCardId;
    /** 来源单据(确权申请ID/授权申请ID) */
    private String sourceTicket;
    private String reason;

    public RightsEvent() {
    }

    public static RightsEvent confirmed(String assetId, String assetName, String rightType,
                                        String rightHolder, String equityCardId, String sourceTicket) {
        RightsEvent e = new RightsEvent();
        e.eventType = TYPE_CONFIRMED;
        e.assetId = assetId;
        e.assetName = assetName;
        e.rightType = rightType;
        e.rightHolder = rightHolder;
        e.equityCardId = equityCardId;
        e.confirmStatus = "已确权";
        e.sourceTicket = sourceTicket;
        e.reason = "确权终审通过制卡,回写确权状态";
        return e;
    }

    public static RightsEvent authorized(String assetId, String rightType, String granteeOrg, String sourceTicket) {
        RightsEvent e = new RightsEvent();
        e.eventType = TYPE_AUTHORIZED;
        e.assetId = assetId;
        e.rightType = rightType;
        e.rightHolder = granteeOrg;
        e.authStatus = "已授权";
        e.sourceTicket = sourceTicket;
        e.reason = "授权生效发证,回写授权状态";
        return e;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus) {
        this.confirmStatus = confirmStatus;
    }

    public String getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(String authStatus) {
        this.authStatus = authStatus;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getRightHolder() {
        return rightHolder;
    }

    public void setRightHolder(String rightHolder) {
        this.rightHolder = rightHolder;
    }

    public String getEquityCardId() {
        return equityCardId;
    }

    public void setEquityCardId(String equityCardId) {
        this.equityCardId = equityCardId;
    }

    public String getSourceTicket() {
        return sourceTicket;
    }

    public void setSourceTicket(String sourceTicket) {
        this.sourceTicket = sourceTicket;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
