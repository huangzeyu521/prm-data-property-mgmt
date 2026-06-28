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

        // 省域计数 + 省→地市嵌套(真下钻):优先已存编码译名,无码按子公司名兜底,仍不中落「未标识」。
        Map<String, Long> byProvince = new LinkedHashMap<>();
        Map<String, Map<String, Long>> byBureau = new LinkedHashMap<>();
        for (DataAssetInfo a : assets) {
            Jurisdiction jur = jurisdictionOf(a.getProvinceCode(), a.getBureauCode(), a.getSubsidiaryName());
            String province = provinceLabel(jur);
            byProvince.merge(province, 1L, Long::sum);
            byBureau.computeIfAbsent(province, k -> new LinkedHashMap<>())
                    .merge(bureauLabel(jur), 1L, Long::sum);
        }

        // 跨维对比:按省分组档案 → 各省确权覆盖率 + 各省授权状态结构。档案省域兜底用权属主体(rightSubject)。
        Map<String, long[]> confirmAgg = new LinkedHashMap<>(); // 省 -> [总数, 已确权数]
        Map<String, Map<String, Long>> provinceAuthStatus = new LinkedHashMap<>();
        for (PropertyArchive ar : archives) {
            Jurisdiction jur = jurisdictionOf(ar.getProvinceCode(), ar.getBureauCode(), ar.getRightSubject());
            String province = provinceLabel(jur);
            long[] agg = confirmAgg.computeIfAbsent(province, k -> new long[2]);
            agg[0]++;
            if (CONFIRMED.equals(ar.getConfirmStatus())) {
                agg[1]++;
            }
            provinceAuthStatus.computeIfAbsent(province, k -> new LinkedHashMap<>())
                    .merge(orUnknown(ar.getAuthStatus()), 1L, Long::sum);
        }
        List<LedgerStatisticsVO.ProvinceConfirm> provinceConfirm = new ArrayList<>();
        for (Map.Entry<String, long[]> e : confirmAgg.entrySet()) {
            long total = e.getValue()[0];
            long confirmed = e.getValue()[1];
            Double rate = total > 0 ? round1(confirmed * 100.0 / total) : 0.0;
            provinceConfirm.add(new LedgerStatisticsVO.ProvinceConfirm(e.getKey(), total, confirmed, rate));
        }

        LedgerStatisticsVO vo = new LedgerStatisticsVO();
        vo.setTotalArchive(archives.size());
        vo.setByRightType(byRightType);
        vo.setByConfirmStatus(byConfirmStatus);
        vo.setByAuthStatus(byAuthStatus);
        vo.setBySubsidiary(bySubsidiary);
        vo.setByDeploymentUnit(byDeploymentUnit);
        vo.setByProvince(byProvince);
        vo.setByBureau(byBureau);
        vo.setProvinceConfirm(provinceConfirm);
        vo.setProvinceAuthStatus(provinceAuthStatus);
        vo.setTrend(buildTrend(archives));
        return vo;
    }

    /** 归属解析:已存 province_code/bureau_code 走组织主数据译名,否则按归属名(子公司/权属主体)兜底解析。 */
    private Jurisdiction jurisdictionOf(String provinceCode, String bureauCode, String fallbackName) {
        Jurisdiction byCode = orgService.describe(provinceCode, bureauCode);
        if (!byCode.isEmpty()) {
            return byCode;
        }
        return orgService.resolve(fallbackName);
    }

    /** 省域展示名:有名取名,有码取码,皆无落「未标识」。 */
    private String provinceLabel(Jurisdiction j) {
        if (StringUtils.hasText(j.provinceName())) {
            return j.provinceName();
        }
        return StringUtils.hasText(j.provinceCode()) ? j.provinceCode() : UNIDENTIFIED;
    }

    /** 地市展示名:有名取名,有码取码,皆无落「未标识」。 */
    private String bureauLabel(Jurisdiction j) {
        if (StringUtils.hasText(j.bureauName())) {
            return j.bureauName();
        }
        return StringUtils.hasText(j.bureauCode()) ? j.bureauCode() : UNIDENTIFIED;
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
