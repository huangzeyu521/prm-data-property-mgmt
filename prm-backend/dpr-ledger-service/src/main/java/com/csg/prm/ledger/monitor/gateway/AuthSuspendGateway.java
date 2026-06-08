package com.csg.prm.ledger.monitor.gateway;

/**
 * 授权熔断网关(权益动态监测 F-01 -> 数据授权 F-03)。
 * 监测识别违规/越权时,联动暂停被授权资产下的生效证书并建追责。
 * 生产环境由 Feign 调用 dpr-authorize-service 的 /api/dpr/auth/cert/suspend-by-asset;
 * 本地/测试用 {@link LocalAuthSuspendGateway} 桩实现。
 */
public interface AuthSuspendGateway {

    /**
     * 暂停某资产下全部生效授权证书。
     * @return 被暂停的证书数量(本地桩返回 0)
     */
    int suspendByAsset(String assetId, String reason, String sourceAlertId, String violationType);
}
