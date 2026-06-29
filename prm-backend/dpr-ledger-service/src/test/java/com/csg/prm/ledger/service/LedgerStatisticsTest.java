package com.csg.prm.ledger.service;

import com.csg.prm.ledger.dto.LedgerStatisticsVO;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.DataAssetInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @Autowired
    private DataAssetInfoMapper assetMapper;

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
        assertNotNull(vo.getCoverageBySubsidiary());
    }

    @Test
    void statistics_byDeploymentUnit_tenBucketsAlwaysShown_andClassified() {
        // 深圳供电局→深圳桶(单列,不并入广东);广西电网→广西桶
        insertAsset("DA-DU-SZ", "部署-深圳", "深圳供电局");
        insertAsset("DA-DU-GX", "部署-广西", "广西电网");

        LedgerStatisticsVO vo = statisticsService.statistics();
        List<LedgerStatisticsVO.Coverage> du = vo.getCoverageByDeploymentUnit();
        assertNotNull(du, "应有系统部署单位维度(覆盖率口径)");
        Map<String, Long> totals = du.stream().collect(Collectors.toMap(
                LedgerStatisticsVO.Coverage::getName, LedgerStatisticsVO.Coverage::getTotal, (a, b) -> a));
        // 10 桶恒显(零填充):总部/超高压/双调/五省网/广州/深圳
        for (String unit : com.csg.prm.common.org.DeploymentUnits.ORDER) {
            assertTrue(totals.containsKey(unit), "部署单位应恒显: " + unit);
        }
        assertTrue(totals.get("深圳") >= 1, "深圳供电局应计入深圳桶");
        assertTrue(totals.get("广西") >= 1, "广西电网应计入广西桶");
    }

    @Test
    void statistics_coverage_shouldComputeRates() {
        // 同一子公司 2 资产,其中 1 资产已确权 → 确权覆盖率 = 50%
        insertAsset("DA-COV-1", "覆盖-1", "覆盖率测试公司");
        insertAsset("DA-COV-2", "覆盖-2", "覆盖率测试公司");
        PropertyArchive arc = new PropertyArchive();
        arc.setAssetId("DA-COV-1");
        arc.setAssetName("覆盖-1");
        arc.setConfirmStatus("已确权");
        archiveService.create(arc);

        LedgerStatisticsVO vo = statisticsService.statistics();
        LedgerStatisticsVO.Coverage cov = vo.getCoverageBySubsidiary().stream()
                .filter(c -> "覆盖率测试公司".equals(c.getName())).findFirst().orElse(null);
        assertNotNull(cov, "应统计到覆盖率测试公司");
        assertEquals(2, cov.getTotal());
        assertEquals(1, cov.getConfirmed());
        assertEquals(50.0, cov.getConfirmRate());
    }

    private void insertAsset(String id, String name, String subsidiary) {
        DataAssetInfo a = new DataAssetInfo();
        a.setAssetId(id);
        a.setAssetName(name);
        a.setSubsidiaryName(subsidiary);
        assetMapper.insert(a);
    }
}
