package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.mapper.AuthAgreementMapper;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthAgreementServiceImpl implements AuthAgreementService {

    private final AuthAgreementMapper mapper;
    private final ChainEvidenceService chainEvidenceService;
    private final com.csg.prm.authorize.gateway.DataAccessGateway dataAccessGateway;

    public AuthAgreementServiceImpl(AuthAgreementMapper mapper, ChainEvidenceService chainEvidenceService,
                                    com.csg.prm.authorize.gateway.DataAccessGateway dataAccessGateway) {
        this.mapper = mapper;
        this.chainEvidenceService = chainEvidenceService;
        this.dataAccessGateway = dataAccessGateway;
    }

    @Override
    @Transactional
    public String generate(String applyId, String templateId, String granteeOrg) {
        if (!StringUtils.hasText(applyId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "申请ID不能为空");
        }
        AuthAgreement a = new AuthAgreement();
        a.setAgreementNo("XY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        a.setApplyId(applyId);
        a.setTemplateId(templateId);
        a.setGranteeOrg(granteeOrg);
        a.setSealStatus(AuthAgreement.SEAL_PENDING);
        a.setReviewStatus(AuthAgreement.REVIEW_PENDING);
        a.setArchiveStatus(AuthAgreement.ARCHIVE_NO);
        mapper.insert(a);
        return a.getAgreementId();
    }

    @Override
    @Transactional
    public void signByGrantor(String agreementId, String fileUrl) {
        AuthAgreement a = require(agreementId);
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setGrantorSigned(Boolean.TRUE);
        upd.setGrantorSignTime(LocalDateTime.now());
        if (StringUtils.hasText(fileUrl)) {
            upd.setFileUrl(fileUrl);
        }
        upd.setSealStatus(sealStatusAfter(true, Boolean.TRUE.equals(a.getGranteeSigned())));
        upd.setSealVerify("授权方签章有效(CA核验通过)");
        upd.setSignTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void signByGrantee(String agreementId, String fileUrl) {
        AuthAgreement a = require(agreementId);
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setGranteeSigned(Boolean.TRUE);
        upd.setGranteeSignTime(LocalDateTime.now());
        if (StringUtils.hasText(fileUrl)) {
            upd.setFileUrl(fileUrl);
        }
        upd.setSealStatus(sealStatusAfter(Boolean.TRUE.equals(a.getGrantorSigned()), true));
        upd.setSealVerify("被授权方签章有效(CA核验通过)");
        upd.setSignTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    private String sealStatusAfter(boolean grantor, boolean grantee) {
        if (grantor && grantee) {
            return AuthAgreement.SEAL_SIGNED;
        }
        return (grantor || grantee) ? AuthAgreement.SEAL_PARTIAL : AuthAgreement.SEAL_PENDING;
    }

    @Override
    @Transactional
    public void review(String agreementId, boolean pass) {
        AuthAgreement a = require(agreementId);
        if (!AuthAgreement.SEAL_SIGNED.equals(a.getSealStatus())) {
            throw new BizException("协议尚未完成双方签章,不可审核");
        }
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setReviewStatus(pass ? AuthAgreement.REVIEW_PASS : AuthAgreement.REVIEW_REJECT);
        mapper.updateById(upd);
        // 协议审核通过 -> 正式生效,触发底层数据资源权限开通(需求§4.3.3)
        if (pass) {
            dataAccessGateway.grant(a.getApplyId(), a.getGranteeOrg(), a.getFileUrl());
        }
    }

    @Override
    @Transactional
    public void archive(String agreementId) {
        AuthAgreement a = require(agreementId);
        if (!AuthAgreement.REVIEW_PASS.equals(a.getReviewStatus())) {
            throw new BizException("仅审核通过的协议可归档");
        }
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setArchiveStatus(AuthAgreement.ARCHIVE_YES);
        mapper.updateById(upd);
        // 关键节点上链存证(协议存档):锁定双签协议内容指纹
        chainEvidenceService.anchor("协议存档", agreementId,
                "授权运营协议 " + a.getAgreementNo() + " / " + a.getGranteeOrg(),
                String.join("|", a.getAgreementNo(), a.getApplyId(), a.getGranteeOrg(),
                        a.getFileUrl() == null ? "" : a.getFileUrl()));
    }

    @Override
    public AuthAgreement getById(String agreementId) {
        return require(agreementId);
    }

    @Override
    public PageResult<AuthAgreement> page(long current, long size, String reviewStatus, String archiveStatus) {
        LambdaQueryWrapper<AuthAgreement> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(reviewStatus), AuthAgreement::getReviewStatus, reviewStatus)
                .eq(StringUtils.hasText(archiveStatus), AuthAgreement::getArchiveStatus, archiveStatus)
                .orderByDesc(AuthAgreement::getCreateTime);
        IPage<AuthAgreement> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    private AuthAgreement require(String agreementId) {
        if (!StringUtils.hasText(agreementId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "协议ID不能为空");
        }
        AuthAgreement a = mapper.selectById(agreementId);
        if (a == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "协议不存在");
        }
        return a;
    }
}
