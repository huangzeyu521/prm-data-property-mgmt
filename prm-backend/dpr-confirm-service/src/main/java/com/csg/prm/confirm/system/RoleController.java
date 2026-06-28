package com.csg.prm.confirm.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.confirm.auth.SysUser;
import com.csg.prm.confirm.auth.SysUserMapper;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/dpr/system/role")
@RequiresRole({"admin"})
public class RoleController {

    /** 固定角色目录:code / name / 权限说明。 */
    private static final String[][] CATALOG = {
            {"apply", "申报人 · 数字化部门团队", "数字化部门团队负责人/成员;发起确权/授权申请,收集编制归集资料(确权节点20/40)"},
            {"business", "业务管理部门团队", "业务管理部门团队负责人/成员;确权配合提供资料(节点30)、授权业务审核"},
            {"precheck", "归集预审 · 数字化部门团队", "数字化部门团队;归集审查资料完整性、复核 AI 校验结果(确权节点40)"},
            {"review", "合规管控小组", "数据产权合规管控小组;合规审核(确权节点50)、权益风险处置、授权合规审核"},
            {"manager", "数字化部主管", "公司总部数字化管理部门主管;确权审核(节点60)、制卡归集(节点80)、授权主管审核"},
            {"director", "经理 / 高级经理", "经理/高级经理;确权终审审批(节点70)、授权经理审核"},
            {"gm", "副总经理 / 总经理", "副总经理/总经理;授权副总审批(终审)"},
            {"leadership", "领导小组办公室", "网络安全和数字南网建设领导小组办公室;批量授权末节点决策"},
            {"admin", "配置管理员", "系统配置、模板、用户与角色管理"},
            {"view", "管理层 · 只读", "分管领导/领导;综合分析与台账只读查看,不做修改"},
            {"all", "超级管理员", "不受 RBAC 限制的全量权限(内置/演示)"},
    };

    private final SysUserMapper userMapper;

    public RoleController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
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
        return Result.success(out);
    }
}
