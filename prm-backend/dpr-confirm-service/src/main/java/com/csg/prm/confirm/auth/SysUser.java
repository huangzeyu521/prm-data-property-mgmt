package com.csg.prm.confirm.auth;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 系统用户(内建认证+RBAC)。生产由 4A 统一身份,此表用于本地/演示登录。 */
@TableName("IM_SYS_USER")
public class SysUser {

    @TableId("CEC_USER_ID")
    private String userId;
    @TableField("CEC_USERNAME")
    private String username;
    @TableField("CEC_PASSWORD_HASH")
    private String passwordHash;
    @TableField("CEC_REAL_NAME")
    private String realName;
    @TableField("CEC_ROLE")
    private String role;
    @TableField("CEC_PROVINCE_CODE")
    private String provinceCode;
    @TableField("CEC_STATUS")
    private String status;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getProvinceCode() { return provinceCode; }
    public void setProvinceCode(String provinceCode) { this.provinceCode = provinceCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
