package com.csg.prm.common.context;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * 当前登录用户上下文(由 4A/网关解析后注入)。用于 RBAC + ABAC 数据范围控制(数据不出域)。
 */
public class UserContext implements Serializable {

    /** 4A 账号 */
    private String userId;
    private String userName;
    /** 省公司代码,用于 ABAC 行级数据隔离 */
    private String provinceCode;
    /** 单位/局代码 */
    private String bureauCode;
    /** 角色集合 */
    private Set<String> roles;

    public static UserContext system() {
        UserContext ctx = new UserContext();
        ctx.userId = "system";
        ctx.userName = "system";
        ctx.roles = Collections.emptySet();
        return ctx;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getBureauCode() {
        return bureauCode;
    }

    public void setBureauCode(String bureauCode) {
        this.bureauCode = bureauCode;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}
