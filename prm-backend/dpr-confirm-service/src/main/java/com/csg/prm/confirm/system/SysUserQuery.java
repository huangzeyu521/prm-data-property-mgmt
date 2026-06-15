package com.csg.prm.confirm.system;

import com.csg.prm.common.query.PageQuery;

/** 用户查询条件:按登录名/姓名/角色/状态过滤。 */
public class SysUserQuery extends PageQuery {

    private String username;
    private String realName;
    private String role;
    private String status;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
