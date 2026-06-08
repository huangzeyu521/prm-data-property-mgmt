package com.csg.prm.ledger.monitor.gateway;

/**
 * 重确权派生网关(权益动态监测 F-01 -> 数据确权 F-02)。
 * 监测识别数据新增/来源变更/到期时,联动派生重确权工单(附录F 3.3.2 季度重确权)。
 * 生产环境由 Feign 调用 dpr-confirm-service 的 /api/dpr/confirm/apply/re-confirm;
 * 本地/测试用 {@link LocalReConfirmGateway} 桩实现。
 */
public interface ReConfirmGateway {

    /**
     * 派生重确权工单。
     * @return 新建重确权申请ID(本地桩返回 null)
     */
    String trigger(String assetId, String assetName, String rightType, String reason, String sourceRef);
}
