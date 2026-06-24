/*
 * Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
 * 中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
 * 本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
 */
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
@MapperScan({"com.csg.prm.ledger.mapper", "com.csg.prm.ledger.monitor.mapper", "com.csg.prm.common.evidence.mapper", "com.csg.prm.common.aitrace.mapper", "com.csg.prm.common.org.mapper"})
public class LedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
    }
}
