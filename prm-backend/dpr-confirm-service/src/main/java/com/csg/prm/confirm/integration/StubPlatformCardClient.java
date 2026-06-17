package com.csg.prm.confirm.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 平台对接 Stub:平台接口未提供期间的占位实现。
 * - listVisibleAssetIds 返回空 → 档案列表回退 PRM 已确权资产;
 * - pushPropertyAndEquity 仅记录写回载荷、返回 false(pending),不真正外发。
 * TODO:拿到平台接口/数据字典后,替换为 HTTP 实现(WebClient/Feign),并在 AssetCardFieldMapper 登记字段/枚举映射。
 */
@Component
public class StubPlatformCardClient implements PlatformCardClient {

    private static final Logger log = LoggerFactory.getLogger(StubPlatformCardClient.class);

    @Override
    public boolean platformAvailable() {
        return false;
    }

    @Override
    public List<String> listVisibleAssetIds() {
        return List.of();
    }

    @Override
    public List<com.csg.prm.confirm.integration.dto.PlatformCardRef> searchCards(String keyword, int limit) {
        return List.of();
    }

    @Override
    public boolean cardExists(String assetId) {
        return false;
    }

    @Override
    public boolean pushPropertyAndEquity(String assetId, Map<String, Object> property, List<Map<String, Object>> equity) {
        log.info("[资产卡片写回·STUB] assetId={} 产权字段数={} 权益条目={} —— 平台接口未接入,仅构造载荷未真正写回",
                assetId, property == null ? 0 : property.size(), equity == null ? 0 : equity.size());
        return false;
    }
}
