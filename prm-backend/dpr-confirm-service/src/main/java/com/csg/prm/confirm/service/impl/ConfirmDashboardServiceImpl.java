package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.confirm.dto.ConfirmDashboardVO;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.ConfirmDashboardService;
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
public class ConfirmDashboardServiceImpl implements ConfirmDashboardService {

    private static final String UNKNOWN = "未分类";

    private final ConfirmApplyMapper applyMapper;
    private final EquityCardMapper cardMapper;

    public ConfirmDashboardServiceImpl(ConfirmApplyMapper applyMapper, EquityCardMapper cardMapper) {
        this.applyMapper = applyMapper;
        this.cardMapper = cardMapper;
    }

    private static final Set<String> IN_REVIEW = Set.of(
            ConfirmApply.STATUS_PRECHECK, ConfirmApply.STATUS_COMPLIANCE,
            ConfirmApply.STATUS_MANAGER, ConfirmApply.STATUS_DIRECTOR);

    @Override
    public ConfirmDashboardVO dashboard(String deptName, String startTime, String endTime) {
        // 组织(部门)/时间周期 筛选
        LambdaQueryWrapper<ConfirmApply> w = new LambdaQueryWrapper<ConfirmApply>()
                .eq(StringUtils.hasText(deptName), ConfirmApply::getRespDept, deptName)
                .ge(StringUtils.hasText(startTime), ConfirmApply::getCreateTime, startTime)
                .le(StringUtils.hasText(endTime), ConfirmApply::getCreateTime, endTime);
        List<ConfirmApply> applies = applyMapper.selectList(w);
        List<EquityCard> cards = cardMapper.selectList(null);

        long total = applies.size();
        long done = applies.stream().filter(a -> ConfirmApply.STATUS_DONE.equals(a.getStatus())).count();
        long rejected = applies.stream().filter(a -> ConfirmApply.STATUS_REJECTED.equals(a.getStatus())).count();
        // 已撤回为终态,不计入"审批中(待办)";否则减法会把撤回单错算成在审(枚举一致性)
        long withdrawn = applies.stream().filter(a -> ConfirmApply.STATUS_WITHDRAWN.equals(a.getStatus())).count();
        // 待处理=审批中(IN_REVIEW)实计,而非"非终态"减法——草稿(未提交)不是待办,不能算进积压(否则误触积压预警)
        long pending = applies.stream().filter(a -> a.getStatus() != null && IN_REVIEW.contains(a.getStatus())).count();
        long decided = done + rejected;
        double passRate = decided == 0 ? 0d
                : BigDecimal.valueOf(done * 100.0 / decided).setScale(2, RoundingMode.HALF_UP).doubleValue();
        long reConfirmCount = applies.stream().filter(a -> Boolean.TRUE.equals(a.getReConfirm())).count();

        Map<String, Long> statusDist = applies.stream()
                .collect(Collectors.groupingBy(a -> StringUtils.hasText(a.getStatus()) ? a.getStatus() : UNKNOWN,
                        Collectors.counting()));
        Map<String, Long> rightTypeDist = cards.stream()
                .collect(Collectors.groupingBy(c -> StringUtils.hasText(c.getRightType()) ? c.getRightType() : UNKNOWN,
                        Collectors.counting()));

        ConfirmDashboardVO vo = new ConfirmDashboardVO();
        vo.setTotalApply(total);
        vo.setPending(pending);
        vo.setDone(done);
        vo.setRejected(rejected);
        vo.setPassRate(passRate);
        vo.setCardCount(cards.size());
        vo.setReConfirmCount(reConfirmCount);
        vo.setStatusDistribution(statusDist);
        vo.setRightTypeDistribution(rightTypeDist);

        // 流程瓶颈识别:各审批中节点积压 + 瓶颈环节(最大积压)
        Map<String, Long> nodeBacklog = applies.stream()
                .filter(a -> a.getStatus() != null && IN_REVIEW.contains(a.getStatus()))
                .collect(Collectors.groupingBy(ConfirmApply::getStatus, Collectors.counting()));
        vo.setNodeBacklog(nodeBacklog);
        vo.setBottleneckNode(nodeBacklog.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("无积压"));

        // 月度趋势:申请量 + 通过率
        Map<String, List<ConfirmApply>> byMonth = new TreeMap<>(applies.stream()
                .filter(a -> a.getCreateTime() != null)
                .collect(Collectors.groupingBy(a -> a.getCreateTime().toString().substring(0, 7))));
        List<ConfirmDashboardVO.TrendPoint> trend = new ArrayList<>();
        byMonth.forEach((m, ms) -> {
            long md = ms.stream().filter(a -> ConfirmApply.STATUS_DONE.equals(a.getStatus())).count();
            long mr = ms.stream().filter(a -> ConfirmApply.STATUS_REJECTED.equals(a.getStatus())).count();
            long mdec = md + mr;
            double mpr = mdec == 0 ? 0d
                    : BigDecimal.valueOf(md * 100.0 / mdec).setScale(2, RoundingMode.HALF_UP).doubleValue();
            trend.add(new ConfirmDashboardVO.TrendPoint(m, ms.size(), mpr));
        });
        vo.setTrend(trend);

        // 风险趋势预警
        List<String> alerts = new ArrayList<>();
        if (decided > 0 && passRate < 70) {
            alerts.add("确权通过率偏低(" + passRate + "%),建议复核驳回原因与材料质量");
        }
        if (total > 0 && pending > total * 0.5) {
            alerts.add("待处理积压占比高(" + pending + "/" + total + "),流转效率需关注");
        }
        long maxBacklog = nodeBacklog.values().stream().max(Long::compare).orElse(0L);
        if (maxBacklog >= 3) {
            alerts.add(vo.getBottleneckNode() + " 环节积压 " + maxBacklog + " 件,为当前流程瓶颈,建议增派审核力量");
        }
        if (reConfirmCount >= 3) {
            alerts.add("重确权工单较多(" + reConfirmCount + " 件),监测联动派生频繁,关注数据稳定性");
        }
        if (alerts.isEmpty()) {
            alerts.add("各项指标正常,无显著风险");
        }
        vo.setRiskAlerts(alerts);
        return vo;
    }
}
