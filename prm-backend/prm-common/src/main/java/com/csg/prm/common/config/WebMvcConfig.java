package com.csg.prm.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 共享 Web MVC 配置:为所有 /api/** 注册用户上下文拦截器 + RBAC 强制拦截器，并配置Jackson日期时间格式化。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /** RBAC 强制开关:关闭时不拦截(兼容既有无 token 测试);生产置 true 或由 4A 网关把关 */
    @Value("${prm.auth.enabled:false}")
    private boolean authEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 顺序:先解析用户上下文(JWT/头),再做 RBAC 强制
        registry.addInterceptor(new UserContextInterceptor()).addPathPatterns("/api/**");
        registry.addInterceptor(new RbacInterceptor(authEnabled)).addPathPatterns("/api/**");
    }

    /**
     * 配置Jackson ObjectMapper以自定义LocalDateTime的序列化格式
     * 将默认的ISO 8601格式(2026-06-06T17:41:30)转换为更友好的格式(2026-06-06 17:41:30)
     */
    @Bean
    public ObjectMapper objectMapper() {
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        return Jackson2ObjectMapperBuilder.json()
                .modules(javaTimeModule)
                .build();
    }
}
