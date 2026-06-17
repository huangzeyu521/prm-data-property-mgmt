package com.csg.prm.confirm.integration.dto;

import com.csg.prm.confirm.entity.EquityCard;

import java.time.LocalDateTime;

/**
 * 数据资产卡片「权益基本信息」子Tab 只读契约的一条权益条目(源:权益卡片 EquityCard)。
 * 一个资产可有 0..N 条(持有/使用/经营三权)。
 */
public record AssetEquityVO(
        String cardNo,            // 权益编号
        String rightType,         // 权益类型(三权分置)
        String rightOwner,        // 权益主体
        String rightSource,       // 权益来源
        String scope,             // 权益范围 / 边界(授权范围⊆此边界)
        LocalDateTime validDate,  // 有效期
        String cardStatus,        // 正常 / 冻结 / 失效
        String relatedApplyId     // 关联确权登记
) {
    public static AssetEquityVO of(EquityCard c) {
        return new AssetEquityVO(c.getCardNo(), c.getRightType(), c.getRightOwner(),
                c.getRightSource(), c.getScope(), c.getValidDate(), c.getCardStatus(), c.getApplyId());
    }
}
