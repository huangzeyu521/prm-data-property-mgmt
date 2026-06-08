package com.csg.prm.confirm.service.impl;

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
import java.util.List;
import java.util.Map;
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

    @Override
    public ConfirmDashboardVO dashboard() {
        List<ConfirmApply> applies = applyMapper.selectList(null);
        List<EquityCard> cards = cardMapper.selectList(null);

        long total = applies.size();
        long done = applies.stream().filter(a -> ConfirmApply.STATUS_DONE.equals(a.getStatus())).count();
        long rejected = applies.stream().filter(a -> ConfirmApply.STATUS_REJECTED.equals(a.getStatus())).count();
        long pending = total - done - rejected;
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
        return vo;
    }
}
