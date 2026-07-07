package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitConflictRule;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.service.AitConflictRuleService;
import com.csg.prm.confirm.aitool.service.AitConflictService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权属冲突识别与分析(#1~#6)能力测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitConflictAnalysisTest {

    @Autowired private AitConflictService conflictService;
    @Autowired private AitConflictRuleService ruleService;

    private AitKgClaim claim(String asset, String subject, String right, boolean exclusive, String src) {
        AitKgClaim c = new AitKgClaim();
        c.setAssetId(asset);
        c.setSubject(subject);
        c.setRightType(right);
        c.setExclusive(exclusive);
        c.setSourceType(src);
        return c;
    }

    /** #1 规则配置:停用主体冲突规则后,不再识别主体冲突;恢复后再识别。 */
    @Test
    void rule_config_enable_disable() {
        List<AitConflictRule> rules = ruleService.list();
        assertTrue(rules.size() >= 5, "应种入五类冲突规则");
        String subjRuleId = rules.stream().filter(r -> AitConflict.TYPE_SUBJECT.equals(r.getRuleType()))
                .findFirst().orElseThrow().getRuleId();

        String asset = "DA-RULE-" + System.nanoTime();
        conflictService.addClaim(claim(asset, "广东电网", "持有权", false, AitKgClaim.SRC_MATERIAL));

        ruleService.toggle(subjRuleId, false);
        try {
            List<AitConflict> off = conflictService.detect(claim(asset, "深圳供电局", "持有权", false, AitKgClaim.SRC_CURRENT));
            assertTrue(off.stream().noneMatch(c -> AitConflict.TYPE_SUBJECT.equals(c.getConflictType())),
                    "停用后不应识别主体冲突");
        } finally {
            ruleService.toggle(subjRuleId, true); // 恢复,避免影响其他用例
        }
        List<AitConflict> on = conflictService.detect(claim(asset, "南网科研院", "持有权", false, AitKgClaim.SRC_CURRENT));
        assertTrue(on.stream().anyMatch(c -> AitConflict.TYPE_SUBJECT.equals(c.getConflictType())),
                "恢复后应识别主体冲突");
    }

    /** #2 类型冲突:同一数据集多种权利主张并存。#3 结构化追溯标注。 */
    @Test
    void type_conflict_and_structured_trace() {
        String asset = "DA-TYPE-" + System.nanoTime();
        conflictService.addClaim(claim(asset, "广东电网", "持有权", false, AitKgClaim.SRC_MATERIAL));
        List<AitConflict> found = conflictService.detect(claim(asset, "广东电网", "使用权", false, AitKgClaim.SRC_CURRENT));

        AitConflict tc = found.stream().filter(c -> AitConflict.TYPE_RIGHTTYPE.equals(c.getConflictType()))
                .findFirst().orElseThrow();
        // #3 结构化追溯
        assertNotNull(tc.getConflictFields(), "应标注冲突字段");
        assertNotNull(tc.getClauseRef(), "应标注条款依据");
        assertNotNull(tc.getRelatedRecordNo(), "应追溯关联记录编号");
        assertNotNull(tc.getLegalRisk(), "应给出法律风险等级");
    }

    /** #4 影响范围分析 + #5 详细对比表(当前 vs 历史)。 */
    @Test
    void report_impact_analysis_and_comparison() {
        String asset = "DA-IMPACT-" + System.nanoTime();
        AitKgClaim hist = claim(asset, "广东电网", "持有权", false, AitKgClaim.SRC_HISTORY);
        hist.setValidDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        conflictService.addClaim(hist);
        AitKgClaim cur = claim(asset, "深圳供电局", "使用权", false, AitKgClaim.SRC_CURRENT);
        conflictService.addClaim(cur);
        conflictService.detect(cur);

        Map<String, Object> rpt = conflictService.report(asset);
        Map<?, ?> impact = (Map<?, ?>) rpt.get("impactAnalysis");
        assertNotNull(impact, "应有影响范围分析");
        assertNotNull(impact.get("legalRiskLevel"), "应有法律风险等级");
        assertFalse(((List<?>) impact.get("depts")).isEmpty(), "应有涉及主体/部门");
        assertFalse(((List<?>) rpt.get("comparisonTable")).isEmpty(), "应有当前vs历史详细对比表");
    }

    /** #5 Word 报告导出 + #6 Excel 记录导出 + 状态筛选。 */
    @Test
    void report_word_and_records_excel_export() {
        String asset = "DA-EXP-" + System.nanoTime();
        conflictService.addClaim(claim(asset, "广东电网", "持有权", false, AitKgClaim.SRC_MATERIAL));
        conflictService.detect(claim(asset, "深圳供电局", "持有权", false, AitKgClaim.SRC_CURRENT));

        byte[] word = conflictService.exportReportWord(asset, null, null, null, null, null);
        assertTrue(word.length > 0, "Word 报告应可导出");
        byte[] excel = conflictService.exportConflictExcel(asset, null, null, null, null, null, null, null);
        assertTrue(excel.length > 0, "Excel 记录应可导出");

        // #6 状态筛选(待处置)
        List<AitConflict> open = conflictService.conflicts(asset, null, null, null, null, null,
                AitConflict.STATUS_OPEN, null);
        assertFalse(open.isEmpty(), "应能按状态筛选到待处置冲突");
    }
}
