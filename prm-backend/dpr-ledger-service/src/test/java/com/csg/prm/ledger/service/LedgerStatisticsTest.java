package com.csg.prm.ledger.service;

import com.csg.prm.ledger.dto.LedgerStatisticsVO;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.DataAssetInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

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
        assertNotNull(vo.getBySubsidiary());
    }

    @Test
    void statistics_byDeploymentUnit_tenBucketsAlwaysShown_andClassified() {
        // 深圳供电局→深圳桶(单列,不并入广东);广西电网→广西桶
        insertAsset("DA-DU-SZ", "部署-深圳", "深圳供电局");
        insertAsset("DA-DU-GX", "部署-广西", "广西电网");

        LedgerStatisticsVO vo = statisticsService.statistics();
        Map<String, Long> du = vo.getByDeploymentUnit();
        assertNotNull(du, "应有系统部署单位维度");
        // 10 桶恒显(零填充):总部/超高压/双调/五省网/广州/深圳
        for (String unit : com.csg.prm.common.org.DeploymentUnits.ORDER) {
            assertTrue(du.containsKey(unit), "部署单位应恒显: " + unit);
        }
        assertTrue(du.get("深圳") >= 1, "深圳供电局应计入深圳桶");
        assertTrue(du.get("广西") >= 1, "广西电网应计入广西桶");
    }

    private void insertAsset(String id, String name, String subsidiary) {
        DataAssetInfo a = new DataAssetInfo();
        a.setAssetId(id);
        a.setAssetName(name);
        a.setSubsidiaryName(subsidiary);
        assetMapper.insert(a);
    }
}
