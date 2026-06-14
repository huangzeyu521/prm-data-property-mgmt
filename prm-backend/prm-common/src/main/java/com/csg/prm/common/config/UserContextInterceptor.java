package com.csg.prm.common.config;

import com.csg.prm.common.auth.JwtUtil;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 共享用户上下文拦截器:优先认 Authorization: Bearer JWT(内建登录),无则退回 X-User-* 头(4A/网关注入)。
 * 正式环境由统一网关完成 4A OAuth2 校验后透传 X-User-* 头;本地/演示用内建登录签发的 JWT。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1) 优先 JWT(内建登录)
        Map<String, String> claims = JwtUtil.verify(request.getHeader("Authorization"));
        if (claims != null && StringUtils.hasText(claims.get("sub"))) {
            UserContext ctx = new UserContext();
            ctx.setUserId(claims.get("sub"));
            ctx.setUserName(claims.get("name"));
            ctx.setProvinceCode(claims.get("prov"));
            String role = claims.get("role");
            ctx.setRoles(StringUtils.hasText(role) ? Collections.singleton(role) : Collections.emptySet());
            UserContextHolder.set(ctx);
            return true;
        }
        // 2) 退回 X-User-* 头(4A 网关注入 / 测试)
        String userId = request.getHeader("X-User-Id");
        if (StringUtils.hasText(userId)) {
            UserContext ctx = new UserContext();
            ctx.setUserId(userId);
            ctx.setUserName(header(request, "X-User-Name"));
            ctx.setProvinceCode(header(request, "X-Province-Code"));
            ctx.setBureauCode(header(request, "X-Bureau-Code"));
            String roles = request.getHeader("X-User-Roles");
            if (StringUtils.hasText(roles)) {
                ctx.setRoles(Arrays.stream(roles.split(",")).map(String::trim).collect(Collectors.toSet()));
            }
            UserContextHolder.set(ctx);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private String header(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        return StringUtils.hasText(v) ? v : null;
    }
}
