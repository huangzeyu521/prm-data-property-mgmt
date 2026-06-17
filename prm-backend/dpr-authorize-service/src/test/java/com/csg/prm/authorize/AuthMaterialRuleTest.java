package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthMaterialRule;
import com.csg.prm.authorize.service.AuthMaterialRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权可配置应交材料清单规则:种入校验(批量/一事一议)+ 按 涉第三方/涉隐私商密 触发生成清单。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthMaterialRuleTest {

    @Autowired
    private AuthMaterialRuleService ruleService;

    /** 启动种入:两场景各 3 条(表5 + 第三方许可凭证 + 信息授权协议)。 */
    @Test
    void seed_loads_both_scenes() {
        List<AuthMaterialRule> batch = ruleService.listEnabled(AuthMaterialRuleService.SCENE_BATCH);
        List<AuthMaterialRule> special = ruleService.listEnabled(AuthMaterialRuleService.SCENE_SPECIAL);
        assertEquals(3, batch.size(), "批量应种入 3 条规则");
        assertEquals(3, special.size(), "一事一议应种入 3 条规则");
        assertTrue(batch.stream().anyMatch(r -> r.getMaterialName().contains("表5")), "应含表5");
        assertTrue(batch.stream().anyMatch(r -> AuthMaterialRule.T_ALWAYS.equals(r.getTriggerType())), "应含常交项");
    }

    /** 无第三方、无敏感:仅表5。 */
    @Test
    void plain_apply_yields_only_table5() {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        List<String> req = ruleService.requiredNames(a);
        assertEquals(1, req.size());
        assertTrue(req.get(0).contains("表5"));
    }

    /** 涉第三方来源:加"第三方许可凭证";未涉敏感则不加"信息授权协议"。 */
    @Test
    void third_party_adds_license_only() {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setThirdPartySource("某征信机构数据");
        List<String> req = ruleService.requiredNames(a);
        assertTrue(req.stream().anyMatch(n -> n.contains("第三方许可凭证")), "涉三方应加许可凭证");
        assertFalse(req.stream().anyMatch(n -> n.contains("信息授权协议")), "未涉敏感不应加信息授权协议");
    }

    /** 涉个人隐私/商密:加"信息授权协议"。 */
    @Test
    void sensitive_adds_info_agreement() {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        a.setSensitiveType("个人隐私");
        List<String> req = ruleService.requiredNames(a);
        assertTrue(req.stream().anyMatch(n -> n.contains("信息授权协议")), "涉敏感应加信息授权协议");
        assertFalse(req.stream().anyMatch(n -> n.contains("第三方许可凭证")), "未涉三方不应加许可凭证");
    }
}
