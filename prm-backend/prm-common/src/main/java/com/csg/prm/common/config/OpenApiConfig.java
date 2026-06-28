package com.csg.prm.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 3 文档配置(共享,放 prm-common 供各服务自动装配)。
 * <p>统一 {@link io.swagger.v3.oas.models.info.Info}(标题取 spring.application.name),并声明内建登录
 * JWT(Authorization: Bearer)鉴权方案,供 swagger-ui 在线调试时一键 Authorize。
 * <p>统一返回体 Result{code,message,data,timestamp}、分页 PageResult{total,pageNum,pageSize,records}、
 * 分页入参 PageQuery{current,size} 由 springdoc 自动 introspect,无需额外注册;业务异常经
 * GlobalExceptionHandler 统一为 Result(HTTP 200,体内 code 携带业务码)。
 * <p>UI:{@code /swagger-ui.html}(在线预览调试),机读:{@code /v3/api-docs}。鉴权拦截仅作用于
 * {@code /api/**},文档端点不受 RBAC 拦截。
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:数据产权管理}")
    private String appName;

    @Bean
    public OpenAPI prmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " 接口文档")
                        .version("0.1.0-SNAPSHOT")
                        .description("中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。"
                                + "统一返回体 Result{code,message,data,timestamp};分页 PageResult{total,pageNum,pageSize,records};"
                                + "业务异常经 GlobalExceptionHandler 统一为 Result(HTTP 200,体内 code 携带业务码)。")
                        .contact(new Contact().name("数据产权管理 IM-DAM-DPR"))
                        .license(new License().name("CSG Proprietary")))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("内建登录 JWT;格式 Authorization: Bearer <token>"
                                        + "(prm.auth.enabled=true 时 /api/** 强制校验角色)")));
    }
}
