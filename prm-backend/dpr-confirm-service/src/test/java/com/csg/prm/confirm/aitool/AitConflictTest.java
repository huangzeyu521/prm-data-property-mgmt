package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.service.AitConflictService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 智能确权辅助工具 M2 权属冲突识别测试:知识图谱 + 主体/范围/时效/历史四类冲突检测 + 报告。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitConflictTest {

    @Autowired
    private AitConflictService conflictService;

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
