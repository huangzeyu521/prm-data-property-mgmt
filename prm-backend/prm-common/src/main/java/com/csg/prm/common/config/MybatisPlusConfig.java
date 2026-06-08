package com.csg.prm.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 共享 MyBatis-Plus 配置。分页方言通过 prm.db-type 切换:
 *   开发/测试 H2 -> MYSQL;生产达梦 -> DM。代码零改动切库。
 * 由各服务 scanBasePackages=com.csg.prm 自动装配。
 */
@Configuration
public class MybatisPlusConfig {

    @Value("${prm.db-type:MYSQL}")
    private String dbType;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DbType type = DbType.getDbType(dbType.toLowerCase());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(type == null ? DbType.MYSQL : type));
        return interceptor;
    }
}
