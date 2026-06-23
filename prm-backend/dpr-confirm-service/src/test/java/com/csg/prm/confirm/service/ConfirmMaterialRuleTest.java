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

    /** 确权变更:应交材料按变更触发类型收敛为差异项(核心表单始终留;来源/关联仅留相关维度)。 */
    @Test
    void change_registration_narrows_to_trigger_dimension() {
        ConfirmApply a = new ConfirmApply();
        a.setSourceIdentification("A");        // A 来源
        a.setRelationIdentification("G");      // G 关联
        a.setRegisterType("确权变更");

        // 数据来源变更 → 保留来源(A),不留关联(G);核心表单始终保留
        a.setChangeTrigger("数据来源变更");
        List<String> src = ruleService.requiredNames(a);
        assertTrue(src.stream().anyMatch(n -> n.contains("表1")), "核心表单(表1)始终保留");
        assertTrue(src.stream().anyMatch(n -> n.contains("来源设备")), "来源变更应保留 A 来源材料");
        assertFalse(src.stream().anyMatch(n -> n.contains("行政监管")), "来源变更不应含 G 关联材料");

        // 管理要求变更 → 保留关联(G),不留来源(A)
        a.setChangeTrigger("管理要求变更");
        List<String> rel = ruleService.requiredNames(a);
        assertTrue(rel.stream().anyMatch(n -> n.contains("行政监管")), "管理要求变更应保留 G 关联材料");
        assertFalse(rel.stream().anyMatch(n -> n.contains("来源设备")), "管理要求变更不应含 A 来源材料");

        // 初始确权(对照):不收敛,来源+关联都在
        a.setRegisterType("初始确权");
        a.setChangeTrigger(null);
        List<String> init = ruleService.requiredNames(a);
        assertTrue(init.stream().anyMatch(n -> n.contains("来源设备"))
                && init.stream().anyMatch(n -> n.contains("行政监管")), "初始确权不收敛,A+G 均在");
    }

    /** 确权变更·组合/未知触发不可过度收敛:组合触发须两维都留,未知触发须不收敛(全留),杜绝漏要材料。 */
    @Test
    void change_combined_or_unknown_trigger_does_not_over_narrow() {
        ConfirmApply a = new ConfirmApply();
        a.setSourceIdentification("A");
        a.setRelationIdentification("G");
        a.setRegisterType("确权变更");

        // 附录F §3.3.2 组合表述:"数据来源变更和管理要求变更" → 来源(A)+关联(G) 都应保留
        a.setChangeTrigger("数据来源变更和管理要求变更");
        List<String> both = ruleService.requiredNames(a);
        assertTrue(both.stream().anyMatch(n -> n.contains("来源设备")), "组合触发应保留 A 来源");
        assertTrue(both.stream().anyMatch(n -> n.contains("行政监管")), "组合触发应保留 G 关联");

        // 未识别触发 → 保守不收敛(全留),不得漏掉来源/关联材料
        a.setChangeTrigger("某种未列举的变更原因XYZ");
        List<String> unknown = ruleService.requiredNames(a);
        assertTrue(unknown.stream().anyMatch(n -> n.contains("来源设备"))
                && unknown.stream().anyMatch(n -> n.contains("行政监管")), "未知触发应不收敛,A+G 均在");
    }
}
