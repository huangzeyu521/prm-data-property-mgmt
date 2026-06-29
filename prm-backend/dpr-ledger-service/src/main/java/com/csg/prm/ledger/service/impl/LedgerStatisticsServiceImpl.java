package com.csg.prm.ledger.service.impl;

import com.csg.prm.common.org.DeploymentUnits;
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
    /** 确权覆盖率口径:确权状态为此值计入"已确权"。 */
    private static final String CONFIRMED = "已确权";
    /** 授权率口径:授权状态为此值计入"已授权"。 */
    private static final String AUTHORIZED = "已授权";
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
        // 产权进度口径(非资产库存):某资产「已确权/已授权」= 其确权档案 confirmStatus/authStatus 命中。
        java.util.Set<String> confirmedIds = archives.stream()
                .filter(a -> CONFIRMED.equals(a.getConfirmStatus()))
                .map(PropertyArchive::getAssetId).collect(Collectors.toSet());
        java.util.Set<String> authorizedIds = archives.stream()
                .filter(a -> AUTHORIZED.equals(a.getAuthStatus()))
                .map(PropertyArchive::getAssetId).collect(Collectors.toSet());

        // 各系统部署单位 确权覆盖率/授权率(打√口径固定 10 桶恒显,零填充)
        Map<String, long[]> duAgg = new LinkedHashMap<>(); // 单位 -> [总数, 已确权, 已授权]
        for (String unit : DeploymentUnits.ORDER) {
            duAgg.put(unit, new long[3]);
        }
        for (DataAssetInfo a : assets) {
            String unit = DeploymentUnits.classify(a.getProvinceCode(), a.getBureauCode(), a.getSubsidiaryName());
            accCoverage(duAgg, unit, a.getAssetId(), confirmedIds, authorizedIds);
        }
        // 各子公司 确权覆盖率/授权率
        Map<String, long[]> subAgg = new LinkedHashMap<>();
        for (DataAssetInfo a : assets) {
            accCoverage(subAgg, orUnknown(a.getSubsidiaryName()), a.getAssetId(), confirmedIds, authorizedIds);
        }

        LedgerStatisticsVO vo = new LedgerStatisticsVO();
        vo.setTotalArchive(archives.size());
        vo.setByRightType(byRightType);
        vo.setByConfirmStatus(byConfirmStatus);
        vo.setByAuthStatus(byAuthStatus);
        vo.setCoverageByDeploymentUnit(toCoverageList(duAgg, true));
        vo.setCoverageBySubsidiary(toCoverageList(subAgg, false));
        vo.setTrend(buildTrend(archives));
        return vo;
    }

    /** 累计某维度某桶:总数 + 已确权 + 已授权。 */
    private void accCoverage(Map<String, long[]> agg, String key, String assetId,
                             java.util.Set<String> confirmedIds, java.util.Set<String> authorizedIds) {
        long[] c = agg.computeIfAbsent(key, k -> new long[3]);
        c[0]++;
        if (confirmedIds.contains(assetId)) {
            c[1]++;
        }
        if (authorizedIds.contains(assetId)) {
            c[2]++;
        }
    }

    /** 聚合 → 覆盖率列表(率=已确权/总、已授权/总);dropZeroUnidentified=true 时丢弃总数为0的「未标识」桶。 */
    private List<LedgerStatisticsVO.Coverage> toCoverageList(Map<String, long[]> agg, boolean dropZeroUnidentified) {
        List<LedgerStatisticsVO.Coverage> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : agg.entrySet()) {
            long total = e.getValue()[0];
            if (dropZeroUnidentified && total == 0 && DeploymentUnits.UNIDENTIFIED.equals(e.getKey())) {
                continue;
            }
            long confirmed = e.getValue()[1];
            long authorized = e.getValue()[2];
            Double cr = total > 0 ? round1(confirmed * 100.0 / total) : 0.0;
            Double ar = total > 0 ? round1(authorized * 100.0 / total) : 0.0;
            out.add(new LedgerStatisticsVO.Coverage(e.getKey(), total, confirmed, authorized, cr, ar));
        }
        return out;
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
