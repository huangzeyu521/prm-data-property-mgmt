package com.csg.prm.confirm.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.auth.SysUser;
import com.csg.prm.confirm.auth.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 系统用户管理(管理员):分页/新增/编辑/删除/重置密码/启停。
 * 复用既有 IM_SYS_USER 表与 SysUserMapper;不返回密码哈希;关键操作落审计日志。
 */
@Service
public class SysUserAdminService {

    /** 重置后默认密码(与演示账号初始密码一致)。 */
    public static final String DEFAULT_PWD = "Prm@1234";
    /** 合法角色集合,与 @RequiresRole / 前端 roles.js 对齐。 */
    private static final List<String> VALID_ROLES = Arrays.asList("apply", "review", "admin", "view", "all");
    private static final String STATUS_ON = "启用";
    private static final String STATUS_OFF = "停用";

    private final SysUserMapper mapper;
    private final SysOpLogService opLog;

    public SysUserAdminService(SysUserMapper mapper, SysOpLogService opLog) {
        this.mapper = mapper;
        this.opLog = opLog;
    }

    public PageResult<SysUser> page(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            w.like(SysUser::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getRealName())) {
            w.like(SysUser::getRealName, query.getRealName());
        }
        if (StringUtils.hasText(query.getRole())) {
            w.eq(SysUser::getRole, query.getRole());
        }
        if (StringUtils.hasText(query.getStatus())) {
            w.eq(SysUser::getStatus, query.getStatus());
        }
        w.orderByAsc(SysUser::getUsername);
        IPage<SysUser> page = mapper.selectPage(query.toPage(), w);
        page.getRecords().forEach(u -> u.setPasswordHash(null)); // 绝不外泄密码哈希
        return PageResult.of(page);
    }

    @Transactional
    public String create(SysUser in) {
        validate(in, true);
        if (existsUsername(in.getUsername(), null)) {
            throw new BizException("登录名已存在:" + in.getUsername());
        }
        SysUser u = new SysUser();
        u.setUserId(UUID.randomUUID().toString().replace("-", ""));
        u.setUsername(in.getUsername().trim());
        u.setRealName(in.getRealName().trim());
        u.setRole(in.getRole());
        u.setProvinceCode(in.getProvinceCode());
        u.setStatus(StringUtils.hasText(in.getStatus()) ? in.getStatus() : STATUS_ON);
        u.setPasswordHash(Sm3Util.hashHex(DEFAULT_PWD));
        mapper.insert(u);
        opLog.record("新增用户", u.getUsername(), "角色=" + u.getRole(), "成功");
        return u.getUserId();
    }

    @Transactional
    public void update(SysUser in) {
        if (!StringUtils.hasText(in.getUserId())) {
            throw new BizException("用户ID不能为空");
        }
        SysUser db = mapper.selectById(in.getUserId());
        if (db == null) {
            throw new BizException("用户不存在");
        }
        validate(in, false);
        // 登录名不可改(避免唯一键漂移与登录态错配);仅改姓名/角色/省份/状态
        db.setRealName(in.getRealName().trim());
        db.setRole(in.getRole());
        db.setProvinceCode(in.getProvinceCode());
        if (StringUtils.hasText(in.getStatus())) {
            db.setStatus(in.getStatus());
        }
        db.setPasswordHash(null); // 不在普通更新里改密码,避免置空
        mapper.updateById(db);
        opLog.record("编辑用户", db.getUsername(), "角色=" + db.getRole(), "成功");
    }

    @Transactional
    public void delete(String userId) {
        SysUser db = mapper.selectById(userId);
        if (db == null) {
            throw new BizException("用户不存在");
        }
        if (userId.equals(currentUserId())) {
            throw new BizException("不能删除当前登录账号");
        }
        mapper.deleteById(userId);
        opLog.record("删除用户", db.getUsername(), null, "成功");
    }

    @Transactional
    public void resetPassword(String userId) {
        SysUser db = mapper.selectById(userId);
        if (db == null) {
            throw new BizException("用户不存在");
        }
        db.setPasswordHash(Sm3Util.hashHex(DEFAULT_PWD));
        mapper.updateById(db);
        opLog.record("重置密码", db.getUsername(), "重置为默认密码", "成功");
    }

    @Transactional
    public String toggleStatus(String userId) {
        SysUser db = mapper.selectById(userId);
        if (db == null) {
            throw new BizException("用户不存在");
        }
        if (userId.equals(currentUserId())) {
            throw new BizException("不能停用当前登录账号");
        }
        String next = STATUS_ON.equals(db.getStatus()) ? STATUS_OFF : STATUS_ON;
        db.setPasswordHash(null);
        db.setStatus(next);
        mapper.updateById(db);
        opLog.record("停用".equals(next) ? "停用用户" : "启用用户", db.getUsername(), null, "成功");
        return next;
    }

    private void validate(SysUser in, boolean isCreate) {
        if (in == null) {
            throw new BizException("参数不能为空");
        }
        if (isCreate && !StringUtils.hasText(in.getUsername())) {
            throw new BizException("登录名不能为空");
        }
        if (!StringUtils.hasText(in.getRealName())) {
            throw new BizException("姓名不能为空");
        }
        if (!StringUtils.hasText(in.getRole()) || !VALID_ROLES.contains(in.getRole())) {
            throw new BizException("角色不合法,应为:" + VALID_ROLES);
        }
    }

    private boolean existsUsername(String username, String excludeId) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim());
        if (excludeId != null) {
            w.ne(SysUser::getUserId, excludeId);
        }
        Long n = mapper.selectCount(w);
        return n != null && n > 0;
    }

    private String currentUserId() {
        return UserContextHolder.get() == null ? null : UserContextHolder.get().getUserId();
    }
}
