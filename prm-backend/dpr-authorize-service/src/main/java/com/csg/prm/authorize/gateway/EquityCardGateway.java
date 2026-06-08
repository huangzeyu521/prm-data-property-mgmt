package com.csg.prm.authorize.gateway;

/**
 * 权益卡片网关(先确后授校验端口)。
 * 生产环境由 Feign 调用 dpr-confirm-service 校验卡片是否存在且为"正常"状态;
 * 本地/测试用 {@link LocalEquityCardGateway} 桩实现。
 */
public interface EquityCardGateway {

    /** 确权边界:可用性 + 权属类型 + 确权范围 + 确权有效期(用于"授权⊆确权边界"校验) */
    record CardBoundary(boolean usable, String rightType, String scope, java.time.LocalDateTime validDate) {
    }

    /** 卡片是否可用于授权(存在且状态正常,未冻结/未失效) */
    boolean isUsable(String equityCardId);

    /** 获取卡片确权边界(范围/期限),供"授权范围/期限不得超出确权边界"强校验 */
    CardBoundary boundary(String equityCardId);
}
