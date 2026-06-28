package com.csg.prm.confirm.auth;

import com.csg.prm.common.api.Result;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 内建认证接口:登录签发 JWT、查当前用户。生产由 4A 接管登录,此处用于本地/演示。 */
@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 登录:body {username, password} → {token, user} */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return Result.success(authService.login(body.get("username"), body.get("password")));
    }

    /** 当前登录用户(由拦截器从 JWT/头解析的上下文) */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || ctx.getUserId() == null || "system".equals(ctx.getUserId())) {
            throw new BusinessException(401, "未登录");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", ctx.getUserId());
        m.put("realName", ctx.getUserName());
        m.put("role", ctx.getRoles() == null || ctx.getRoles().isEmpty() ? "" : ctx.getRoles().iterator().next());
        m.put("provinceCode", ctx.getProvinceCode());
        return Result.success(m);
    }
}
