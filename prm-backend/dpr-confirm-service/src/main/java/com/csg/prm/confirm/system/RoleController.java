package com.csg.prm.confirm.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.api.R;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.confirm.auth.SysUser;
import com.csg.prm.confirm.auth.SysUserMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统管理 - 角色管理(仅管理员)。
 * 角色码与 @RequiresRole 硬绑定,故角色目录只读;此处给出权限说明 + 每角色用户数统计。
 * 角色分配在用户管理中完成。
 */
@RestController
@RequestMapping("/api/dpr/system/role")
@RequiresRole({"admin"})
public class RoleController {

    /** 固定角色目录:code / name / 权限说明。 */
    private static final String[][] CATALOG = {
            {"apply", "申报人", "发起确权/授权申请,管理本人申请与材料"},
            {"review", "审核 / 审批", "受理并逐级审批确权/授权申请,出具意见"},
            {"admin", "配置管理员", "系统配置、模板、用户与角色管理"},
            {"view", "管理层 · 只读", "综合分析与台账只读查看,不做修改"},
            {"all", "超级管理员", "不受 RBAC 限制的全量权限(内置/演示)"},
    };

    private final SysUserMapper userMapper;

    public RoleController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String[] r : CATALOG) {
            Long count = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, r[0]));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", r[0]);
            m.put("name", r[1]);
            m.put("description", r[2]);
            m.put("userCount", count == null ? 0 : count);
            out.add(m);
        }
        return R.ok(out);
    }
}
