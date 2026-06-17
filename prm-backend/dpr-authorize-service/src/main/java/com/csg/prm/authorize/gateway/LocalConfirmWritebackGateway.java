package com.csg.prm.authorize.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 资产卡片写回网关本地桩:仅记录,不真正跨服务调用。用于本地联调与单测(保持确定性、不依赖确权服务在线)。
 * 生产由 {@link HttpConfirmWritebackGateway}(@Primary,非 test profile)覆盖。
 */
@Component
public class LocalConfirmWritebackGateway implements ConfirmWritebackGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalConfirmWritebackGateway.class);

    @Override
    public void writeback(String assetId) {
        log.debug("[授权→卡片写回·桩] assetId={} —— 本地桩不跨服务调用", assetId);
    }
}
