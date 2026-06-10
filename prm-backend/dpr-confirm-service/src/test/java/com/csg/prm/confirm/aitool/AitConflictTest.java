package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.dto.KgGraphVO;
import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.service.AitConflictService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 智能确权辅助工具 M2 权属冲突识别测试:知识图谱 + 主体/范围/时效/历史四类冲突检测 + 报告。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitConflictTest {

    @Autowired
    private AitConflictService conflictService;
    @Autowired
    private AitMaterialService materialService;
    @Autowired
    private ConfirmApplyService applyService;

    /** #9 条款语义分析自动建主张 + 知识图谱结构化输出(节点+关系)。 */
    @Test
    void buildClaimFromMaterial_semantic_and_graph() {
        ConfirmApply apply = new ConfirmApply();
        apply.setAssetId("DA-KG-1");
        apply.setAssetName("线路资产数据");
        apply.setRightType("数据持有权");
        apply.setRightHolder("广东电网");
        apply.setValidDate(LocalDateTime.now().plusYears(3));
        String applyId = applyService.saveDraft(apply);

        AitMaterial m = new AitMaterial();
        m.setFileName("授权证明.pdf");
        m.setApplyId(applyId);
        m.setContent("数据持有权,授权范围约定字段,已加盖公章,有效期3年");
        String mid = materialService.upload(m);
        materialService.parse(mid);

        // 条款语义分析 → 自动建"证明材料"主张
        String claimId = conflictService.buildClaimFromMaterial(mid);
        assertNotNull(claimId);
        assertTrue(conflictService.claims("DA-KG-1").stream()
                .anyMatch(c -> AitKgClaim.SRC_MATERIAL.equals(c.getSourceType())), "应有证明材料来源的主张");

        // 知识图谱:含 客体/主体/授权事项 节点 + 授权/归属 关系
        KgGraphVO g = conflictService.graph("DA-KG-1");
        assertTrue(g.getNodes().stream().anyMatch(n -> "客体".equals(n.type())), "应有客体节点");
        assertTrue(g.getNodes().stream().anyMatch(n -> "主体".equals(n.type())), "应有主体节点");
        assertTrue(g.getNodes().stream().anyMatch(n -> "授权事项".equals(n.type())), "应有授权事项节点");
        assertTrue(g.getEdges().stream().anyMatch(e -> "授权".equals(e.relation())), "应有授权关系");
        assertTrue(g.getEdges().stream().anyMatch(e -> "归属".equals(e.relation())), "应有归属关系");
    }

    @Autowired
    private com.csg.prm.confirm.mapper.EquityCardMapper cardMapper;

    /** #10 动态更新:人工修改/删除节点 + 历史案例自动同步(从权益卡片,去重幂等)。 */
    @Test
    void claim_update_delete_and_history_sync() {
        // 人工修改节点:add → updateClaim(改授权范围)
        String cid = conflictService.addClaim(claim("DA-UPD-1", "广东电网", "数据持有权", "全字段", null, false, "当前申请"));
        AitKgClaim upd = new AitKgClaim();
        upd.setClaimId(cid);
        upd.setAuthScope("约定字段");
        conflictService.updateClaim(upd);
        assertEquals("约定字段", conflictService.claims("DA-UPD-1").get(0).getAuthScope(), "修改应生效");
        // 人工删除节点
        conflictService.deleteClaim(cid);
        assertTrue(conflictService.claims("DA-UPD-1").isEmpty(), "删除后应无主张");

        // 历史案例自动同步:插权益卡片 → sync → 历史确权主张
        com.csg.prm.confirm.entity.EquityCard card = new com.csg.prm.confirm.entity.EquityCard();
        card.setAssetId("DA-HIST-1");
        card.setCardNo("EC-HIST-1");
        card.setRightType("数据加工使用权");
        card.setRightOwner("南网科研院");
        card.setValidDate(LocalDateTime.now().plusYears(2));
        cardMapper.insert(card);
        assertEquals(1, conflictService.syncHistoryClaims("DA-HIST-1"), "应从权益卡片同步出 1 条历史确权主张");
        assertEquals(0, conflictService.syncHistoryClaims("DA-HIST-1"), "去重:重复同步应 0 条");
        assertTrue(conflictService.claims("DA-HIST-1").stream()
                .anyMatch(c -> AitKgClaim.SRC_HISTORY.equals(c.getSourceType())), "应生成历史确权主张");
    }

    /** #11 主体冲突:同一客体被多主体声明数据持有权(非经营、非排他)→ 应判主体冲突,描述含客体+双方主体。 */
    @Test
    void subjectConflict_for_multi_holder_holding_right() {
        String asset = "DA-HOLD-1";
        conflictService.addClaim(claim(asset, "广东电网", "数据持有权", "全字段", null, false, "历史确权"));
        AitKgClaim cur = claim(asset, "南网科研院", "数据持有权", "全字段", null, false, "当前申请");
        List<AitConflict> found = conflictService.detect(cur);

        assertTrue(found.stream().anyMatch(c -> AitConflict.TYPE_SUBJECT.equals(c.getConflictType())),
                "同一客体多主体持有权应判主体冲突");
        AitConflict sc = found.stream().filter(c -> AitConflict.TYPE_SUBJECT.equals(c.getConflictType()))
                .findFirst().orElseThrow();
        assertTrue(sc.getConflictDesc().contains(asset), "冲突描述应含客体,实际:" + sc.getConflictDesc());
        assertTrue(sc.getConflictDesc().contains("广东电网") && sc.getConflictDesc().contains("南网科研院"),
                "应含双方冲突主体,实际:" + sc.getConflictDesc());
    }

    /** #12 范围冲突:对比当前范围与历史排他范围,算出具体重叠字段(交集),非整段范围。 */
    @Test
    void scopeConflict_identifies_specific_overlap_fields() {
        String asset = "DA-SCOPE-1";
        // 历史排他授权:字段 用电量、电压、负荷
        conflictService.addClaim(claim(asset, "广东电网", "数据加工使用权", "用电量、电压、负荷", null, true, "历史确权"));
        // 当前申请:用电量、负荷、地址 → 与历史重叠 用电量、负荷(不含电压/地址)
        AitKgClaim cur = claim(asset, "南网科研院", "数据加工使用权", "用电量、负荷、地址", null, false, "当前申请");
        AitConflict sc = conflictService.detect(cur).stream()
                .filter(c -> AitConflict.TYPE_SCOPE.equals(c.getConflictType())).findFirst().orElseThrow();

        assertTrue(sc.getConflictDesc().contains("部分重叠"), "应判部分重叠,实际:" + sc.getConflictDesc());
        assertTrue(sc.getImpactScope().contains("用电量") && sc.getImpactScope().contains("负荷"),
                "影响范围应含具体重叠字段,实际:" + sc.getImpactScope());
        assertFalse(sc.getImpactScope().contains("电压"), "仅历史有的字段不应在重叠区域:" + sc.getImpactScope());
        assertFalse(sc.getImpactScope().contains("地址"), "仅当前有的字段不应在重叠区域:" + sc.getImpactScope());
    }

    /** #13 时效冲突:授权有效期超出数据生命周期 → 算出超期天数与超期区间[X~Y]。 */
    @Test
    void validityConflict_computes_overrun_range_and_days() {
        String asset = "DA-VALID-1";
        LocalDateTime lifeEnd = LocalDateTime.of(2027, 1, 1, 0, 0); // 数据生命周期到期
        conflictService.addClaim(claim(asset, "广东电网", "数据持有权", "全字段", lifeEnd, false, "历史确权"));
        // 当前授权:至 2027-02-01(超 31 天)
        AitKgClaim cur = claim(asset, "广东电网", "数据加工使用权", "全字段", lifeEnd.plusDays(31), false, "当前申请");
        AitConflict vc = conflictService.detect(cur).stream()
                .filter(c -> AitConflict.TYPE_VALIDITY.equals(c.getConflictType())).findFirst().orElseThrow();

        assertTrue(vc.getConflictDesc().contains("31 天"), "应算出超期天数,实际:" + vc.getConflictDesc());
        assertTrue(vc.getImpactScope().contains("超 31 天"), "影响应含超期天数,实际:" + vc.getImpactScope());
        assertTrue(vc.getImpactScope().contains("2027-01-02") && vc.getImpactScope().contains("2027-02-01"),
                "影响应含超期区间[2027-01-02~2027-02-01],实际:" + vc.getImpactScope());
    }

    /** #14 历史记录比对:不同主体不同权利→归属矛盾(高,带历史明细);同主体不同权利→权利类型变更(中,非误报高)。 */
    @Test
    void historyConflict_distinguishes_contradiction_vs_change() {
        // (1) 归属矛盾:历史 广东电网 持有权(有效期2027) / 当前 深圳供电局 加工使用权
        String a1 = "DA-HIST-C1";
        conflictService.addClaim(claim(a1, "广东电网", "数据持有权", "全字段",
                LocalDateTime.of(2027, 1, 1, 0, 0), false, "历史确权"));
        AitKgClaim cur1 = claim(a1, "深圳供电局", "数据加工使用权", "全字段", null, false, "当前申请");
        AitConflict c1 = conflictService.detect(cur1).stream()
                .filter(c -> AitConflict.TYPE_HISTORY.equals(c.getConflictType())).findFirst().orElseThrow();
        assertEquals("高", c1.getRiskLevel(), "不同主体不同权利应为权属归属矛盾(高)");
        assertTrue(c1.getConflictDesc().contains("归属"), "应标归属矛盾:" + c1.getConflictDesc());
        assertTrue(c1.getConflictDesc().contains("有效期至2027"), "应带历史记录明细:" + c1.getConflictDesc());

        // (2) 权利类型变更:历史 广东电网 持有权 / 当前 广东电网 加工使用权(同主体)→ 中,非误报高
        String a2 = "DA-HIST-C2";
        conflictService.addClaim(claim(a2, "广东电网", "数据持有权", "全字段", null, false, "历史确权"));
        AitKgClaim cur2 = claim(a2, "广东电网", "数据加工使用权", "全字段", null, false, "当前申请");
        AitConflict c2 = conflictService.detect(cur2).stream()
                .filter(c -> AitConflict.TYPE_HISTORY.equals(c.getConflictType())).findFirst().orElseThrow();
        assertEquals("中", c2.getRiskLevel(), "同主体不同权利是变更(中),非误报矛盾(高)");
        assertTrue(c2.getConflictDesc().contains("变更"), "应标权利类型变更:" + c2.getConflictDesc());
    }

    /** #15 结构化报告:决策建议 + 来源/影响范围汇总 + 高风险摘要。 */
    @Test
    void report_has_decision_source_impact_and_highrisk_summary() {
        String asset = "DA-RPT-1";
        // 历史确权 广东电网 经营权 排他 + 当前 深圳供电局 经营权 → 主体冲突(高) + 范围冲突
        conflictService.addClaim(claim(asset, "广东电网", "数据产品经营权", "全字段", null, true, "历史确权"));
        AitKgClaim cur = claim(asset, "深圳供电局", "数据产品经营权", "全字段", null, false, "当前申请");
        conflictService.addClaim(cur);
        conflictService.detect(cur); // 产生并持久化冲突

        Map<String, Object> rpt = conflictService.report(asset, null, null, null, null);
        assertEquals("建议驳回/暂缓", rpt.get("decision"), "有高风险应建议驳回/暂缓");
        assertTrue(((Number) rpt.get("highRiskCount")).intValue() >= 1, "应有高风险冲突");
        assertFalse(((List<?>) rpt.get("highRiskSummary")).isEmpty(), "应有高风险摘要");
        assertFalse(((Map<?, ?>) rpt.get("bySource")).isEmpty(), "应有来源汇总");
        List<?> subjects = (List<?>) rpt.get("involvedSubjects");
        assertTrue(subjects.contains("广东电网") && subjects.contains("深圳供电局"), "影响范围应含涉及主体:" + subjects);
        assertEquals(asset, rpt.get("involvedObject"));
    }

    private AitKgClaim claim(String asset, String subject, String rt, String scope,
                             LocalDateTime valid, boolean exclusive, String source) {
        AitKgClaim c = new AitKgClaim();
        c.setAssetId(asset);
        c.setSubject(subject);
        c.setRightType(rt);
        c.setAuthScope(scope);
        c.setValidDate(valid);
        c.setExclusive(exclusive);
        c.setSourceType(source);
        return c;
    }

    @Test
    void detect_subject_scope_validity_history_conflicts() {
        String asset = "DA-CONF-1";
        // 历史确权:广东电网 经营权 全字段 排他 有效期至2027
        conflictService.addClaim(claim(asset, "广东电网", "数据产品经营权", "全字段",
                LocalDateTime.now().plusYears(1), true, AitKgClaim.SRC_HISTORY));

        // 当前申请:深圳供电局 经营权 全字段 有效期至2030(不同主体、范围重叠、时效更长)
        AitKgClaim cur = claim(asset, "深圳供电局", "数据产品经营权", "全字段",
                LocalDateTime.now().plusYears(4), false, AitKgClaim.SRC_CURRENT);
        conflictService.addClaim(cur);
        List<AitConflict> found = conflictService.detect(cur);

        Set<String> types = found.stream().map(AitConflict::getConflictType).collect(Collectors.toSet());
        assertTrue(types.contains(AitConflict.TYPE_SUBJECT), "应检出主体冲突");
        assertTrue(types.contains(AitConflict.TYPE_SCOPE), "应检出范围冲突");
        assertTrue(types.contains(AitConflict.TYPE_VALIDITY), "应检出时效冲突");
        // 同资产、不同权利类型的历史 -> 历史矛盾
    }

    @Test
    void detect_history_contradiction_on_right_type() {
        String asset = "DA-CONF-2";
        conflictService.addClaim(claim(asset, "广东电网", "所有权", "全字段",
                LocalDateTime.now().plusYears(2), false, AitKgClaim.SRC_HISTORY));
        AitKgClaim cur = claim(asset, "广东电网", "数据加工使用权", "约定字段",
                LocalDateTime.now().plusYears(1), false, AitKgClaim.SRC_CURRENT);
        conflictService.addClaim(cur);
        List<AitConflict> found = conflictService.detect(cur);
        assertTrue(found.stream().anyMatch(c -> AitConflict.TYPE_HISTORY.equals(c.getConflictType())),
                "历史确权所有权 vs 当前使用权应检出历史记录冲突");
    }

    @Test
    void no_conflict_when_clean() {
        String asset = "DA-CONF-3";
        AitKgClaim cur = claim(asset, "广东电网", "数据持有权", "约定字段",
                LocalDateTime.now().plusYears(1), false, AitKgClaim.SRC_CURRENT);
        conflictService.addClaim(cur);
        List<AitConflict> found = conflictService.detect(cur);
        assertTrue(found.isEmpty(), "无历史主张时不应有冲突");
        Map<String, Object> report = conflictService.report(asset);
        assertEquals(0, ((Number) report.get("total")).intValue());
    }
}
