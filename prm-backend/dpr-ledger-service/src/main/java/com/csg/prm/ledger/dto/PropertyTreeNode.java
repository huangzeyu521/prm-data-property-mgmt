package com.csg.prm.ledger.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 产权树节点("子公司—系统—模式—数据集"立体层级)。
 */
public class PropertyTreeNode {

    public static final String TYPE_SUBSIDIARY = "SUBSIDIARY";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_SCHEMA = "SCHEMA";
    public static final String TYPE_DATASET = "DATASET";

    private String id;
    private String label;
    private String type;
    /** 数据集节点:关联资产ID */
    private String assetId;
    /** 数据集节点:确权状态(未确权/申请中/已确权) */
    private String confirmStatus;
    private List<PropertyTreeNode> children = new ArrayList<>();

    public PropertyTreeNode() {
    }

    public PropertyTreeNode(String id, String label, String type) {
        this.id = id;
        this.label = label;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getConfirmStatus() {
        return confirmStatus;
    }

    public void setConfirmStatus(String confirmStatus) {
        this.confirmStatus = confirmStatus;
    }

    public List<PropertyTreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<PropertyTreeNode> children) {
        this.children = children;
    }
}
