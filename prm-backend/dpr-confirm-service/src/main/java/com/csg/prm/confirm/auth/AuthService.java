package com.csg.prm.confirm.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.auth.JwtUtil;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.system.SysOpLogService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 内建认证:登录(SM3 密码校验)+ 签发 JWT;启动时幂等种入每角色演示账号。
 * 播种运行器置最高优先级,先于慢启动的 AI 语料/模板运行器执行,缩小"已可接流量但演示账号未种入"的登录窗口期。
 * dev profile 另由 data.sql 在 datasource 初始化期(早于 Web 接流量)确定性种入,此处幂等共存(已存在即跳过)。
 */
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthService implements ApplicationRunner {

    /** 演示账号默认密码(生产由 4A 接管,此处仅本地/演示) */
    private static final String DEFAULT_PWD = "Prm@1234";
    private static final long TTL = 8 * 3600; // 8 小时

    private final SysUserMapper mapper;
    private final SysOpLogService opLog;

    public AuthService(SysUserMapper mapper, SysOpLogService opLog) {
        this.mapper = mapper;
        this.opLog = opLog;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("apply", "梁晶晶", "apply", "GD");
        seed("precheck", "周慎之", "precheck", "GD");
        seed("review", "李天天", "review", "GD");
        seed("admin", "陈明亮", "admin", "");
        seed("viewer", "黄文静", "view", "");
        seed("super", "吴海涛", "all", "");
    }

    private void seed(String username, String realName, String role, String prov) {
        Long n = mapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (n != null && n > 0) return;
        SysUser u = new SysUser();
        u.setUserId(UUID.randomUUID().toString().replace("-", ""));
        u.setUsername(username);
        u.setPasswordHash(Sm3Util.hashHex(DEFAULT_PWD));
        u.setRealName(realName);
        u.setRole(role);
        u.setProvinceCode(prov);
        u.setStatus("启用");
        mapper.insert(u);
    }

    /** 登录:校验用户名+SM3密码,返回 token + 用户信息 */
    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException("用户名与密码不能为空");
        }
        SysUser u = mapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("LIMIT 1"));
        if (u == null || !Sm3Util.hashHex(password).equals(u.getPasswordHash())) {
            opLog.record(u == null ? null : u.getUserId(), u == null ? username : u.getRealName(),
                    "登录", username, "用户名或密码错误", "失败", null);
            throw new BizException("用户名或密码错误");
        }
        if (u.getStatus() != null && !"启用".equals(u.getStatus())) {
            opLog.record(u.getUserId(), u.getRealName(), "登录", username, "账号已停用", "失败", null);
            throw new BizException("账号已停用");
        }
        String token = JwtUtil.issue(u.getUserId(), u.getRealName(), u.getRole(), u.getProvinceCode(), TTL);
        opLog.record(u.getUserId(), u.getRealName(), "登录", username, "登录成功", "成功", null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("user", info(u));
        return out;
    }

    public Map<String, Object> info(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", u.getUserId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("role", u.getRole());
        m.put("provinceCode", u.getProvinceCode());
        return m;
    }

    public SysUser byId(String userId) {
        return mapper.selectById(userId);
    }
}
