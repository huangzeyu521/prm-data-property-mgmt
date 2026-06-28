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
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.org.Jurisdiction;
import com.csg.prm.common.org.OrgService;
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
    private final com.csg.prm.authorize.mapper.AuthCertTemplateMapper templateMapper;
    private final com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper;
    private final com.csg.prm.authorize.gateway.EquityCardGateway equityCardGateway;
    private final OrgService orgService;

    public AuthCertServiceImpl(AuthCertMapper mapper, AccountabilityService accountabilityService,
                               ChainEvidenceService chainEvidenceService,
                               com.csg.prm.authorize.mapper.AuthCertTemplateMapper templateMapper,
                               com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper,
                               com.csg.prm.authorize.gateway.EquityCardGateway equityCardGateway,
                               OrgService orgService) {
        this.mapper = mapper;
        this.accountabilityService = accountabilityService;
        this.chainEvidenceService = chainEvidenceService;
        this.templateMapper = templateMapper;
        this.applyMapper = applyMapper;
        this.equityCardGateway = equityCardGateway;
        this.orgService = orgService;
    }

    /** 按授权类型(模式)+权益类型 选生效证书模板(自选/自动匹配)。 */
    private com.csg.prm.authorize.entity.AuthCertTemplate matchTemplate(AuthApply apply) {
        String certType = "批量".equals(apply.getAuthMode())
                ? com.csg.prm.authorize.entity.AuthCertTemplate.TYPE_BATCH
                : com.csg.prm.authorize.entity.AuthCertTemplate.TYPE_SPECIAL;
        java.util.List<com.csg.prm.authorize.entity.AuthCertTemplate> tpls = templateMapper.selectList(
                new LambdaQueryWrapper<com.csg.prm.authorize.entity.AuthCertTemplate>()
                        .eq(com.csg.prm.authorize.entity.AuthCertTemplate::getCertType, certType)
                        .eq(com.csg.prm.authorize.entity.AuthCertTemplate::getRightType, apply.getRightType())
                        .eq(com.csg.prm.authorize.entity.AuthCertTemplate::getTemplateStatus, "生效中")
                        .orderByDesc(com.csg.prm.authorize.entity.AuthCertTemplate::getCreateTime));
        return tpls.isEmpty() ? null : tpls.get(0);
    }

    @Override
    @Transactional
    public String generateFromApply(AuthApply apply) {
        // 出证前合规校验:授权范围不超确权边界(确权边界为"全字段"=不限;"约定字段"=须在边界内)
        com.csg.prm.authorize.gateway.EquityCardGateway.CardBoundary b = equityCardGateway.boundary(apply.getEquityCardId());
        if (b != null && StringUtils.hasText(b.scope()) && !"全字段".equals(b.scope())
                && StringUtils.hasText(apply.getScope()) && !b.scope().equals(apply.getScope())) {
            throw new BusinessException("授权范围超出确权边界,出证拦截(确权范围:" + b.scope() + ")");
        }
        // 归口网级回填:按被授权方组织解析省/地市码,落到证书与发证存证(补此前的 null)
        Jurisdiction jur = orgService.resolve(apply.getGranteeOrg());
        AuthCert cert = new AuthCert();
        cert.setCertNo(generateCertNo());
        cert.setApplyId(apply.getApplyId());
        cert.setAssetId(apply.getAssetId());
        cert.setGranteeOrg(apply.getGranteeOrg());
        cert.setRightType(apply.getRightType());
        cert.setScope(apply.getScope());
        cert.setValidDate(apply.getValidDate());
        cert.setCertStatus(AuthCert.STATUS_EFFECTIVE);
        // 按授权类型自动匹配生效证书模板(自选/标准化出证)
        com.csg.prm.authorize.entity.AuthCertTemplate tpl = matchTemplate(apply);
        if (tpl != null) {
            cert.setTemplateId(tpl.getTemplateId());
            cert.setTemplateName(tpl.getTemplateName());
        } else {
            cert.setTemplateName("标准授权证书(默认)");
        }
        if (StringUtils.hasText(jur.provinceCode())) {
            cert.setProvinceCode(jur.provinceCode());
        }
        if (StringUtils.hasText(jur.bureauCode())) {
            cert.setBureauCode(jur.bureauCode());
        }
        mapper.insert(cert);
        // 关键节点上链存证(授权发证):SM3 指纹锚定上链,防篡改、可追溯;带归口网级省/地市码
        chainEvidenceService.anchor("授权发证", cert.getCertId(),
                "授权证书 " + cert.getCertNo() + " / " + cert.getGranteeOrg(),
                String.join("|", cert.getCertNo(), cert.getApplyId(), cert.getAssetId(),
                        cert.getGranteeOrg(), cert.getRightType()),
                jur.provinceCode(), jur.bureauCode());
        return cert.getCertId();
    }

    @Override
    public AuthCert getById(String certId) {
        AuthCert cert = mapper.selectById(certId);
        if (cert == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "授权证书不存在");
        }
        return cert;
    }

    @Override
    public com.csg.prm.authorize.dto.AuthCertRenderVO render(String certId) {
        AuthCert cert = getById(certId);
        com.csg.prm.authorize.dto.AuthCertRenderVO vo = new com.csg.prm.authorize.dto.AuthCertRenderVO();
        vo.setCertNo(cert.getCertNo());
        vo.setGranteeOrg(cert.getGranteeOrg());
        vo.setAssetId(cert.getAssetId());
        // 所属系统:assetId 去 SYS: 前缀(库表级证书,凭证显系统而非 raw assetId)
        String assetId = cert.getAssetId();
        vo.setSysName(assetId != null && assetId.startsWith("SYS:") ? assetId.substring(4) : assetId);
        vo.setRightType(cert.getRightType());
        vo.setScope(cert.getScope());
        vo.setValidDate(cert.getValidDate());
        vo.setCertStatus(cert.getCertStatus());
        vo.setTemplateName(cert.getTemplateName());
        if (StringUtils.hasText(cert.getTemplateId())) {
            com.csg.prm.authorize.entity.AuthCertTemplate tpl = templateMapper.selectById(cert.getTemplateId());
            if (tpl != null) {
                vo.setCertType(tpl.getCertType());
                vo.setTemplateContent(tpl.getTemplateContent());
            }
        }
        // 合规校验:授权范围 ≤ 确权边界(经申请的权益卡片回溯)
        boolean ok = true;
        String result = "授权范围未超确权边界,内容合规";
        AuthApply apply = StringUtils.hasText(cert.getApplyId()) ? applyMapper.selectById(cert.getApplyId()) : null;
        if (apply != null) {
            // 表5/表6/§3.4.4:数据表(库表名)/模式/使用场景 由申请单 join 带出(render 本就加载 apply,零额外查询)
            vo.setAssetName(apply.getAssetName());
            vo.setSchemaName(apply.getSchemaName());
            vo.setScenario(apply.getScenario());
            com.csg.prm.authorize.gateway.EquityCardGateway.CardBoundary b = equityCardGateway.boundary(apply.getEquityCardId());
            if (b != null && StringUtils.hasText(b.scope()) && !"全字段".equals(b.scope())
                    && StringUtils.hasText(cert.getScope()) && !b.scope().equals(cert.getScope())) {
                ok = false;
                result = "授权范围与确权边界(" + b.scope() + ")不一致,需复核";
            } else {
                result = "授权范围未超确权边界(确权:" + (b != null && StringUtils.hasText(b.scope()) ? b.scope() : "全字段") + "),内容合规";
            }
        }
        vo.setComplianceOk(ok);
        vo.setComplianceResult(result);
        return vo;
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
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "资产ID不能为空");
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
            throw new BusinessException("已撤销证书不可续签");
        }
        if (newValidDate == null || newValidDate.isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "续签有效期必须晚于当前时间");
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
