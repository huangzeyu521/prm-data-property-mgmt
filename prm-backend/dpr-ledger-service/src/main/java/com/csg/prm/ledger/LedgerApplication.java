package com.csg.prm.ledger;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 产权信息台账服务(F-01)启动类。
 * scanBasePackages 覆盖 com.csg.prm 以装配 prm-common 的全局组件(异常处理、字段自动填充等)。
 */
@SpringBootApplication(scanBasePackages = "com.csg.prm")
@EnableDiscoveryClient
@EnableScheduling
@MapperScan({"com.csg.prm.ledger.mapper", "com.csg.prm.ledger.monitor.mapper", "com.csg.prm.common.evidence.mapper"})
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
