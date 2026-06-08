package com.csg.prm.ledger.monitor.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 授权熔断网关本地桩。无 authorize 服务依赖时记录联动意图并返回 0。
 * 生产环境另以 Feign 实现 + @Primary 覆盖,真正调用 dpr-authorize-service 暂停证书。
 */
@Component
public class LocalAuthSuspendGateway implements AuthSuspendGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalAuthSuspendGateway.class);

    @Override
    public int suspendByAsset(String assetId, String reason, String sourceAlertId, String violationType) {
        log.warn("[监测联动熔断-本地桩] 资产={} 违规类型={} 原因={} 来源预警={} -> 待生产Feign调用授权服务暂停证书",
                assetId, violationType, reason, sourceAlertId);
        return 0;
    }
}
