package com.csg.prm.common.org;

import java.util.ArrayList;
import java.util.List;

/** 组织树节点(供前端级联/树选择)。 */
public class OrgNode {

    private String id;
    private String bizOrgId;
    private String name;
    private String code;
    private String shortName;
    private String orgLevel;
    private String sortNo;
    private List<OrgNode> children = new ArrayList<>();

    public static OrgNode of(SysOrganization o) {
        OrgNode n = new OrgNode();
        n.id = o.getId();
        n.bizOrgId = o.getBizOrgId();
        n.name = o.getBizOrgName();
        n.code = o.getBizOrgCode();
        n.shortName = o.getShortName();
        n.orgLevel = o.getOrgLevel();
        n.sortNo = o.getSortNo();
        return n;
    }

    public String getId() { return id; }
    public String getBizOrgId() { return bizOrgId; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getShortName() { return shortName; }
    public String getOrgLevel() { return orgLevel; }
    public String getSortNo() { return sortNo; }
    public List<OrgNode> getChildren() { return children; }
    public void setChildren(List<OrgNode> children) { this.children = children; }
}
