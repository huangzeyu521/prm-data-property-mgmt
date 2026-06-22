package com.csg.prm.common.aitrace;

import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 校验快照——服务端化 + 防篡改(跨域共享:确权 + 授权)。
 * seal:把展示载荷在服务端计 SM3、上链存证、关联本业务全部 AI 留痕,封装为防篡改包(由调用方存入自有列)。
 * verify:重算 payload 的 SM3 并与存证比对(防篡改验真)。本服务不绑定任何业务表。
 */
@Service
public class AiSnapshotService {

    private final ChainEvidenceService chainEvidenceService;
    private final AiRunLogService aiRunLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiSnapshotService(ChainEvidenceService chainEvidenceService, AiRunLogService aiRunLogService) {
        this.chainEvidenceService = chainEvidenceService;
        this.aiRunLogService = aiRunLogService;
    }

    /**
     * 固化防篡改快照:计 SM3 + 上链 + 关联本业务全部 AI 留痕,返回封装后的快照串(调用方存入自有列)。
     * @param evidenceType 存证业务类型(如 确权AI校验快照 / 授权AI校验快照)
     * @param bizId        业务主键
     * @param bizName      人读摘要(资产名等)
     * @param clientJson   前端展示载荷
     */
    public String seal(String evidenceType, String bizId, String bizName, String clientJson) {
        JsonNode payload = readTree(clientJson);
        String canonical = canonical(payload);
        String sm3 = Sm3Util.hashHex(canonical);
        String evidenceId = chainEvidenceService.anchor(evidenceType, bizId,
                "AI 校验快照 / " + (StringUtils.hasText(bizName) ? bizName : bizId), canonical);
        List<String> runLogIds = aiRunLogService.listByBiz(bizId).stream().map(AiRunLog::getLogId).toList();

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("payload", payload);
        wrapper.put("payloadSm3", sm3);
        wrapper.put("evidenceId", evidenceId);
        wrapper.put("aiRunCount", runLogIds.size());
        wrapper.set("aiRunLogIds", objectMapper.valueToTree(runLogIds));
        return wrapper.toString();
    }

    /** 校验已固化快照串的完整性:重算 payload SM3 并与存证比对(防篡改验真)。 */
    public Map<String, Object> verify(String snapshotJson) {
        Map<String, Object> r = new LinkedHashMap<>();
        if (!StringUtils.hasText(snapshotJson)) {
            r.put("verified", false);
            r.put("reason", "无 AI 校验快照");
            return r;
        }
        JsonNode wrapper = readTree(snapshotJson);
        JsonNode payload = wrapper.get("payload");
        String storedSm3 = wrapper.path("payloadSm3").asText(null);
        String evidenceId = wrapper.path("evidenceId").asText(null);
        if (payload == null || !StringUtils.hasText(storedSm3) || !StringUtils.hasText(evidenceId)) {
            r.put("verified", false);
            r.put("reason", "旧格式快照(无防篡改元信息),建议重新一键校验后提交");
            return r;
        }
        String canonical = canonical(payload);
        boolean sm3Match = Sm3Util.hashHex(canonical).equals(storedSm3);
        boolean chainOk = chainEvidenceService.verify(evidenceId, canonical);
        r.put("verified", sm3Match && chainOk);
        r.put("sm3Match", sm3Match);
        r.put("chainOk", chainOk);
        r.put("payloadSm3", storedSm3);
        r.put("evidenceId", evidenceId);
        r.put("aiRunCount", wrapper.path("aiRunCount").asInt(0));
        r.put("reason", (sm3Match && chainOk) ? "快照完整,未被篡改" : "快照与存证不一致,疑似被篡改");
        return r;
    }

    private String canonical(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    private JsonNode readTree(String json) {
        try {
            return StringUtils.hasText(json) ? objectMapper.readTree(json) : objectMapper.createObjectNode();
        } catch (Exception e) {
            ObjectNode n = objectMapper.createObjectNode();
            n.put("_raw", json == null ? "" : json);
            return n;
        }
    }
}
