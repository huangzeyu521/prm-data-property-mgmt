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
    public List<com.csg.prm.confirm.integration.dto.PlatformTableMeta> listTableMeta(String assetId) {
        return List.of();
    }

    /**
     * 平台未接入期间:用随包的 AST-001 真实样例材料(resources/platform-stub/*.docx)模拟"平台已上传原件",
     * 使"平台同步材料"可在线预览。按附件名关键词映射到对应样例;无匹配返回 null(该材料无本地可预览原件)。
     * 接入后替换为对平台附件下载 API 的真实 HTTP 取件。
     */
    @Override
    public byte[] fetchAttachment(String assetId, String fileName) {
        String resource = resourceFor(fileName);
        if (resource == null) {
            return null;
        }
        try (java.io.InputStream in = getClass().getResourceAsStream(resource)) {
            return in == null ? null : in.readAllBytes();
        } catch (java.io.IOException e) {
            log.warn("[平台附件·STUB] 读取样例失败 resource={} err={}", resource, e.getMessage());
            return null;
        }
    }

    /** 附件名关键词 → 随包样例资源(ASCII 路径,避免中文类路径)。 */
    private String resourceFor(String fileName) {
        if (fileName == null) {
            return null;
        }
        if (fileName.contains("监管")) {
            return "/platform-stub/ast001-relation-g.docx";
        }
        if (fileName.contains("隐私") || fileName.contains("入网")) {
            return "/platform-stub/ast001-privacy-h.docx";
        }
        if (fileName.contains("来源") || fileName.contains("投入") || fileName.contains("证明")) {
            return "/platform-stub/ast001-source.docx";
        }
        return null;
    }

    @Override
    public boolean pushPropertyAndEquity(String assetId, Map<String, Object> property, List<Map<String, Object>> equity) {
        log.info("[资产卡片写回·STUB] assetId={} 产权字段数={} 权益条目={} —— 平台接口未接入,仅构造载荷未真正写回",
                assetId, property == null ? 0 : property.size(), equity == null ? 0 : equity.size());
        return false;
    }
}
