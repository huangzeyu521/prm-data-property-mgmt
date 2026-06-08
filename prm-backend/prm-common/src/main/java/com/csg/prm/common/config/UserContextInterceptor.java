package com.csg.prm.common.config;

import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 共享用户上下文拦截器(4A 集成占位):从网关下发的请求头解析用户身份与数据范围,放入 ThreadLocal。
 * 正式环境由统一网关完成 4A OAuth2 校验后透传 X-User-* 头。
 */
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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
