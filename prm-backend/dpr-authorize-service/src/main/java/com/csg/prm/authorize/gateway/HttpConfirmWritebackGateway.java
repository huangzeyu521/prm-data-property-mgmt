package com.csg.prm.authorize.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 资产卡片写回网关-跨服务真实实现:授权生效后 HTTP 调 dpr-confirm-service 的
 * POST /api/dpr/confirm/asset/{assetId}/writeback,由确权服务统一把产权/权益结论写回平台卡片。
 * 非致命:确权服务不可达(离线)时仅告警,绝不影响授权主流程。
 */
@Component
@Primary
@Profile("!test")
public class HttpConfirmWritebackGateway implements ConfirmWritebackGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpConfirmWritebackGateway.class);

    private final RestTemplate rest;
    private final String confirmBaseUrl;

    public HttpConfirmWritebackGateway(@Value("${prm.confirm.base-url:http://host.docker.internal:9102}") String confirmBaseUrl) {
        this.confirmBaseUrl = confirmBaseUrl;
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.rest = new RestTemplate(f);
        log.info("[授权→卡片写回] HttpConfirmWritebackGateway 已启用,confirm={}", confirmBaseUrl);
    }

    @Override
    public void writeback(String assetId) {
        if (!StringUtils.hasText(assetId)) {
            return;
        }
        try {
            rest.postForObject(confirmBaseUrl + "/api/dpr/confirm/asset/{assetId}/writeback",
                    null, String.class, assetId);
        } catch (RuntimeException e) {
            log.warn("[授权→卡片写回] 调确权服务失败(不影响授权): assetId={}, 原因={}", assetId, e.getMessage());
        }
    }
}
