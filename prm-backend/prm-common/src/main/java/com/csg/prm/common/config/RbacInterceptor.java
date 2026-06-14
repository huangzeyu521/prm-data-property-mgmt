package com.csg.prm.common.config;

import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * RBAC 强制拦截器:对标注 @RequiresRole 的端点校验当前用户角色;角色 all 始终放行。
 * 由 prm.auth.enabled 开关控制(关闭时不拦截,兼容既有无 token 测试)。在用户上下文拦截器之后运行。
 */
public class RbacInterceptor implements HandlerInterceptor {

    private final boolean enabled;

    public RbacInterceptor(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || !(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod hm = (HandlerMethod) handler;
        RequiresRole rr = hm.getMethodAnnotation(RequiresRole.class);
        if (rr == null) {
            rr = hm.getBeanType().getAnnotation(RequiresRole.class);
        }
        if (rr == null || rr.value().length == 0) {
            return true; // 未标注 = 开放(仅需通过认证层,本拦截器不强制)
        }
        UserContext ctx = UserContextHolder.get();
        Set<String> roles = ctx == null ? null : ctx.getRoles();
        if (roles == null || roles.isEmpty()) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "未登录或登录失效,无法访问受控操作");
        }
        if (roles.contains("all")) {
            return true;
        }
        for (String need : rr.value()) {
            if (roles.contains(need)) {
                return true;
            }
        }
        throw new BizException(ResultCode.FORBIDDEN.getCode(),
                "无权限:该操作需要角色 " + String.join("/", rr.value()));
    }
}
