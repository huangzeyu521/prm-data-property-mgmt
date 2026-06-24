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
    void statistics_byProvinceBureau_resolvesViaOrgTree_fromSubsidiary() {
        // 无 province_code:按子公司名兜底解析组织树。
        // "广东电网"(省级简称)→省 广东电网有限责任公司;"深圳供电局"(地市)→省 广东 + 地市 深圳供电局。
        insertAsset("DA-STAT-GD", "统计-广东", "广东电网");
        insertAsset("DA-STAT-SZ", "统计-深圳", "深圳供电局");

        LedgerStatisticsVO vo = statisticsService.statistics();

        assertNotNull(vo.getByProvince());
        // 两条资产经组织主数据均上溯到广东省
        assertTrue(vo.getByProvince().getOrDefault("广东电网有限责任公司", 0L) >= 2,
                "广东电网 + 深圳供电局 均应归入广东省");
        // 真下钻:byBureau 按省嵌套,广东省下含 深圳供电局 与 未标识(省级资产无地市)
        assertNotNull(vo.getByBureau());
        Map<String, Long> gdBureaus = vo.getByBureau().get("广东电网有限责任公司");
        assertNotNull(gdBureaus, "广东省应有地市下钻子集");
        assertTrue(gdBureaus.containsKey("深圳供电局"), "广东省下应含深圳供电局");
        assertTrue(gdBureaus.containsKey("未标识"), "省级资产无地市,应落未标识");
    }

    @Test
    void statistics_provinceConfirmRate_groupsArchivesByProvince() {
        // 档案省域兜底用权属主体(rightSubject);确权率按省分组算 已确权/总数。
        PropertyArchive ar = new PropertyArchive();
        ar.setAssetId("DA-PC-GD");
        ar.setAssetName("确权率-广东");
        ar.setRightSubject("广东电网");
        ar.setConfirmStatus("已确权");
        archiveService.create(ar);

        LedgerStatisticsVO vo = statisticsService.statistics();
        assertNotNull(vo.getProvinceConfirm());
        LedgerStatisticsVO.ProvinceConfirm gd = vo.getProvinceConfirm().stream()
                .filter(p -> "广东电网有限责任公司".equals(p.getProvince())).findFirst().orElse(null);
        assertNotNull(gd, "应有广东省确权率条目");
        assertTrue(gd.getConfirmed() >= 1 && gd.getTotal() >= 1, "广东省应有已确权与总数计数");
        assertTrue(gd.getRate() != null && gd.getRate() > 0, "确权率应 > 0");
    }

    private void insertAsset(String id, String name, String subsidiary) {
        DataAssetInfo a = new DataAssetInfo();
        a.setAssetId(id);
        a.setAssetName(name);
        a.setSubsidiaryName(subsidiary);
        assetMapper.insert(a);
    }
}
