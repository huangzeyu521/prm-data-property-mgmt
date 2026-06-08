package com.csg.prm.authorize.gateway;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 权益卡片网关本地桩实现:卡片编号非空且非"FROZEN"前缀视为可用。
 * 用于本地联调与单测验证"先确后授 / 冻结熔断"规则;
 * 生产环境提供基于 Feign 调用 dpr-confirm-service 的实现,并以 @Primary 覆盖此桩。
 */
@Component
public class LocalEquityCardGateway implements EquityCardGateway {

    @Override
    public boolean isUsable(String equityCardId) {
        return StringUtils.hasText(equityCardId) && !equityCardId.startsWith("FROZEN");
    }

    @Override
    public CardBoundary boundary(String equityCardId) {
        String id = equityCardId == null ? "" : equityCardId;
        // 约定:卡片号含 NARROW -> 确权范围"约定字段";含 SHORT -> 确权有效期仅 10 天(便于"授权⊆确权边界"校验)
        String scope = id.contains("NARROW") ? "约定字段" : "全字段";
        java.time.LocalDateTime valid = id.contains("SHORT")
                ? java.time.LocalDateTime.now().plusDays(10)
                : java.time.LocalDateTime.now().plusYears(5);
        return new CardBoundary(isUsable(id), null, scope, valid);
    }
}
