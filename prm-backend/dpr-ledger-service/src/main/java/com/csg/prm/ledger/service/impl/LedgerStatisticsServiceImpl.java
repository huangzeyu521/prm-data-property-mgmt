package com.csg.prm.ledger.service.impl;

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LedgerStatisticsServiceImpl implements LedgerStatisticsService {

    private static final String UNKNOWN = "未分类";
    private static final int TREND_MONTHS = 15;

    private final PropertyArchiveMapper archiveMapper;
    private final DataAssetInfoMapper assetMapper;

    public LedgerStatisticsServiceImpl(PropertyArchiveMapper archiveMapper, DataAssetInfoMapper assetMapper) {
        this.archiveMapper = archiveMapper;
        this.assetMapper = assetMapper;
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
        Map<String, Long> byRegion = assets.stream()
                .collect(Collectors.groupingBy(a -> regionOf(a.getSubsidiaryName()), Collectors.counting()));

        LedgerStatisticsVO vo = new LedgerStatisticsVO();
        vo.setTotalArchive(archives.size());
        vo.setByRightType(byRightType);
        vo.setByConfirmStatus(byConfirmStatus);
        vo.setByAuthStatus(byAuthStatus);
        vo.setBySubsidiary(bySubsidiary);
        vo.setByRegion(byRegion);
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

    /** 由南网省级子公司归属推导地域(区域),作为独立于"子公司"的粗粒度维度。 */
    private String regionOf(String subsidiary) {
        if (!StringUtils.hasText(subsidiary)) {
            return UNKNOWN;
        }
        if (subsidiary.contains("广东") || subsidiary.contains("深圳") || subsidiary.contains("广西") || subsidiary.contains("海南")) {
            return "华南区";
        }
        if (subsidiary.contains("云南") || subsidiary.contains("贵州")) {
            return "西南区";
        }
        return "总部及其他";
    }

    private Double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private String orUnknown(String v) {
        return StringUtils.hasText(v) ? v : UNKNOWN;
    }
}
