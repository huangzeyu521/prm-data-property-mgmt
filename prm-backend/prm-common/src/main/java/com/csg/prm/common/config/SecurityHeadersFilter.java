package com.csg.prm.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 等保三级安全响应头过滤器:为所有响应注入安全加固头(防点击劫持、MIME 嗅探、强制 HTTPS、最小化引用泄露等)。
 * 依据等级保护 2.0 三级与 Web 安全基线。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /** 安全响应头基线(对外暴露便于自检/前端展示) */
    public static final Map<String, String> SECURITY_HEADERS = buildHeaders();

    private static Map<String, String> buildHeaders() {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        h.put("X-Content-Type-Options", "nosniff");
        h.put("X-Frame-Options", "DENY");
        h.put("X-XSS-Protection", "1; mode=block");
        h.put("Referrer-Policy", "no-referrer");
        h.put("Content-Security-Policy", "default-src 'self'");
        h.put("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        return h;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SECURITY_HEADERS.forEach(response::setHeader);
        chain.doFilter(request, response);
    }
}
