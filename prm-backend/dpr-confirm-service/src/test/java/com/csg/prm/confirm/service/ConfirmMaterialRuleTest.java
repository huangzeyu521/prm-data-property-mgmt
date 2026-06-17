package com.csg.prm.confirm.service;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmMaterialRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可配置应交材料清单规则:种入校验 + 按 A–J/涉三方触发生成应交清单(替代硬编码 CODE_MATERIAL)。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmMaterialRuleTest {

    @Autowired
    private ConfirmMaterialRuleService ruleService;

    /** 启动种入:确权场景应有联调清单 Excel 的全部规则(2 常交 + 表2 + A–F + G–J = 13)。 */
    @Test
    void seed_loads_confirm_rules() {
        List<ConfirmMaterialRule> rules = ruleService.listEnabled(ConfirmMaterialRuleService.SCENE_CONFIRM);
        assertTrue(rules.size() >= 13, "应种入 ≥13 条规则,实际 " + rules.size());
        assertTrue(rules.stream().anyMatch(r -> "A".equals(r.getTriggerCode())), "应含来源码 A");
        assertTrue(rules.stream().anyMatch(r -> "J".equals(r.getTriggerCode())), "应含关联码 J");
        assertTrue(rules.stream().anyMatch(r -> ConfirmMaterialRule.T_ALWAYS.equals(r.getTriggerType())), "应含常交项");
    }

    /** 未勾选任何来源/关联:仅常交项,不含 A–J 条件材料。 */
    @Test
    void no_selection_yields_only_always_items() {
        ConfirmApply a = new ConfirmApply();
        List<String> req = ruleService.requiredNames(a);
        assertTrue(req.contains("《表1 数据确权信息清单(系统级)》"), "应含表1");
        assertFalse(req.stream().anyMatch(n -> n.contains("自行生产")), "未选 A 不应含 A 材料");
        assertFalse(req.stream().anyMatch(n -> n.contains("表2")), "未涉三方不应含表2");
    }

    /** 来源识别命中(兼容 "A自行生产数据" 长串):仅对应码材料入清单,未选码不入。 */
    @Test
    void source_codes_drive_checklist() {
        ConfirmApply a = new ConfirmApply();
        a.setSourceIdentification("A自行生产数据,E交易采购数据");
        List<String> req = ruleService.requiredNames(a);
        assertTrue(req.contains("数据来源设备/系统建设投入情况说明"), "A 材料应入清单");
        assertTrue(req.contains("交易采购情况说明"), "E 材料应入清单");
        assertFalse(req.contains("公共数据授权说明"), "未选 C 不应入清单");
    }

    /** 关联识别 + 涉三方:G–J 材料 + 表2 入清单。 */
    @Test
    void relation_codes_and_table2_drive_checklist() {
        ConfirmApply a = new ConfirmApply();
        a.setRelationIdentification("H,J");
        a.setInvolvesThirdParty(true);
        List<String> req = ruleService.requiredNames(a);
        assertTrue(req.stream().anyMatch(n -> n.contains("个人/家庭隐私")), "H 材料应入清单");
        assertTrue(req.stream().anyMatch(n -> n.contains("第三方机构协议")), "J 材料应入清单");
        assertTrue(req.stream().anyMatch(n -> n.contains("表2")), "涉三方应含表2");
        assertFalse(req.stream().anyMatch(n -> n.contains("行政监管")), "未选 G 不应入清单");
    }
}
