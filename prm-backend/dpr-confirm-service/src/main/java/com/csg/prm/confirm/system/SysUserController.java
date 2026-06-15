package com.csg.prm.confirm.system;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.confirm.auth.SysUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统管理 - 用户管理接口(仅管理员)。
 * 复用 IM_SYS_USER;密码不出库,重置回默认密码。
 */
@RestController
@RequestMapping("/api/dpr/system/user")
@RequiresRole({"admin"})
public class SysUserController {

    private final SysUserAdminService service;

    public SysUserController(SysUserAdminService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public R<PageResult<SysUser>> page(@RequestBody SysUserQuery query) {
        return R.ok(service.page(query));
    }

    @PostMapping
    public R<String> create(@RequestBody SysUser user) {
        return R.ok(service.create(user));
    }

    @PutMapping
    public R<Void> update(@RequestBody SysUser user) {
        service.update(user);
        return R.ok();
    }

    @DeleteMapping("/{userId}")
    public R<Void> delete(@PathVariable String userId) {
        service.delete(userId);
        return R.ok();
    }

    @PostMapping("/{userId}/reset-password")
    public R<String> resetPassword(@PathVariable String userId) {
        service.resetPassword(userId);
        return R.ok(SysUserAdminService.DEFAULT_PWD);
    }

    @PostMapping("/{userId}/toggle-status")
    public R<String> toggleStatus(@PathVariable String userId) {
        return R.ok(service.toggleStatus(userId));
    }
}
