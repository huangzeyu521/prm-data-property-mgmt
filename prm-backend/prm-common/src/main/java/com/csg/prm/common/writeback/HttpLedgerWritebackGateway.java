package com.csg.prm.common.writeback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 台账回写网关 HTTP 实现:经 RestClient 调用台账服务 /api/dpr/ledger/archive/writeback。
 * 仅当 prm.ledger.writeback-enabled=true 时启用并 @Primary 覆盖本地桩。
 * 回写失败仅告警、不抛出(确权/授权主流程已成功,保证最终一致由重试/对账兜底)。
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.ledger.writeback-enabled", havingValue = "true")
public class HttpLedgerWritebackGateway implements LedgerWritebackGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpLedgerWritebackGateway.class);

    private final RestClient client;

    public HttpLedgerWritebackGateway(
            @Value("${prm.ledger.writeback-url:http://localhost:9101}") String ledgerUrl) {
        this.client = RestClient.builder().baseUrl(ledgerUrl).build();
        log.info("[台账回写] 已启用 HTTP 回写,台账地址={}", ledgerUrl);
    }

    @Override
    public void apply(RightsEvent event) {
        try {
            client.post().uri("/api/dpr/ledger/archive/writeback")
                    .header("Content-Type", "application/json")
                    .header("X-User-Id", "system")
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[台账回写] 事件={} 资产={} 回写成功", event.getEventType(), event.getAssetId());
        } catch (RuntimeException e) {
            log.warn("[台账回写] 事件={} 资产={} 回写失败(不阻断主流程): {}",
                    event.getEventType(), event.getAssetId(), e.getMessage());
        }
    }
}
