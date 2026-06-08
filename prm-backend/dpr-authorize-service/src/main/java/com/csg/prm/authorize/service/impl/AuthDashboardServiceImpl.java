package com.csg.prm.authorize.service.impl;

import com.csg.prm.authorize.dto.AuthDashboardVO;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.mapper.AuthCertMapper;
import com.csg.prm.authorize.service.AuthDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthDashboardServiceImpl implements AuthDashboardService {

    private static final String UNKNOWN = "未分类";

    private final AuthApplyMapper applyMapper;
    private final AuthCertMapper certMapper;

    public AuthDashboardServiceImpl(AuthApplyMapper applyMapper, AuthCertMapper certMapper) {
        this.applyMapper = applyMapper;
        this.certMapper = certMapper;
    }

    @Override
    public AuthDashboardVO dashboard() {
        List<AuthApply> applies = applyMapper.selectList(null);
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
        return vo;
    }
}
