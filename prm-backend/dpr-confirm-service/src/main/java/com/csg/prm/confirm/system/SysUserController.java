package com.csg.prm.confirm.system;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.confirm.auth.SysUser;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/dpr/system/user")
@RequiresRole({"admin"})
public class SysUserController {

    private final SysUserAdminService service;

    public SysUserController(SysUserAdminService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<SysUser>> page(@Valid @RequestBody SysUserQuery query) {
        return Result.success(service.page(query));
    }

    @PostMapping
    public Result<String> create(@Valid @RequestBody SysUser user) {
        return Result.success(service.create(user));
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody SysUser user) {
        service.update(user);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable String userId) {
        service.delete(userId);
        return Result.success();
    }

    @PostMapping("/{userId}/reset-password")
    public Result<String> resetPassword(@PathVariable String userId) {
        service.resetPassword(userId);
        return Result.success(SysUserAdminService.DEFAULT_PWD);
    }

    @PostMapping("/{userId}/toggle-status")
    public Result<String> toggleStatus(@PathVariable String userId) {
        return Result.success(service.toggleStatus(userId));
    }
}
