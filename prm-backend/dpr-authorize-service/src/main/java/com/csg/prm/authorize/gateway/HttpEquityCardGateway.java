package com.csg.prm.authorize.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * 权益卡片网关-跨服务真实实现:HTTP 调 dpr-confirm-service 查询卡片真实状态,
 * 使"先确后授/冻结熔断"基于确权侧实况生效(修复:本地桩仅看卡号字符串,真实冻结不熔断)。
 * confirm 服务不可达(单测/离线)时回退 {@link LocalEquityCardGateway} 桩,保持测试确定性。
 */
@Component
@Primary
@org.springframework.context.annotation.Profile("!test")
public class HttpEquityCardGateway implements EquityCardGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpEquityCardGateway.class);
    private static final Set<String> USABLE_STATUS = Set.of("正常", "生效");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RestTemplate rest;
    private final String confirmBaseUrl;
    private final LocalEquityCardGateway fallback;

    public HttpEquityCardGateway(@Value("${prm.confirm.base-url:http://host.docker.internal:9102}") String confirmBaseUrl,
                                 LocalEquityCardGateway fallback) {
        this.confirmBaseUrl = confirmBaseUrl;
        this.fallback = fallback;
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
        this.rest = new RestTemplate(f);
        log.info("[先确后授] HttpEquityCardGateway 已启用,confirm={}", confirmBaseUrl);
    }

    @Override
    public boolean isUsable(String equityCardId) {
        if (!StringUtils.hasText(equityCardId)) {
            return false;
        }
        Map<?, ?> card = fetchCard(equityCardId);
        if (card == REMOTE_DOWN) {
            log.debug("[先确后授] isUsable({}) 远端不可达→回退桩", equityCardId);
            return fallback.isUsable(equityCardId);
        }
        log.debug("[先确后授] isUsable({}) 实况status={}", equityCardId, card == null ? "卡不存在" : card.get("cardStatus"));
        // 远端可达:以确权侧实况为准——卡不存在或状态非 正常/生效 即不可用(冻结/失效熔断)
        return card != null && USABLE_STATUS.contains(String.valueOf(card.get("cardStatus")));
    }

    @Override
    public CardBoundary boundary(String equityCardId) {
        Map<?, ?> card = fetchCard(equityCardId);
        if (card == REMOTE_DOWN) {
            return fallback.boundary(equityCardId);
        }
        if (card == null) {
            return new CardBoundary(false, null, null, null);
        }
        boolean usable = USABLE_STATUS.contains(String.valueOf(card.get("cardStatus")));
        LocalDateTime valid = parseTime(card.get("validDate"));
        // 卡片无独立范围字段,确权边界范围按"全字段"口径(与既有规则一致)
        return new CardBoundary(usable, str(card.get("rightType")), "全字段",
                valid != null ? valid : LocalDateTime.now().plusYears(5));
    }

    /** 远端不可达哨兵(区别于"卡不存在"=null) */
    private static final Map<?, ?> REMOTE_DOWN = Map.of("__down__", true);

    private Map<?, ?> fetchCard(String cardNo) {
        try {
            ResponseEntity<Map> resp = rest.getForEntity(
                    confirmBaseUrl + "/api/dpr/confirm/card/by-no/" + cardNo, Map.class);
            Map<?, ?> body = resp.getBody();
            if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
                return REMOTE_DOWN;
            }
            Object data = body.get("data");
            return data instanceof Map ? (Map<?, ?>) data : null;
        } catch (Exception e) {
            log.debug("[先确后授] fetchCard({}) 异常,回退桩: {}", cardNo, e.getMessage());
            return REMOTE_DOWN;
        }
    }

    private LocalDateTime parseTime(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(v).substring(0, 19), DT);
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
