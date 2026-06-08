package com.csg.prm.authorize;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 数据授权管理服务(F-03)启动类。
 */
@SpringBootApplication(scanBasePackages = "com.csg.prm")
@EnableDiscoveryClient
@MapperScan({"com.csg.prm.authorize.mapper", "com.csg.prm.common.evidence.mapper"})
public class AuthorizeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizeApplication.class, args);
    }
}
