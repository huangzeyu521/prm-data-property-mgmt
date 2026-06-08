package com.csg.prm.ledger.aggregate;

import java.io.Serializable;

/**
 * 跨域记录(中性 DTO):统一表示来自确权域/授权域/风险域的一条业务记录,
 * 供资产360贯通视图与统一待办中心聚合展示,避免台账服务耦合各域的实体类型。
 */
public class DomainRecord implements Serializable {

    /** 业务域:确权 / 授权 / 风险 */
    private String domain;
    /** 业务单据ID(确权申请ID / 授权申请ID / 风险ID) */
    private String id;
    /** 业务单号 */
    private String no;
    private String assetId;
    private String assetName;
    /** 产权类型 */
    private String rightType;
    /** 关联方:确权=权属主体,授权=被授权方,风险=风险类型 */
    private String party;
    /** 当前状态(中文流程态) */
    private String status;
    /** 当前节点编号(可空) */
    private Integer node;
    /** 发生/创建时间(ISO 字符串) */
    private String time;
    /** 前端可跳转路由(便于待办一键直达办理页) */
    private String link;

    public DomainRecord() {
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNo() {
        return no;
    }

    public void setNo(String no) {
        this.no = no;
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

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getParty() {
        return party;
    }

    public void setParty(String party) {
        this.party = party;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getNode() {
        return node;
    }

    public void setNode(Integer node) {
        this.node = node;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
