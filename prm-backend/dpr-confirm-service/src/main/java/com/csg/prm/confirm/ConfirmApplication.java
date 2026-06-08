package com.csg.prm.confirm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 数据确权管理服务(F-02)启动类。
 */
@SpringBootApplication(scanBasePackages = "com.csg.prm")
@EnableDiscoveryClient
@EnableAsync
@MapperScan({"com.csg.prm.confirm.mapper", "com.csg.prm.confirm.aitool.mapper", "com.csg.prm.common.evidence.mapper"})
public class ConfirmApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfirmApplication.class, args);
    }
}
