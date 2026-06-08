package com.csg.prm.ledger.service;

import com.csg.prm.ledger.dto.LedgerStatisticsVO;
import com.csg.prm.ledger.entity.PropertyArchive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 产权台账统计分析(F-01-001-001-007)集成测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerStatisticsTest {

    @Autowired
    private PropertyArchiveService archiveService;
    @Autowired
    private LedgerStatisticsService statisticsService;

    @Test
    void statistics_should_aggregate_by_dimensions() {
        PropertyArchive a = new PropertyArchive();
        a.setAssetId("DA-STAT-001");
        a.setAssetName("统计测试表");
        a.setRightType("数据产品经营权");
        a.setConfirmStatus("已确权");
        archiveService.create(a);

        LedgerStatisticsVO vo = statisticsService.statistics();
        assertTrue(vo.getTotalArchive() >= 1);
        assertNotNull(vo.getByRightType());
        assertTrue(vo.getByRightType().containsKey("数据产品经营权"));
        assertTrue(vo.getByConfirmStatus().containsKey("已确权"));
        assertNotNull(vo.getBySubsidiary());
    }
}
