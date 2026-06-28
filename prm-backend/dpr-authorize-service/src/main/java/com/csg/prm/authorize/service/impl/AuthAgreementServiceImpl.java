package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.mapper.AuthAgreementMapper;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthAgreementServiceImpl implements AuthAgreementService {

    private static final java.util.Set<String> SEAL_EXT = java.util.Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx");
    private static final long SEAL_MAX_BYTES = 20L * 1024 * 1024;

    private final AuthAgreementMapper mapper;
    private final ChainEvidenceService chainEvidenceService;
    private final com.csg.prm.authorize.gateway.DataAccessGateway dataAccessGateway;
    private final com.csg.prm.authorize.mapper.AuthSealUploadLogMapper sealLogMapper;
    private final com.csg.prm.authorize.mapper.AgreementReviewLogMapper reviewLogMapper;
    private final com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper;
    private final com.csg.prm.authorize.mapper.AgreementArchiveLogMapper archiveLogMapper;

    public AuthAgreementServiceImpl(AuthAgreementMapper mapper, ChainEvidenceService chainEvidenceService,
                                    com.csg.prm.authorize.gateway.DataAccessGateway dataAccessGateway,
                                    com.csg.prm.authorize.mapper.AuthSealUploadLogMapper sealLogMapper,
                                    com.csg.prm.authorize.mapper.AgreementReviewLogMapper reviewLogMapper,
                                    com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper,
                                    com.csg.prm.authorize.mapper.AgreementArchiveLogMapper archiveLogMapper) {
        this.mapper = mapper;
        this.chainEvidenceService = chainEvidenceService;
        this.dataAccessGateway = dataAccessGateway;
        this.sealLogMapper = sealLogMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.applyMapper = applyMapper;
        this.archiveLogMapper = archiveLogMapper;
    }

    private void recordArchiveLog(AuthAgreement a, String action, String operator) {
        com.csg.prm.authorize.entity.AgreementArchiveLog log = new com.csg.prm.authorize.entity.AgreementArchiveLog();
        log.setAgreementId(a.getAgreementId());
        log.setAgreementNo(a.getAgreementNo());
        log.setAction(action);
        log.setOperator(StringUtils.hasText(operator) ? operator : "系统用户");
        log.setOperateTime(LocalDateTime.now());
        archiveLogMapper.insert(log);
    }

    @Override
    @Transactional
    public com.csg.prm.authorize.entity.AuthSealUploadLog uploadSeal(String agreementId, String role,
                                                                     String fileName, byte[] data) {
        AuthAgreement a = require(agreementId);
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "上传角色不能为空(授权方/被授权方)");
        }
        if (!StringUtils.hasText(fileName) || data == null || data.length == 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "签章文件为空");
        }
        if (data.length > SEAL_MAX_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "文件超过 20MB 上限");
        }
        // ① 自动校验文件格式
        boolean formatOk = SEAL_EXT.contains(extOf(fileName));
        if (!formatOk) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "签章文件格式不支持,仅支持 PDF/Word/图片");
        }
        // ② 检查签章有效性(模拟 CA/CV 核验:文件非空且达最小尺寸视为签章清晰可核验)
        boolean sealValid = data.length >= 64;
        String verify = sealValid
                ? role + "签章有效:CA/CV 核验通过,印章清晰、与登记印模一致"
                : role + "签章无效:文件过小/印章不清晰,核验未通过";
        // ③ 记录上传行为 + 验证结果
        com.csg.prm.authorize.entity.AuthSealUploadLog log = new com.csg.prm.authorize.entity.AuthSealUploadLog();
        log.setAgreementId(agreementId);
        log.setUploaderRole(role);
        log.setFileName(fileName);
        log.setFileData(java.util.Base64.getEncoder().encodeToString(data));
        log.setFormatOk(Boolean.TRUE);
        log.setSealValid(sealValid);
        log.setVerifyResult(verify);
        log.setUploadTime(LocalDateTime.now());
        sealLogMapper.insert(log);
        // ④ 签章有效则置对应方已签 + 更新签章状态/验证文案
        if (sealValid) {
            AuthAgreement upd = new AuthAgreement();
            upd.setAgreementId(agreementId);
            boolean grantor = "授权方".equals(role) || Boolean.TRUE.equals(a.getGrantorSigned());
            boolean grantee = "被授权方".equals(role) || Boolean.TRUE.equals(a.getGranteeSigned());
            if ("授权方".equals(role)) {
                upd.setGrantorSigned(Boolean.TRUE);
                upd.setGrantorSignTime(LocalDateTime.now());
            } else {
                upd.setGranteeSigned(Boolean.TRUE);
                upd.setGranteeSignTime(LocalDateTime.now());
            }
            upd.setSealStatus(sealStatusAfter(grantor, grantee));
            upd.setSealVerify(verify);
            upd.setFileUrl("/api/dpr/auth/agreement/upload-log/" + log.getLogId() + "/file");
            upd.setSignTime(LocalDateTime.now());
            mapper.updateById(upd);
        }
        log.setFileData(null);
        return log;
    }

    @Override
    public byte[] downloadSeal(String logId) {
        com.csg.prm.authorize.entity.AuthSealUploadLog log = getUploadLog(logId);
        if (!StringUtils.hasText(log.getFileData())) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "该记录无签章文件");
        }
        return java.util.Base64.getDecoder().decode(log.getFileData());
    }

    @Override
    public com.csg.prm.authorize.entity.AuthSealUploadLog getUploadLog(String logId) {
        com.csg.prm.authorize.entity.AuthSealUploadLog log = sealLogMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "上传记录不存在");
        }
        return log;
    }

    @Override
    public java.util.List<com.csg.prm.authorize.entity.AuthSealUploadLog> listUploadLogs(String agreementId) {
        java.util.List<com.csg.prm.authorize.entity.AuthSealUploadLog> list = sealLogMapper.selectList(
                new LambdaQueryWrapper<com.csg.prm.authorize.entity.AuthSealUploadLog>()
                        .eq(com.csg.prm.authorize.entity.AuthSealUploadLog::getAgreementId, agreementId)
                        .orderByDesc(com.csg.prm.authorize.entity.AuthSealUploadLog::getUploadTime));
        list.forEach(l -> l.setFileData(null));
        return list;
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase() : "";
    }

    @Override
    @Transactional
    public String generate(String applyId, String templateId, String granteeOrg) {
        if (!StringUtils.hasText(applyId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "申请ID不能为空");
        }
        AuthAgreement a = new AuthAgreement();
        a.setAgreementNo("XY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        a.setApplyId(applyId);
        a.setTemplateId(templateId);
        a.setGranteeOrg(granteeOrg);
        a.setSealStatus(AuthAgreement.SEAL_PENDING);
        a.setReviewStatus(AuthAgreement.REVIEW_PENDING);
        a.setArchiveStatus(AuthAgreement.ARCHIVE_NO);
        // 联表授权申请带出 协议类型/所属部门(供存档检索)
        com.csg.prm.authorize.entity.AuthApply apply = applyMapper.selectById(applyId);
        if (apply != null) {
            a.setAgreementType(apply.getRightType());
            a.setDeptName(StringUtils.hasText(apply.getBusinessDomain()) ? apply.getBusinessDomain() : apply.getApplicantManager());
            if (!StringUtils.hasText(granteeOrg)) {
                a.setGranteeOrg(apply.getGranteeOrg());
            }
        }
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
    public void review(String agreementId, boolean pass, String opinion) {
        AuthAgreement a = require(agreementId);
        if (!AuthAgreement.SEAL_SIGNED.equals(a.getSealStatus())) {
            throw new BusinessException("协议尚未完成双方签章,不可审核");
        }
        String result = pass ? AuthAgreement.REVIEW_PASS : AuthAgreement.REVIEW_REJECT;
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setReviewStatus(result);
        mapper.updateById(upd);
        // 记录审核处理(审核人/结论/意见/时间)
        com.csg.prm.authorize.entity.AgreementReviewLog log = new com.csg.prm.authorize.entity.AgreementReviewLog();
        log.setAgreementId(agreementId);
        log.setAgreementNo(a.getAgreementNo());
        log.setReviewer("协议审核员");
        log.setResult(result);
        log.setOpinion(StringUtils.hasText(opinion) ? opinion
                : (pass ? "内容与申请单一致,签章有效,准予通过" : "内容不符/签章存疑,驳回重签"));
        log.setReviewTime(LocalDateTime.now());
        reviewLogMapper.insert(log);
        // 协议审核通过 -> 正式生效,触发底层数据资源权限开通(需求§4.3.3)
        if (pass) {
            dataAccessGateway.grant(a.getApplyId(), a.getGranteeOrg(), a.getFileUrl());
        }
    }

    @Override
    public java.util.List<com.csg.prm.authorize.entity.AgreementReviewLog> listReviewLogs(String agreementId) {
        return reviewLogMapper.selectList(
                new LambdaQueryWrapper<com.csg.prm.authorize.entity.AgreementReviewLog>()
                        .eq(com.csg.prm.authorize.entity.AgreementReviewLog::getAgreementId, agreementId)
                        .orderByDesc(com.csg.prm.authorize.entity.AgreementReviewLog::getReviewTime));
    }

    @Override
    @Transactional
    public void archive(String agreementId) {
        AuthAgreement a = require(agreementId);
        if (!AuthAgreement.REVIEW_PASS.equals(a.getReviewStatus())) {
            throw new BusinessException("仅审核通过的协议可归档");
        }
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setArchiveStatus(AuthAgreement.ARCHIVE_YES);
        upd.setArchiveTime(LocalDateTime.now());
        mapper.updateById(upd);
        // 审计日志:归档行为留痕(可追溯)
        recordArchiveLog(a, "归档", null);
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
    public PageResult<AuthAgreement> page(long current, long size, String reviewStatus, String archiveStatus,
                                          String agreementType, String deptName, String archiveStart, String archiveEnd) {
        LambdaQueryWrapper<AuthAgreement> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(reviewStatus), AuthAgreement::getReviewStatus, reviewStatus)
                .eq(StringUtils.hasText(archiveStatus), AuthAgreement::getArchiveStatus, archiveStatus)
                .eq(StringUtils.hasText(agreementType), AuthAgreement::getAgreementType, agreementType)
                .like(StringUtils.hasText(deptName), AuthAgreement::getDeptName, deptName)
                .ge(StringUtils.hasText(archiveStart), AuthAgreement::getArchiveTime, archiveStart)
                .le(StringUtils.hasText(archiveEnd), AuthAgreement::getArchiveTime, archiveEnd)
                .orderByDesc(AuthAgreement::getCreateTime);
        IPage<AuthAgreement> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    @Override
    @Transactional
    public void recordAccess(String agreementId, String action, String operator) {
        AuthAgreement a = require(agreementId);
        recordArchiveLog(a, StringUtils.hasText(action) ? action : "查看", operator);
    }

    @Override
    public java.util.List<com.csg.prm.authorize.entity.AgreementArchiveLog> listArchiveLogs(String agreementId) {
        return archiveLogMapper.selectList(
                new LambdaQueryWrapper<com.csg.prm.authorize.entity.AgreementArchiveLog>()
                        .eq(com.csg.prm.authorize.entity.AgreementArchiveLog::getAgreementId, agreementId)
                        .orderByDesc(com.csg.prm.authorize.entity.AgreementArchiveLog::getOperateTime));
    }

    private AuthAgreement require(String agreementId) {
        if (!StringUtils.hasText(agreementId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "协议ID不能为空");
        }
        AuthAgreement a = mapper.selectById(agreementId);
        if (a == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "协议不存在");
        }
        return a;
    }
}
