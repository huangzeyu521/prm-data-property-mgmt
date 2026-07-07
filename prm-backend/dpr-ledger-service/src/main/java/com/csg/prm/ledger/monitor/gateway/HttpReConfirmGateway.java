package com.csg.prm.ledger.monitor.gateway;

import com.csg.prm.common.api.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * 重确权派生网关 HTTP 实现:经 RestClient 调用 dpr-confirm-service 的
 * POST /api/dpr/confirm/apply/re-confirm(ConfirmApplyServiceImpl.createReConfirm),
 * 真正派生一条重确权草稿申请——与季度定时任务(ReConfirmReminderJob)复用同一条服务端逻辑。
 * 仅当 prm.aggregate.enabled=true 时启用并 @Primary 覆盖本地桩 {@link LocalReConfirmGateway}。
 * P0(权益动态监测):此前本地桩只打日志返回 null,监测页"权属变动→重确权"按钮点了等于没点。
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.aggregate.enabled", havingValue = "true")
public class HttpReConfirmGateway implements ReConfirmGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpReConfirmGateway.class);

    private final RestClient client;

    public HttpReConfirmGateway(@Value("${prm.aggregate.confirm-url:http://localhost:9102}") String confirmUrl) {
        this.client = RestClient.builder().baseUrl(confirmUrl).build();
        log.info("[重确权联动] 已启用 HTTP 调用,确权服务地址={}", confirmUrl);
    }

    @Override
    public String trigger(String assetId, String assetName, String rightType, String reason, String sourceRef) {
        Result<String> result = client.post()
                .uri(uriBuilder -> uriBuilder.path("/api/dpr/confirm/apply/re-confirm")
                        .queryParam("assetId", assetId)
                        .queryParamIfPresent("assetName", Optional.ofNullable(assetName))
                        .queryParamIfPresent("rightType", Optional.ofNullable(rightType))
                        .queryParamIfPresent("reason", Optional.ofNullable(reason))
                        .queryParamIfPresent("sourceRef", Optional.ofNullable(sourceRef))
                        .build())
                .header("X-User-Id", "system")
                .retrieve()
                .body(new ParameterizedTypeReference<Result<String>>() { });
        String newApplyId = result != null ? result.getData() : null;
        log.warn("[重确权联动] 资产={} 权属={} 原因={} 来源={} -> 新建重确权申请={}",
                assetId, rightType, reason, sourceRef, newApplyId);
        return newApplyId;
    }
}
