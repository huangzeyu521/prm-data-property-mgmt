package com.csg.prm.confirm;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.auth.AuthService;
import com.csg.prm.confirm.auth.SysUser;
import com.csg.prm.confirm.system.SysOpLog;
import com.csg.prm.confirm.system.SysOpLogQuery;
import com.csg.prm.confirm.system.SysOpLogService;
import com.csg.prm.confirm.system.SysUserAdminService;
import com.csg.prm.confirm.system.SysUserQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 系统管理(用户/角色/操作日志)能力测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class SystemMgmtTest {

    @Autowired private SysUserAdminService userService;
    @Autowired private SysOpLogService opLogService;
    @Autowired private AuthService authService;

    @Test
    void create_then_page_hides_password() {
        SysUser u = new SysUser();
        u.setUsername("zhangwei");
        u.setRealName("张伟");
        u.setRole("apply");
        u.setProvinceCode("GD");
        String id = userService.create(u);
        assertNotNull(id);

        SysUserQuery q = new SysUserQuery();
        q.setUsername("zhangwei");
        PageResult<SysUser> page = userService.page(q);
        assertTrue(page.getTotal() >= 1, "应能分页查到新建用户");
        SysUser got = page.getRecords().get(0);
        assertEquals("张伟", got.getRealName());
        assertNull(got.getPasswordHash(), "分页结果不得外泄密码哈希");
    }

    @Test
    void duplicate_username_rejected() {
        SysUser u = new SysUser();
        u.setUsername("dupuser");
        u.setRealName("重复甲");
        u.setRole("view");
        userService.create(u);

        SysUser u2 = new SysUser();
        u2.setUsername("dupuser");
        u2.setRealName("重复乙");
        u2.setRole("view");
        assertThrows(BizException.class, () -> userService.create(u2), "重复登录名应被拒绝");
    }

    @Test
    void invalid_role_rejected() {
        SysUser u = new SysUser();
        u.setUsername("badrole");
        u.setRealName("非法角色");
        u.setRole("superman");
        assertThrows(BizException.class, () -> userService.create(u), "非法角色应被拒绝");
    }

    @Test
    void reset_password_and_login_with_default() {
        SysUser u = new SysUser();
        u.setUsername("resetme");
        u.setRealName("待重置");
        u.setRole("apply");
        String id = userService.create(u);

        userService.resetPassword(id);
        Map<String, Object> r = authService.login("resetme", SysUserAdminService.DEFAULT_PWD);
        assertNotNull(r.get("token"), "重置后应能用默认密码登录");
    }

    @Test
    void toggle_status_blocks_login_when_disabled() {
        SysUser u = new SysUser();
        u.setUsername("toggleme");
        u.setRealName("启停测试");
        u.setRole("apply");
        String id = userService.create(u);

        String next = userService.toggleStatus(id);
        assertEquals("停用", next);
        assertThrows(BizException.class,
                () -> authService.login("toggleme", SysUserAdminService.DEFAULT_PWD),
                "停用后不应能登录");

        assertEquals("启用", userService.toggleStatus(id), "再次切换应恢复启用");
    }

    @Test
    void delete_user() {
        SysUser u = new SysUser();
        u.setUsername("deleteme");
        u.setRealName("待删除");
        u.setRole("view");
        String id = userService.create(u);

        userService.delete(id);
        SysUserQuery q = new SysUserQuery();
        q.setUsername("deleteme");
        assertEquals(0, userService.page(q).getTotal(), "删除后不应再查到");
    }

    @Test
    void login_writes_audit_log() {
        // 演示账号 admin/Prm@1234 由启动种入
        authService.login("admin", "Prm@1234");
        SysOpLogQuery q = new SysOpLogQuery();
        q.setAction("登录");
        q.setResult("成功");
        PageResult<SysOpLog> page = opLogService.page(q);
        assertTrue(page.getTotal() >= 1, "成功登录应留下审计日志");
    }

    @Test
    void admin_operation_writes_audit_log() {
        SysUser u = new SysUser();
        u.setUsername("audited");
        u.setRealName("审计对象");
        u.setRole("apply");
        userService.create(u);

        SysOpLogQuery q = new SysOpLogQuery();
        q.setAction("新增用户");
        assertTrue(opLogService.page(q).getTotal() >= 1, "新增用户应留下审计日志");
    }
}
