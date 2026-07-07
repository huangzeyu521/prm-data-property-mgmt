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
 * 授权熔断网关 HTTP 实现:经 RestClient 调用 dpr-authorize-service 的
 * POST /api/dpr/auth/cert/suspend-by-asset,真正暂停被授权资产下的生效证书。
 * 仅当 prm.aggregate.enabled=true 时启用并 @Primary 覆盖本地桩 {@link LocalAuthSuspendGateway}。
 * P0(权益动态监测):此前本地桩只打日志返回 0,UI 会显示"已熔断:暂停 0 张授权证书"——看似处理成功,
 * 实际什么都没发生。这是用户主动发起的动作(不是后台静默回写),调用失败须让异常往上抛,
 * 使前端拿到真实错误提示,而不是把失败伪装成"确认无需暂停"的假成功。
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.aggregate.enabled", havingValue = "true")
public class HttpAuthSuspendGateway implements AuthSuspendGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpAuthSuspendGateway.class);

    private final RestClient client;

    public HttpAuthSuspendGateway(@Value("${prm.aggregate.auth-url:http://localhost:9103}") String authUrl) {
        this.client = RestClient.builder().baseUrl(authUrl).build();
        log.info("[监测联动熔断] 已启用 HTTP 调用,授权服务地址={}", authUrl);
    }

    @Override
    public int suspendByAsset(String assetId, String reason, String sourceAlertId, String violationType) {
        Result<Integer> result = client.post()
                .uri(uriBuilder -> uriBuilder.path("/api/dpr/auth/cert/suspend-by-asset")
                        .queryParam("assetId", assetId)
                        .queryParamIfPresent("reason", Optional.ofNullable(reason))
                        .queryParamIfPresent("sourceAlertId", Optional.ofNullable(sourceAlertId))
                        .queryParamIfPresent("violationType", Optional.ofNullable(violationType))
                        .build())
                .header("X-User-Id", "system")
                .retrieve()
                .body(new ParameterizedTypeReference<Result<Integer>>() { });
        int suspended = (result != null && result.getData() != null) ? result.getData() : 0;
        log.warn("[监测联动熔断] 资产={} 违规类型={} 原因={} 来源预警={} -> 暂停证书数={}",
                assetId, violationType, reason, sourceAlertId, suspended);
        return suspended;
    }
}
