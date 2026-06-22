package com.csg.prm.confirm.service;

import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmAiRunLog;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 确权 AI 校验快照——服务端化 + 防篡改(南网"全流程留痕追溯"§2):
 * 提交时把(材料AI校验+规则完整性+权益归集)载荷在服务端计 SM3、上链存证、关联逐次留痕,
 * 封装为防篡改快照存于申请;人工预审/审计可一键校验快照完整性(重算 SM3 比对存证)。
 */
@Service
public class ConfirmAiSnapshotService {

    public static final String EVIDENCE_TYPE = "确权AI校验快照";

    private final ConfirmApplyMapper applyMapper;
    private final ChainEvidenceService chainEvidenceService;
    private final ConfirmAiRunLogService aiRunLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfirmAiSnapshotService(ConfirmApplyMapper applyMapper, ChainEvidenceService chainEvidenceService,
                                    ConfirmAiRunLogService aiRunLogService) {
        this.applyMapper = applyMapper;
        this.chainEvidenceService = chainEvidenceService;
        this.aiRunLogService = aiRunLogService;
    }

    /**
     * 固化防篡改 AI 校验快照:服务端计 SM3 + 上链 + 关联本申请全部 AI 留痕,封装入库。
     * @param clientJson 前端展示载荷(materialCheck/ruleReport/consolidation/checkedAt/qualityScore)
     */
    @Transactional
    public void save(String applyId, String clientJson) {
        ConfirmApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException("确权申请不存在");
        }
        JsonNode payload = readTree(clientJson);
        String canonical = canonical(payload);
        String sm3 = Sm3Util.hashHex(canonical);
        // 上链存证(SM3 指纹锚定):防篡改、可审计
        String evidenceId = chainEvidenceService.anchor(EVIDENCE_TYPE, applyId,
                "AI 校验快照 / " + (apply.getAssetName() == null ? applyId : apply.getAssetName()), canonical);
        List<String> runLogIds = aiRunLogService.listByApply(applyId).stream()
                .map(ConfirmAiRunLog::getLogId).toList();

        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("payload", payload);
        wrapper.put("payloadSm3", sm3);
        wrapper.put("evidenceId", evidenceId);
        wrapper.put("aiRunCount", runLogIds.size());
        wrapper.set("aiRunLogIds", objectMapper.valueToTree(runLogIds));

        ConfirmApply upd = new ConfirmApply();
        upd.setApplyId(applyId);
        upd.setAiSnapshot(wrapper.toString());
        applyMapper.updateById(upd);
    }

    /**
     * 校验已固化快照的完整性:重算 payload 的 SM3 并与存证比对(防篡改验真)。
     * @return verified/payloadSm3/evidenceId/reason
     */
    public Map<String, Object> verify(String applyId) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("applyId", applyId);
        ConfirmApply apply = applyMapper.selectById(applyId);
        if (apply == null || !StringUtils.hasText(apply.getAiSnapshot())) {
            r.put("verified", false);
            r.put("reason", "无 AI 校验快照");
            return r;
        }
        JsonNode wrapper = readTree(apply.getAiSnapshot());
        JsonNode payload = wrapper.get("payload");
        String storedSm3 = wrapper.path("payloadSm3").asText(null);
        String evidenceId = wrapper.path("evidenceId").asText(null);
        if (payload == null || !StringUtils.hasText(storedSm3) || !StringUtils.hasText(evidenceId)) {
            r.put("verified", false);
            r.put("reason", "旧格式快照(无防篡改元信息),建议重新一键校验后提交");
            return r;
        }
        String canonical = canonical(payload);
        String recomputed = Sm3Util.hashHex(canonical);
        boolean sm3Match = recomputed.equals(storedSm3);
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

    /** 规范化序列化(确保 save/verify 计算同一字节序,SM3 可复算)。 */
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
            // 非法 JSON:包装为对象避免 NPE,SM3 仍可对其计算
            ObjectNode n = objectMapper.createObjectNode();
            n.put("_raw", json == null ? "" : json);
            return n;
        }
    }
}
