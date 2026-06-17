package com.csg.prm.confirm.integration;

import com.csg.prm.confirm.integration.dto.AssetPropertyVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 资产卡片写回服务(单向 PRM→平台):确权/授权动作完成后,把该资产的产权/权益结论写回平台卡片。
 * PRM 为结论真源,平台卡片是写回的只读展示副本。
 *
 * 触发点(待接入,见各处 TODO):
 *  - 确权终审通过(ConfirmApply→已完成、生成权益卡片)后调用 writeback(assetId);
 *  - 授权完成(协议签订/权益变更)后调用 writeback(assetId)。
 * 平台接口未提供期间走 StubPlatformCardClient:仅构造载荷、不真正外发。
 */
@Service
public class AssetCardWritebackService {

    private final AssetCardIntegrationService integration;
    private final AssetCardFieldMapper adapter;
    private final PlatformCardClient platform;

    public AssetCardWritebackService(AssetCardIntegrationService integration,
                                     AssetCardFieldMapper adapter, PlatformCardClient platform) {
        this.integration = integration;
        this.adapter = adapter;
        this.platform = platform;
    }

    public record WritebackResult(String assetId, boolean accepted, String note) {
    }

    /** 写回某资产的产权+权益结论。仅"已确权"才写回。 */
    public WritebackResult writeback(String assetId) {
        if (!StringUtils.hasText(assetId)) {
            return new WritebackResult(assetId, false, "缺少资产ID");
        }
        AssetPropertyVO p = integration.property(assetId);
        if (!AssetCardIntegrationService.STATE_DONE.equals(p.state())) {
            return new WritebackResult(assetId, false, "尚未确权完成,暂不写回(state=" + p.state() + ")");
        }
        Map<String, Object> propertyPayload = adapter.toPlatformProperty(p);
        List<Map<String, Object>> equityPayload = adapter.toPlatformEquity(integration.equity(assetId));
        boolean ok = platform.pushPropertyAndEquity(assetId, propertyPayload, equityPayload);
        return new WritebackResult(assetId, ok,
                ok ? "已写回平台卡片产权信息/权益基本信息"
                   : "平台接口未接入,已构造写回载荷待推送(stub),产权字段" + propertyPayload.size()
                        + "项/权益" + equityPayload.size() + "条");
    }
}
