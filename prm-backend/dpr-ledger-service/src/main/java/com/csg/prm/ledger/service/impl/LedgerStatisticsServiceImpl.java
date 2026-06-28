package com.csg.prm.ledger.service.impl;

import com.csg.prm.common.org.DeploymentUnits;
import com.csg.prm.common.org.Jurisdiction;
import com.csg.prm.common.org.OrgService;
import com.csg.prm.ledger.dto.LedgerStatisticsVO;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.DataAssetInfoMapper;
import com.csg.prm.ledger.mapper.PropertyArchiveMapper;
import com.csg.prm.ledger.service.LedgerStatisticsService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LedgerStatisticsServiceImpl implements LedgerStatisticsService {

    private static final String UNKNOWN = "未分类";
    /** 组织主数据解析不到省/地市归属时的占位(随平台同步/重确权回填编码后自然消除)。 */
    private static final String UNIDENTIFIED = "未标识";
    /** 确权覆盖率口径:确权状态为此值计入"已确权"。 */
    private static final String CONFIRMED = "已确权";
    private static final int TREND_MONTHS = 15;

    private final PropertyArchiveMapper archiveMapper;
    private final DataAssetInfoMapper assetMapper;
    private final OrgService orgService;

    public LedgerStatisticsServiceImpl(PropertyArchiveMapper archiveMapper, DataAssetInfoMapper assetMapper,
                                       OrgService orgService) {
        this.archiveMapper = archiveMapper;
        this.assetMapper = assetMapper;
        this.orgService = orgService;
    }

    @Override
    public LedgerStatisticsVO statistics() {
        List<PropertyArchive> archives = archiveMapper.selectList(null);
        List<DataAssetInfo> assets = assetMapper.selectList(null);

        Map<String, Long> byRightType = archives.stream()
                .collect(Collectors.groupingBy(a -> orUnknown(a.getRightType()), Collectors.counting()));
        Map<String, Long> byConfirmStatus = archives.stream()
                .collect(Collectors.groupingBy(a -> orUnknown(a.getConfirmStatus()), Collectors.counting()));
        Map<String, Long> byAuthStatus = archives.stream()
                .collect(Collectors.groupingBy(a -> orUnknown(a.getAuthStatus()), Collectors.counting()));
        Map<String, Long> bySubsidiary = assets.stream()
                .collect(Collectors.groupingBy(a -> orUnknown(a.getSubsidiaryName()), Collectors.counting()));

        // 系统部署单位(打√口径):固定 10 桶恒显(零填充),据资产现有省/地市码或归属名派生归类;不可归类计入「未标识」(仅>0时附加)
        Map<String, Long> byDeploymentUnit = new LinkedHashMap<>();
        for (String unit : DeploymentUnits.ORDER) {
            byDeploymentUnit.put(unit, 0L);
        }
        for (DataAssetInfo a : assets) {
            String unit = DeploymentUnits.classify(a.getProvinceCode(), a.getBureauCode(), a.getSubsidiaryName());
            byDeploymentUnit.merge(unit, 1L, Long::sum);
        }
        byDeploymentUnit.remove(DeploymentUnits.UNIDENTIFIED, 0L); // 无未标识资产则不显示该桶

        // 不按地理省域/地市统计:南网习惯按"系统部署单位"切分(byDeploymentUnit),已移除省域/地市/各省确权率/各省授权状态维度。

        LedgerStatisticsVO vo = new LedgerStatisticsVO();
        vo.setTotalArchive(archives.size());
        vo.setByRightType(byRightType);
        vo.setByConfirmStatus(byConfirmStatus);
        vo.setByAuthStatus(byAuthStatus);
        vo.setBySubsidiary(bySubsidiary);
        vo.setByDeploymentUnit(byDeploymentUnit);
        vo.setTrend(buildTrend(archives));
        return vo;
    }

    /** 按月统计新增档案,生成近 {@value #TREND_MONTHS} 个月的环比/同比趋势。 */
    private List<LedgerStatisticsVO.TrendPoint> buildTrend(List<PropertyArchive> archives) {
        Map<String, Long> byMonth = archives.stream()
                .filter(a -> a.getCreateTime() != null)
                .collect(Collectors.groupingBy(a -> YearMonth.from(a.getCreateTime()).toString(), Collectors.counting()));
        YearMonth now = YearMonth.now();
        List<LedgerStatisticsVO.TrendPoint> trend = new ArrayList<>();
        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth m = now.minusMonths(i);
            long count = byMonth.getOrDefault(m.toString(), 0L);
            Long prev = byMonth.get(m.minusMonths(1).toString());
            Long lastYear = byMonth.get(m.minusMonths(12).toString());
            Double mom = (prev != null && prev > 0) ? round1((count - prev) * 100.0 / prev) : null;
            Double yoy = (lastYear != null && lastYear > 0) ? round1((count - lastYear) * 100.0 / lastYear) : null;
            trend.add(new LedgerStatisticsVO.TrendPoint(m.toString(), count, mom, yoy));
        }
        return trend;
    }

    private Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String orUnknown(String v) {
        return StringUtils.hasText(v) ? v : UNKNOWN;
    }
}
