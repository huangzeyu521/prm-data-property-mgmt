package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.authorize.entity.Accountability;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.mapper.AuthCertMapper;
import com.csg.prm.authorize.service.AccountabilityService;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthCertServiceImpl implements AuthCertService {

    /** 默认违规类型(监测联动未显式指明时) */
    private static final String DEFAULT_VIOLATION = "违规使用";

    private final AuthCertMapper mapper;
    private final AccountabilityService accountabilityService;
    private final ChainEvidenceService chainEvidenceService;

    public AuthCertServiceImpl(AuthCertMapper mapper, AccountabilityService accountabilityService,
                               ChainEvidenceService chainEvidenceService) {
        this.mapper = mapper;
        this.accountabilityService = accountabilityService;
        this.chainEvidenceService = chainEvidenceService;
    }

    @Override
    @Transactional
    public String generateFromApply(AuthApply apply) {
        AuthCert cert = new AuthCert();
        cert.setCertNo(generateCertNo());
        cert.setApplyId(apply.getApplyId());
        cert.setAssetId(apply.getAssetId());
        cert.setGranteeOrg(apply.getGranteeOrg());
        cert.setRightType(apply.getRightType());
        cert.setScope(apply.getScope());
        cert.setValidDate(apply.getValidDate());
        cert.setCertStatus(AuthCert.STATUS_EFFECTIVE);
        mapper.insert(cert);
        // 关键节点上链存证(授权发证):SM3 指纹锚定上链,防篡改、可追溯
        chainEvidenceService.anchor("授权发证", cert.getCertId(),
                "授权证书 " + cert.getCertNo() + " / " + cert.getGranteeOrg(),
                String.join("|", cert.getCertNo(), cert.getApplyId(), cert.getAssetId(),
                        cert.getGranteeOrg(), cert.getRightType()));
        return cert.getCertId();
    }

    @Override
    public AuthCert getById(String certId) {
        AuthCert cert = mapper.selectById(certId);
        if (cert == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "授权证书不存在");
        }
        return cert;
    }

    @Override
    @Transactional
    public void revoke(String certId) {
        getById(certId);
        AuthCert upd = new AuthCert();
        upd.setCertId(certId);
        upd.setCertStatus(AuthCert.STATUS_REVOKED);
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AuthCert> page(PageQuery query) {
        LambdaQueryWrapper<AuthCert> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AuthCert::getCreateTime);
        IPage<AuthCert> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    @Transactional
    public int suspendByAsset(String assetId, String reason, String sourceAlertId, String violationType) {
        if (!StringUtils.hasText(assetId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产ID不能为空");
        }
        LambdaQueryWrapper<AuthCert> w = new LambdaQueryWrapper<>();
        w.eq(AuthCert::getAssetId, assetId).eq(AuthCert::getCertStatus, AuthCert.STATUS_EFFECTIVE);
        List<AuthCert> certs = mapper.selectList(w);
        String vt = StringUtils.hasText(violationType) ? violationType : DEFAULT_VIOLATION;
        for (AuthCert cert : certs) {
            AuthCert upd = new AuthCert();
            upd.setCertId(cert.getCertId());
            upd.setCertStatus(AuthCert.STATUS_SUSPENDED);
            upd.setSuspendReason(reason);
            mapper.updateById(upd);

            // 自动建违规追责记录(待追责)
            Accountability acc = new Accountability();
            acc.setCertId(cert.getCertId());
            acc.setAssetId(cert.getAssetId());
            acc.setGranteeOrg(cert.getGranteeOrg());
            acc.setViolationType(vt);
            acc.setSourceAlertId(sourceAlertId);
            acc.setReason(reason);
            accountabilityService.openForSuspension(acc);

            // 关键节点上链存证(熔断处置)
            chainEvidenceService.anchor("熔断处置", cert.getCertId(),
                    "证书 " + cert.getCertNo() + " 熔断暂停:" + vt,
                    String.join("|", cert.getCertNo(), cert.getAssetId(), vt,
                            reason == null ? "" : reason, sourceAlertId == null ? "" : sourceAlertId));
        }
        return certs.size();
    }

    @Override
    @Transactional
    public void renew(String certId, LocalDateTime newValidDate) {
        AuthCert cert = getById(certId);
        if (AuthCert.STATUS_REVOKED.equals(cert.getCertStatus())) {
            throw new BizException("已撤销证书不可续签");
        }
        if (newValidDate == null || newValidDate.isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "续签有效期必须晚于当前时间");
        }
        AuthCert upd = new AuthCert();
        upd.setCertId(certId);
        upd.setValidDate(newValidDate);
        // 整改后续签:已暂停证书恢复生效
        upd.setCertStatus(AuthCert.STATUS_EFFECTIVE);
        mapper.updateById(upd);
    }

    @Override
    public List<AuthCert> findExpiring(int days) {
        int d = days <= 0 ? 30 : days;
        LocalDateTime threshold = LocalDateTime.now().plusDays(d);
        LambdaQueryWrapper<AuthCert> w = new LambdaQueryWrapper<>();
        w.eq(AuthCert::getCertStatus, AuthCert.STATUS_EFFECTIVE)
                .isNotNull(AuthCert::getValidDate)
                .le(AuthCert::getValidDate, threshold)
                .orderByAsc(AuthCert::getValidDate);
        return mapper.selectList(w);
    }

    private String generateCertNo() {
        return "AC-PRA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
