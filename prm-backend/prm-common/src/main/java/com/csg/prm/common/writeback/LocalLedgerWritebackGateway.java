package com.csg.prm.common.writeback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 台账回写网关本地桩(默认):仅记录回写意图,不跨服务调用。
 * 用于离线/单测(确权、授权域测试不依赖台账服务)。生产/联机由 {@link HttpLedgerWritebackGateway} @Primary 覆盖。
 */
@Component
public class LocalLedgerWritebackGateway implements LedgerWritebackGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalLedgerWritebackGateway.class);

    @Override
    public void apply(RightsEvent event) {
        log.info("[台账回写-本地桩] 事件={} 资产={} 确权状态={} 授权状态={} 卡片={} (未启用真实回写)",
                event.getEventType(), event.getAssetId(), event.getConfirmStatus(),
                event.getAuthStatus(), event.getEquityCardId());
    }
}
