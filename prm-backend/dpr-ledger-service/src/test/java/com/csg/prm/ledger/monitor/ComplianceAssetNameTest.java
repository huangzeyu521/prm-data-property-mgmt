package com.csg.prm.ledger.monitor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.monitor.entity.ComplianceResult;
import com.csg.prm.ledger.monitor.mapper.ComplianceResultMapper;
import com.csg.prm.ledger.monitor.service.ComplianceCheckService;
import com.csg.prm.ledger.service.PropertyArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 合规性检查结果「数据表(assetName)」契约锁(防回归)。
 *
 * 合规性检查页表列 所属系统(assetId 派生)+ 数据表(assetName)。巡检 record() 须把档案的 assetName
 * 写入 ComplianceResult,否则前端「数据表」列恒空。本测试锁:巡检命中的检查结果带回 assetName。
 */
@SpringBootTest
@ActiveProfiles("test")
class ComplianceAssetNameTest {

    @Autowired private PropertyArchiveService archiveService;
    @Autowired private ComplianceCheckService complianceService;
    @Autowired private ComplianceResultMapper complianceMapper;

    @Test
    @DisplayName("巡检命中的合规检查结果带回数据表名(assetName),供页表显示")
    void complianceResult_carriesAssetName() {
        String assetId = "CHK-NAME-" + System.nanoTime();
        PropertyArchive a = new PropertyArchive();
        a.setAssetId(assetId);
        a.setAssetName("合规巡检数据表");
        a.setConfirmStatus("已确权");
        a.setValidDate(LocalDateTime.now().minusDays(1)); // 已过期 → 有效期维命中
        archiveService.create(a);

        complianceService.runComplianceCheck();

        List<ComplianceResult> results = complianceMapper.selectList(
                new LambdaQueryWrapper<ComplianceResult>().eq(ComplianceResult::getAssetId, assetId));
        assertFalse(results.isEmpty(), "已过期档案应产出合规检查结果");
        assertEquals("合规巡检数据表", results.get(0).getAssetName(),
                "检查结果应带回档案的数据表名(assetName),供页表「数据表」列显示");
    }
}
