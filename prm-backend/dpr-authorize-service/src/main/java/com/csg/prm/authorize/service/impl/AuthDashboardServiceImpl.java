package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.authorize.dto.AuthDashboardVO;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.entity.AuthCompliance;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.mapper.AuthCertMapper;
import com.csg.prm.authorize.mapper.AuthComplianceMapper;
import com.csg.prm.authorize.service.AuthDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AuthDashboardServiceImpl implements AuthDashboardService {

    private static final String UNKNOWN = "未分类";

    private final AuthApplyMapper applyMapper;
    private final AuthCertMapper certMapper;
    private final AuthComplianceMapper complianceMapper;

    public AuthDashboardServiceImpl(AuthApplyMapper applyMapper, AuthCertMapper certMapper,
                                    AuthComplianceMapper complianceMapper) {
        this.applyMapper = applyMapper;
        this.certMapper = certMapper;
        this.complianceMapper = complianceMapper;
    }

    @Override
    public AuthDashboardVO dashboard(String scenario, String deptName, String startTime, String endTime) {
        // 使用场景/组织(部门)/时间周期 筛选
        LambdaQueryWrapper<AuthApply> w = new LambdaQueryWrapper<AuthApply>()
                .eq(StringUtils.hasText(scenario), AuthApply::getScenario, scenario)
                .eq(StringUtils.hasText(deptName), AuthApply::getBusinessDomain, deptName)
                .ge(StringUtils.hasText(startTime), AuthApply::getCreateTime, startTime)
                .le(StringUtils.hasText(endTime), AuthApply::getCreateTime, endTime);
        List<AuthApply> applies = applyMapper.selectList(w);
        List<AuthCert> certs = certMapper.selectList(null);

        long total = applies.size();
        long effective = applies.stream().filter(a -> AuthApply.STATUS_EFFECTIVE.equals(a.getStatus())).count();
        long rejected = applies.stream().filter(a -> AuthApply.STATUS_REJECTED.equals(a.getStatus())).count();
        long inReview = applies.stream().filter(a -> AuthApply.STATUS_REVIEW.equals(a.getStatus())).count();
        double effectiveRate = total == 0 ? 0d
                : BigDecimal.valueOf(effective * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
        long suspended = certs.stream().filter(c -> AuthCert.STATUS_SUSPENDED.equals(c.getCertStatus())).count();

        Map<String, Long> modeDist = applies.stream()
                .collect(Collectors.groupingBy(a -> StringUtils.hasText(a.getAuthMode()) ? a.getAuthMode() : UNKNOWN,
                        Collectors.counting()));
        Map<String, Long> rightTypeDist = applies.stream()
                .collect(Collectors.groupingBy(a -> StringUtils.hasText(a.getRightType()) ? a.getRightType() : UNKNOWN,
                        Collectors.counting()));

        AuthDashboardVO vo = new AuthDashboardVO();
        vo.setTotalApply(total);
        vo.setEffective(effective);
        vo.setRejected(rejected);
        vo.setInReview(inReview);
        vo.setCertCount(certs.size());
        vo.setSuspendedCount(suspended);
        vo.setEffectiveRate(effectiveRate);
        vo.setModeDistribution(modeDist);
        vo.setRightTypeDistribution(rightTypeDist);

        // 合规性检查结果分析(红/黄/绿,限筛选范围内的申请)
        Set<String> applyIds = applies.stream().map(AuthApply::getApplyId).collect(Collectors.toSet());
        List<AuthCompliance> checks = complianceMapper.selectList(null).stream()
                .filter(c -> applyIds.isEmpty() || applyIds.contains(c.getApplyId()))
                .collect(Collectors.toList());
        Map<String, Long> complianceDist = checks.stream()
                .collect(Collectors.groupingBy(c -> StringUtils.hasText(c.getRiskLevel()) ? c.getRiskLevel() : UNKNOWN,
                        Collectors.counting()));
        vo.setComplianceDist(complianceDist);

        // 使用场景授权频次
        Map<String, Long> byScenario = applies.stream()
                .collect(Collectors.groupingBy(a -> StringUtils.hasText(a.getScenario()) ? a.getScenario() : UNKNOWN,
                        Collectors.counting()));
        vo.setByScenario(byScenario);

        // 月度趋势:申请量 + 生效率
        Map<String, List<AuthApply>> byMonth = new TreeMap<>(applies.stream()
                .filter(a -> a.getCreateTime() != null)
                .collect(Collectors.groupingBy(a -> a.getCreateTime().toString().substring(0, 7))));
        List<AuthDashboardVO.TrendPoint> trend = new ArrayList<>();
        byMonth.forEach((m, ms) -> {
            long me = ms.stream().filter(a -> AuthApply.STATUS_EFFECTIVE.equals(a.getStatus())).count();
            double mer = ms.isEmpty() ? 0d
                    : BigDecimal.valueOf(me * 100.0 / ms.size()).setScale(2, RoundingMode.HALF_UP).doubleValue();
            trend.add(new AuthDashboardVO.TrendPoint(m, ms.size(), mer));
        });
        vo.setTrend(trend);

        // 授权风险预警(合规高风险:红灯 红/高)
        long redCount = complianceDist.getOrDefault(AuthCompliance.LEVEL_RED, 0L)
                + complianceDist.getOrDefault("高", 0L);
        long decided = effective + rejected;
        double rejectRate = decided == 0 ? 0d
                : BigDecimal.valueOf(rejected * 100.0 / decided).setScale(2, RoundingMode.HALF_UP).doubleValue();
        List<String> alerts = new ArrayList<>();
        if (decided > 0 && rejectRate >= 30) {
            alerts.add("授权驳回率偏高(" + rejectRate + "%),关注申请合规质量");
        }
        if (redCount > 0) {
            alerts.add("合规校验高风险(红灯)" + redCount + " 件,存在越权/越界风险,须重点复核");
        }
        if (suspended > 0) {
            alerts.add("监测联动熔断暂停证书 " + suspended + " 件,疑似违规用数,须整改");
        }
        if (total > 0 && inReview > total * 0.5) {
            alerts.add("审核中积压占比高(" + inReview + "/" + total + "),授权处理效率需关注");
        }
        if (alerts.isEmpty()) {
            alerts.add("各项指标正常,无显著风险");
        }
        vo.setRiskAlerts(alerts);
        return vo;
    }
}
