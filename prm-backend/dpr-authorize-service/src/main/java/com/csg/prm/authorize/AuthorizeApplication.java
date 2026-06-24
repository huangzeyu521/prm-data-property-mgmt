/*
 * Copyright (C) 2026 China Southern Power Grid Co., Ltd. All Rights Reserved.
 * 中国南方电网 · 数据资产管理平台 V3.6 · 数据产权管理模块(IM-DAM-DPR)。
 * 本软件版权归中国南方电网所有,未经书面授权不得复制、修改或发布。
 */
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
@MapperScan({"com.csg.prm.authorize.mapper", "com.csg.prm.common.evidence.mapper", "com.csg.prm.common.aitrace.mapper", "com.csg.prm.common.org.mapper"})
public class AuthorizeApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorizeApplication.class, args);
    }
}
