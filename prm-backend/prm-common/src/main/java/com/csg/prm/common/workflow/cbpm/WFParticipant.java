package com.csg.prm.common.workflow.cbpm;

import java.io.Serializable;

/**
 * 流程参与者(下一环节处理人),对齐 CBPM/BPS 的 WFParticipant 结构。
 * typeCode:person(个人) / role(角色) / organization(机构) / position(岗位)。
 */
public class WFParticipant implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_PERSON = "person";
    public static final String TYPE_ROLE = "role";
    public static final String TYPE_ORGANIZATION = "organization";
    public static final String TYPE_POSITION = "position";

    /** 参与者 id */
    private String id;
    /** 参与者名称 */
    private String name;
    /** 参与者类型 */
    private String typeCode;

    public WFParticipant() {
    }

    public WFParticipant(String id, String name, String typeCode) {
        this.id = id;
        this.name = name;
        this.typeCode = typeCode;
    }

    /** 个人参与者(最常用)。 */
    public static WFParticipant person(String id, String name) {
        return new WFParticipant(id, name, TYPE_PERSON);
    }

    /** 角色参与者。 */
    public static WFParticipant role(String id, String name) {
        return new WFParticipant(id, name, TYPE_ROLE);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }
}
