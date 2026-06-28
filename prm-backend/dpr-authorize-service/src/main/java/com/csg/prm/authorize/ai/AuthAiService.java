package com.csg.prm.authorize.ai;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthMaterial;
import com.csg.prm.authorize.entity.AuthMaterialRule;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.service.AuthMaterialRuleService;
import com.csg.prm.authorize.service.AuthMaterialService;
import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.aitrace.AiRunLog;
import com.csg.prm.common.aitrace.AiRunLogService;
import com.csg.prm.common.aitrace.AiSnapshotService;
import com.csg.prm.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 授权侧大模型校验机制完善(南网需求):校验规则可视化 + 防篡改快照 + 回放,
 * 复用 prm-common 跨域共享的 {@link AiRunLogService}/{@link AiSnapshotService}。
 */
@Service
public class AuthAiService {

    public static final String EVIDENCE_TYPE = "授权AI校验快照";

    /** 单条应交项的校验逻辑可视化(规则明细 + 判定依据) */
    public record CheckLogicItem(String materialName, String triggerType, String triggerLabel,
                                 String ruleDetail, String required, boolean materialPresent,
                                 String aiVerdict, String aiIssues) {
    }

    /** 校验逻辑可视化结果 */
    public record CheckLogic(List<CheckLogicItem> items, String aiModel, String summary) {
    }

    private final AuthApplyMapper applyMapper;
    private final AuthMaterialRuleService ruleService;
    private final AuthMaterialService materialService;
    private final AiRunLogService runLogService;
    private final AiSnapshotService snapshotService;
    private final DawatAiGateway ai;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthAiService(AuthApplyMapper applyMapper, AuthMaterialRuleService ruleService,
                         AuthMaterialService materialService, AiRunLogService runLogService,
                         AiSnapshotService snapshotService, DawatAiGateway ai) {
        this.applyMapper = applyMapper;
        this.ruleService = ruleService;
        this.materialService = materialService;
        this.runLogService = runLogService;
        this.snapshotService = snapshotService;
        this.ai = ai;
    }

    /** 校验过程回放:该授权申请全部大模型操作留痕时间线。 */
    public List<AiRunLog> runlog(String applyId) {
        return runLogService.listByBiz(applyId);
    }

    /**
     * 校验规则可视化(§1):逐应交项展示 校验逻辑(触发规则)+ 规则明细 + 判定依据(AI 结论/问题)。
     * 规则取授权应交规则单一真源;判定依据取该申请最近一次授权材料 AI 校验留痕。
     */
    public CheckLogic checkLogic(String applyId) {
        AuthApply a = mustApply(applyId);
        List<AuthMaterialRule> rules = ruleService.requiredRules(a);
        Set<String> present = new LinkedHashSet<>();
        for (AuthMaterial m : materialService.listByApply(applyId)) {
            present.add(m.getMaterialName() == null ? m.getFileName() : m.getMaterialName());
        }
        Map<String, String[]> aiByName = latestMaterialCheckVerdicts(applyId);

        List<CheckLogicItem> items = new ArrayList<>();
        for (AuthMaterialRule r : rules) {
            String[] verdict = aiByName.get(r.getMaterialName());
            items.add(new CheckLogicItem(r.getMaterialName(), r.getTriggerType(), triggerLabelOf(r),
                    r.getDetail(), r.getRequired(), present.contains(r.getMaterialName()),
                    verdict == null ? null : verdict[0], verdict == null ? null : verdict[1]));
        }
        long present0 = items.stream().filter(CheckLogicItem::materialPresent).count();
        String summary = "应交 " + items.size() + " 项,已交 " + present0 + ",含 AI 判定 " + aiByName.size()
                + " 项;规则源自授权应交清单(单一真源),判定依据取最近一次授权材料 AI 校验留痕。";
        return new CheckLogic(items, ai.modelName(), summary);
    }

    /** 固化防篡改 AI 校验快照,存入授权申请单。 */
    @Transactional
    public void saveSnapshot(String applyId, String clientJson) {
        AuthApply apply = mustApply(applyId);
        String sealed = snapshotService.seal(EVIDENCE_TYPE, applyId, apply.getAssetName(), clientJson);
        AuthApply upd = new AuthApply();
        upd.setApplyId(applyId);
        upd.setAiSnapshot(sealed);
        applyMapper.updateById(upd);
    }

    /** 校验已固化快照完整性(重算 SM3 比对存证)。 */
    public Map<String, Object> verifySnapshot(String applyId) {
        AuthApply apply = applyMapper.selectById(applyId);
        Map<String, Object> r = snapshotService.verify(apply == null ? null : apply.getAiSnapshot());
        r.put("applyId", applyId);
        return r;
    }

    private String triggerLabelOf(AuthMaterialRule r) {
        return switch (r.getTriggerType()) {
            case AuthMaterialRule.T_ALWAYS -> "始终必交(核心表单)";
            case AuthMaterialRule.T_THIRD_PARTY -> "涉第三方来源时必交";
            case AuthMaterialRule.T_SENSITIVE -> "涉个人隐私/商业秘密时必交";
            default -> r.getTriggerType();
        };
    }

    private Map<String, String[]> latestMaterialCheckVerdicts(String applyId) {
        Map<String, String[]> out = new HashMap<>();
        AiRunLog last = null;
        for (AiRunLog l : runLogService.listByBiz(applyId)) {
            if (AiRunLog.CAP_AUTH_MATERIAL_CHECK.equals(l.getCapability())) {
                last = l;
            }
        }
        if (last == null || !StringUtils.hasText(last.getOutput())) {
            return out;
        }
        try {
            JsonNode arr = objectMapper.readTree(last.getOutput()).get("items");
            if (arr != null && arr.isArray()) {
                for (JsonNode it : arr) {
                    String name = it.path("materialName").asText(null);
                    if (name != null) {
                        out.put(name, new String[]{it.path("verdict").asText(null), it.path("issues").asText(null)});
                    }
                }
            }
        } catch (Exception ignore) {
            // 输出非预期结构:判定依据留空
        }
        return out;
    }

    private AuthApply mustApply(String applyId) {
        AuthApply a = applyMapper.selectById(applyId);
        if (a == null) {
            throw new BusinessException("授权申请不存在");
        }
        return a;
    }
}
