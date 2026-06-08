package com.csg.prm.ledger.monitor.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 重确权派生网关本地桩。无 confirm 服务依赖时记录派生意图并返回 null。
 * 生产环境另以 Feign 实现 + @Primary 覆盖,真正调用 dpr-confirm-service 派生重确权工单。
 */
@Component
public class LocalReConfirmGateway implements ReConfirmGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalReConfirmGateway.class);

    @Override
    public String trigger(String assetId, String assetName, String rightType, String reason, String sourceRef) {
        log.warn("[重确权联动-本地桩] 资产={} 权属={} 原因={} 来源={} -> 待生产Feign调用确权服务派生重确权工单",
                assetId, rightType, reason, sourceRef);
        return null;
    }
}
