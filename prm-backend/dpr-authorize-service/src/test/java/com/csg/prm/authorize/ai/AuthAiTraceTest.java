package com.csg.prm.authorize.ai;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthComplianceService;
import com.csg.prm.common.aitrace.AiRunLog;
import com.csg.prm.common.aitrace.AiRunLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权侧大模型校验机制完善(南网需求,打通授权侧):
 * ① 逐次留痕——合规 AI 预审等调用落库,带业务域(授权)/模型/SM3/触发人;
 * ② 防篡改快照——服务端 SM3 + 上链,verify 可验真,篡改即检出;
 * ③ 校验规则可视化——逐授权应交项暴露 校验逻辑 + 规则明细 + AI 判定依据。
 * 基建复用 prm-common 共享 AiRunLogService/AiSnapshotService。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthAiTraceTest {

    @Autowired private AuthApplyService applyService;
    @Autowired private AuthComplianceService complianceService;
    @Autowired private AuthAiService aiService;
    @Autowired private AiRunLogService runLogService;
    @Autowired private AuthApplyMapper applyMapper;

    private String draft() {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId("DA-AUTH-TRACE");
        a.setAssetName("授权留痕测试资产");
        a.setEquityCardId("EC-PRA-VALID01");
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScenario("电力金融征信");
        a.setScope("全字段");
        a.setThirdPartySource("某征信机构");   // 触发第三方许可凭证规则
        a.setSensitiveType("个人隐私");         // 触发信息授权协议规则
        return applyService.saveDraft(a);
    }

    @Test
    void auth_ai_calls_are_logged_with_sm3_and_bizType() {
        String id = draft();
        complianceService.preReview(id); // 合规 AI 预审 -> 授权留痕

        List<AiRunLog> logs = runLogService.listByBiz(id);
        assertTrue(logs.size() >= 1, "授权 AI 调用应逐条留痕,实际 " + logs.size());
        assertTrue(logs.stream().anyMatch(l -> AiRunLog.CAP_AUTH_PRECHECK.equals(l.getCapability())), "应含合规预审留痕");
        assertTrue(logs.stream().allMatch(l -> AiRunLog.BIZ_AUTHORIZE.equals(l.getBizType())), "业务域应标授权");
        assertTrue(logs.stream().allMatch(l -> l.getSm3Hash() != null && !l.getSm3Hash().isBlank()), "每条留痕应有 SM3");
        assertTrue(logs.stream().allMatch(l -> l.getModel() != null && l.getTriggerUser() != null), "应记录模型与触发人");
    }

    @Test
    void auth_snapshot_is_tamper_evident() {
        String id = draft();
        aiService.saveSnapshot(id, "{\"materialCheck\":{\"overall\":\"通过\"}}");

        Map<String, Object> v = aiService.verifySnapshot(id);
        assertEquals(Boolean.TRUE, v.get("verified"), "刚固化的授权快照应验真通过");
        assertNotNull(v.get("evidenceId"), "应有上链存证ID");

        AuthApply a = applyMapper.selectById(id);
        AuthApply upd = new AuthApply();
        upd.setApplyId(id);
        upd.setAiSnapshot(a.getAiSnapshot().replace("通过", "不通过"));
        applyMapper.updateById(upd);

        assertEquals(Boolean.FALSE, aiService.verifySnapshot(id).get("verified"), "篡改后验真应失败(防篡改)");
    }

    @Test
    void auth_check_logic_visualizes_rules() {
        String id = draft();
        AuthAiService.CheckLogic cl = aiService.checkLogic(id);
        assertFalse(cl.items().isEmpty(), "授权应交项校验逻辑不应为空");
        assertTrue(cl.items().stream().anyMatch(i -> i.ruleDetail() != null && !i.ruleDetail().isBlank()), "应暴露规则明细");
        assertTrue(cl.items().stream().anyMatch(i -> i.triggerLabel() != null && i.triggerLabel().contains("必交")), "应暴露校验逻辑(触发规则)");
        assertTrue(cl.items().stream().anyMatch(i -> i.materialName() != null && i.materialName().contains("第三方")), "涉第三方应入校验逻辑");
        assertNotNull(cl.aiModel(), "应标注模型");
    }
}
