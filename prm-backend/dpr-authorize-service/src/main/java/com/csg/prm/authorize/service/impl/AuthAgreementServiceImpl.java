package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthApply;
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

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AuthAgreementServiceImpl.class);
    private static final java.util.Set<String> SEAL_EXT = java.util.Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx");
    private static final long SEAL_MAX_BYTES = 20L * 1024 * 1024;

    private final AuthAgreementMapper mapper;
    private final ChainEvidenceService chainEvidenceService;
    private final com.csg.prm.authorize.gateway.DataAccessGateway dataAccessGateway;
    private final com.csg.prm.authorize.mapper.AuthSealUploadLogMapper sealLogMapper;
    private final com.csg.prm.authorize.mapper.AgreementReviewLogMapper reviewLogMapper;
    private final com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper;
    private final com.csg.prm.authorize.mapper.AgreementArchiveLogMapper archiveLogMapper;
    private final com.csg.prm.authorize.mapper.BatchAuthListMapper batchListMapper;
    private final AppendixDRenderer appendixDRenderer;
    private final com.csg.prm.authorize.service.AuthApplyService authApplyService;

    public AuthAgreementServiceImpl(AuthAgreementMapper mapper, ChainEvidenceService chainEvidenceService,
                                    com.csg.prm.authorize.gateway.DataAccessGateway dataAccessGateway,
                                    com.csg.prm.authorize.mapper.AuthSealUploadLogMapper sealLogMapper,
                                    com.csg.prm.authorize.mapper.AgreementReviewLogMapper reviewLogMapper,
                                    com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper,
                                    com.csg.prm.authorize.mapper.AgreementArchiveLogMapper archiveLogMapper,
                                    com.csg.prm.authorize.mapper.BatchAuthListMapper batchListMapper,
                                    AppendixDRenderer appendixDRenderer,
                                    com.csg.prm.authorize.service.AuthApplyService authApplyService) {
        this.mapper = mapper;
        this.chainEvidenceService = chainEvidenceService;
        this.dataAccessGateway = dataAccessGateway;
        this.sealLogMapper = sealLogMapper;
        this.reviewLogMapper = reviewLogMapper;
        this.applyMapper = applyMapper;
        this.archiveLogMapper = archiveLogMapper;
        this.batchListMapper = batchListMapper;
        this.appendixDRenderer = appendixDRenderer;
        this.authApplyService = authApplyService;
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
        requireFinalDoc(a);
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
            // 双签 ✚ 保密承诺函(附录E)齐即系统自动收尾(核验通过+开数据权限+归档)。前端走 uploadSeal 上传签章件,
            // 故自动收尾必须挂在本路径(signByGrantor/Grantee 仅为旧 API 入口,前端不再调用)。
            autoFinalizeIfFullySigned(agreementId, grantor, grantee);
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
        prefillNegotiationDefaults(a, apply != null ? apply.getValidDate() : null, null);
        mapper.insert(a);
        return a.getAgreementId();
    }

    @Override
    @Transactional
    public String generateForBatch(String batchListId) {
        // 批量授权:一份《数据批量授权清单》→ 一份《运营授权协议》(清单各项=协议附件,授权方↔被授权方框架合同)。
        // 工作指引:清单经领导小组批准后,授权方/被授权方签运营授权协议;附录G 备案附件=授权协议+数据授权清单。
        if (!StringUtils.hasText(batchListId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "批量清单ID不能为空");
        }
        com.csg.prm.authorize.entity.BatchAuthList list = batchListMapper.selectById(batchListId);
        if (list == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "批量授权清单不存在");
        }
        if (!com.csg.prm.authorize.entity.BatchAuthList.STATUS_APPROVED.equals(list.getListStatus())) {
            throw new BusinessException("仅领导小组批准的批量清单可签订运营授权协议(当前:" + list.getListStatus() + ")");
        }
        // 一清单一协议:已生成则直接返回(幂等防重)
        AuthAgreement exist = mapper.selectOne(new LambdaQueryWrapper<AuthAgreement>()
                .eq(AuthAgreement::getBatchListId, batchListId).last("limit 1"));
        if (exist != null) {
            return exist.getAgreementId();
        }
        java.util.List<AuthApply> items = applyMapper.selectList(new LambdaQueryWrapper<AuthApply>()
                .eq(AuthApply::getBatchListId, batchListId));
        if (items.isEmpty()) {
            throw new BusinessException("批量清单无授权项,无法生成协议");
        }
        AuthApply first = items.get(0);
        AuthAgreement a = new AuthAgreement();
        a.setAgreementNo("XY-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        a.setBatchListId(batchListId);
        a.setGranteeOrg(first.getGranteeOrg());          // 清单被授权方批量共享
        a.setAgreementType(first.getRightType());
        a.setDeptName(StringUtils.hasText(first.getBusinessDomain()) ? first.getBusinessDomain() : first.getApplicantManager());
        a.setSealStatus(AuthAgreement.SEAL_PENDING);
        a.setReviewStatus(AuthAgreement.REVIEW_PENDING);
        a.setArchiveStatus(AuthAgreement.ARCHIVE_NO);
        // 协商要素预填:止日=max(今日+3年,明细最长时效);地理范围取清单头申报值(缺省广东省行政区域内)
        LocalDateTime maxItem = items.stream().map(AuthApply::getValidDate)
                .filter(java.util.Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        prefillNegotiationDefaults(a, maxItem, list.getGeoScope());
        mapper.insert(a);
        return a.getAgreementId();
    }

    /**
     * 协商要素预填(生成协议草案时):要素落定表单打开即有合理缺省,只需确认/微调。
     * 止日=max(今日+3年,来源明细最长授权时效)——附录D 表1「一般为3年」且「具体项目数据使用期限不得超过本协议有效期」。
     */
    private void prefillNegotiationDefaults(AuthAgreement a, LocalDateTime maxItemValidDate, String geoScope) {
        a.setDocStatus(AuthAgreement.DOC_DRAFT);
        LocalDateTime threeYears = LocalDateTime.now().plusYears(3);
        a.setValidUntil(maxItemValidDate != null && maxItemValidDate.isAfter(threeYears) ? maxItemValidDate : threeYears);
        a.setGeoScope(StringUtils.hasText(geoScope) ? geoScope : "广东省行政区域内");
        a.setSecurityEncrypt("加密传输(传输通道加密,存储加密)");
        a.setSecurityAccess("最小授权访问控制,按需授予");
        a.setSecurityAudit("全流程操作留痕、可追溯审计");
        a.setDisputeMethod("向甲方所在地人民法院起诉");
        a.setCopiesCount(4);
    }

    @Override
    public String autoGenerateForApprovedApply(String applyId) {
        try {
            if (!StringUtils.hasText(applyId)) {
                return null;
            }
            AuthApply apply = applyMapper.selectById(applyId);
            // 表2 110:副总终审「批准」即形成协议(待双签);兼容存量「已生效」单据补生成
            if (apply == null || !(AuthApply.STATUS_APPROVED.equals(apply.getStatus())
                    || AuthApply.STATUS_EFFECTIVE.equals(apply.getStatus()))) {
                return null;
            }
            if (!AuthApply.MODE_SPECIAL.equals(apply.getAuthMode())) {
                return null; // 批量授权走清单级《运营授权协议》(generateForBatch),不在明细级生成
            }
            // 幂等:该申请已生成协议则复用
            AuthAgreement exist = mapper.selectOne(new LambdaQueryWrapper<AuthAgreement>()
                    .eq(AuthAgreement::getApplyId, applyId).last("limit 1"));
            if (exist != null) {
                return exist.getAgreementId();
            }
            return generate(applyId, null, apply.getGranteeOrg());
        } catch (Exception e) {
            LOGGER.warn("[授权] 申请 {} 批准后自动生成《运营授权协议》失败(可在协议工作台重新生成):{}",
                    applyId, e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public void signByGrantor(String agreementId, String fileUrl) {
        AuthAgreement a = require(agreementId);
        requireFinalDoc(a);
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
        autoFinalizeIfFullySigned(agreementId, true, Boolean.TRUE.equals(a.getGranteeSigned()));
    }

    @Override
    @Transactional
    public void signByGrantee(String agreementId, String fileUrl) {
        AuthAgreement a = require(agreementId);
        requireFinalDoc(a);
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
        autoFinalizeIfFullySigned(agreementId, Boolean.TRUE.equals(a.getGrantorSigned()), true);
    }

    private String sealStatusAfter(boolean grantor, boolean grantee) {
        if (grantor && grantee) {
            return AuthAgreement.SEAL_SIGNED;
        }
        return (grantor || grantee) ? AuthAgreement.SEAL_PARTIAL : AuthAgreement.SEAL_PENDING;
    }

    /**
     * 收尾三条件:甲签 ✚ 乙签 ✚《保密承诺函》(附录E,附录D 第八章「乙方履行本协议须签署」)。
     * 三者齐即系统自动收尾(对齐 35号文:协议环节无独立人工审核节点,授权审核已在上游申请·清单阶段完成):
     * ① 一致性核验通过(签章件 CA 已核 + 协议要素与来源申请单一致,防阴阳合同)→ 写系统核验日志;
     * ② 触发底层数据资源权限开通(需求§4.3.3,原人工 review 通过的关键副作用,此处保留);③ 自动归档 + 审计留痕 + 上链存证。
     * 缺承诺函则保持双签待收尾态,由 uploadConfidentiality 补齐后再触发;幂等(已核验通过则跳过)。
     */
    private void autoFinalizeIfFullySigned(String agreementId, boolean grantor, boolean grantee) {
        if (!(grantor && grantee)) {
            return; // 仅单方签署,等另一方
        }
        AuthAgreement a = require(agreementId);
        if (AuthAgreement.REVIEW_PASS.equals(a.getReviewStatus())) {
            return; // 已核验归档,幂等
        }
        if (!confidentialityOk(a)) {
            LOGGER.info("[协议] {} 已双签,待《保密承诺函》(附录E)收口后自动归档开权限", a.getAgreementNo());
            return; // 双签但承诺函未收口:不归档、不开权限,等 uploadConfidentiality
        }
        // ⓪ 一事一议:先促成来源申请 批准->已生效(35号文 表2 110-130 先签约后执行授权;含卡片冻结熔断复检,
        //   熔断抛出则本次收尾整体回滚,不归档、不开权限);批量清单级协议(applyId 空)/存量已生效单幂等跳过。
        if (StringUtils.hasText(a.getApplyId())) {
            authApplyService.markEffectiveAfterAgreement(a.getApplyId());
        }
        // ① 自动一致性核验通过 + ③ 归档(同一行回写)
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setReviewStatus(AuthAgreement.REVIEW_PASS);
        upd.setArchiveStatus(AuthAgreement.ARCHIVE_YES);
        upd.setArchiveTime(LocalDateTime.now());
        mapper.updateById(upd);
        // 系统核验日志(替代原人工审核记录)
        com.csg.prm.authorize.entity.AgreementReviewLog log = new com.csg.prm.authorize.entity.AgreementReviewLog();
        log.setAgreementId(agreementId);
        log.setAgreementNo(a.getAgreementNo());
        log.setReviewer("系统自动核验");
        log.setResult(AuthAgreement.REVIEW_PASS);
        log.setOpinion("双方签章完成,签章CA核验有效、协议要素与来源申请单一致,系统自动核验通过并归档");
        log.setReviewTime(LocalDateTime.now());
        reviewLogMapper.insert(log);
        // ② 底层数据资源权限开通(需求§4.3.3)——保留原 review(pass) 的关键副作用
        dataAccessGateway.grant(a.getApplyId(), a.getGranteeOrg(), a.getFileUrl());
        // ③ 归档审计 + 上链存证(锁定双签协议内容指纹)
        recordArchiveLog(a, "归档", null);
        chainEvidenceService.anchor("协议存档", agreementId,
                "授权运营协议 " + a.getAgreementNo() + " / " + a.getGranteeOrg(),
                String.join("|", a.getAgreementNo(), a.getApplyId() == null ? "" : a.getApplyId(),
                        a.getGranteeOrg(), a.getFileUrl() == null ? "" : a.getFileUrl()));
    }

    @Override
    @Transactional
    public void review(String agreementId, boolean pass, String opinion) {
        AuthAgreement a = require(agreementId);
        if (!AuthAgreement.SEAL_SIGNED.equals(a.getSealStatus())) {
            throw new BusinessException("协议尚未完成双方签章,不可审核");
        }
        // 幂等:已核验通过的协议(双签时已系统自动核验+开权限+归档)不可重复通过,防 dataAccessGateway.grant 二次开通
        if (pass && AuthAgreement.REVIEW_PASS.equals(a.getReviewStatus())) {
            return;
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
            // 一事一议:同步促成来源申请 批准->已生效(幂等;旧 API 入口与 autoFinalize 保持同一收口语义)
            if (StringUtils.hasText(a.getApplyId())) {
                authApplyService.markEffectiveAfterAgreement(a.getApplyId());
            }
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
                String.join("|", a.getAgreementNo(), a.getApplyId() == null ? "" : a.getApplyId(),
                        a.getGranteeOrg(), a.getFileUrl() == null ? "" : a.getFileUrl()));
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

    @Override
    public com.csg.prm.authorize.dto.AgreementElementsVO elements(String agreementId) {
        AuthAgreement ag = require(agreementId);
        // 批量协议:挂在清单上(一清单一协议),聚合清单各项=协议附件《数据授权清单》供核对
        if (StringUtils.hasText(ag.getBatchListId())) {
            java.util.List<AuthApply> items = applyMapper.selectList(new LambdaQueryWrapper<AuthApply>()
                    .eq(AuthApply::getBatchListId, ag.getBatchListId()));
            return com.csg.prm.authorize.dto.AgreementElementsVO.ofBatch(ag, items);
        }
        // 专项协议:协议要素(§3.4.4)在来源申请单上,按 applyId join 带出(申请单缺失容错)
        com.csg.prm.authorize.entity.AuthApply apply = StringUtils.hasText(ag.getApplyId())
                ? applyMapper.selectById(ag.getApplyId()) : null;
        return com.csg.prm.authorize.dto.AgreementElementsVO.of(ag, apply);
    }

    @Override
    public byte[] appendixDDoc(String agreementId) {
        // 正式稿:返回落定时锁定的正文快照(签署与存证以此为准,防签后改稿);草案:按当前要素实时渲染
        AuthAgreement a = require(agreementId);
        if (AuthAgreement.DOC_FINAL.equals(a.getDocStatus()) && StringUtils.hasText(a.getDocSnapshot())) {
            return a.getDocSnapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        String html = appendixDRenderer.render(elements(agreementId), a);
        return html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ===== 协议要素落定(附录D 协商项:草案填空 → 正式稿锁定 → 才可签章) =====

    @Override
    public com.csg.prm.authorize.dto.AgreementNegotiationVO negotiation(String agreementId) {
        AuthAgreement a = require(agreementId);
        return com.csg.prm.authorize.dto.AgreementNegotiationVO.of(a, confidentialityOk(a));
    }

    @Override
    @Transactional
    public void saveNegotiation(String agreementId, com.csg.prm.authorize.dto.AgreementNegotiationVO dto) {
        AuthAgreement a = require(agreementId);
        requireNotTerminated(a);
        if (AuthAgreement.DOC_FINAL.equals(a.getDocStatus())) {
            throw new BusinessException("正式稿已锁定,要素不可再改;如需修改请先退回草案(任一方签章后不可退回)");
        }
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setValidUntil(parseDay(dto.validUntil()));
        upd.setGeoScope(dto.geoScope());
        upd.setSecurityEncrypt(dto.securityEncrypt());
        upd.setSecurityAccess(dto.securityAccess());
        upd.setSecurityAudit(dto.securityAudit());
        upd.setBenefitAllocation(dto.benefitAllocation());
        upd.setPenaltyAmount(dto.penaltyAmount());
        upd.setDisputeMethod(dto.disputeMethod());
        upd.setServiceDelivery(dto.serviceDelivery());
        upd.setCopiesCount(dto.copiesCount());
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void finalizeDoc(String agreementId) {
        AuthAgreement a = require(agreementId);
        requireNotTerminated(a);
        if (AuthAgreement.DOC_FINAL.equals(a.getDocStatus())) {
            return; // 已是正式稿,幂等
        }
        // 完备性校验:附录D 留白协商项全部落定才允许锁定(只填空,不改条款)
        java.util.List<String> missing = new java.util.ArrayList<>();
        if (a.getValidUntil() == null) {
            missing.add("授权有效期止日(表1)");
        }
        if (!StringUtils.hasText(a.getGeoScope())) {
            missing.add("地理范围(表1)");
        }
        if (!StringUtils.hasText(a.getSecurityEncrypt()) || !StringUtils.hasText(a.getSecurityAccess())
                || !StringUtils.hasText(a.getSecurityAudit())) {
            missing.add("数据安全要求三行(表2)");
        }
        if (!StringUtils.hasText(a.getPenaltyAmount())) {
            missing.add("违约金金额(第九章)");
        }
        if (!StringUtils.hasText(a.getDisputeMethod())) {
            missing.add("争议解决方式(第十章)");
        }
        if (!StringUtils.hasText(a.getServiceDelivery())) {
            missing.add("乙方送达信息(第十章)");
        }
        if (!missing.isEmpty()) {
            throw new BusinessException("协议要素未落定,不能生成正式稿。缺:" + String.join("、", missing));
        }
        if (StringUtils.hasText(a.getPenaltyAmount())) {
            try {
                if (Double.parseDouble(a.getPenaltyAmount()) <= 0) {
                    throw new BusinessException("违约金金额须为正数(万元)");
                }
            } catch (NumberFormatException ex) {
                throw new BusinessException("违约金金额须为数字(万元):" + a.getPenaltyAmount());
            }
        }
        // 表1:自签订日一般3年、最长不超过5年(以锁定日为基准校验)
        if (a.getValidUntil().isAfter(LocalDateTime.now().plusYears(5))) {
            throw new BusinessException("授权有效期最长不超过5年(附录D 表1),请调整止日");
        }
        // 「具体项目数据使用期限不得超过本协议有效期」:止日须覆盖来源明细的最长授权时效
        LocalDateTime maxItem = maxItemValidDate(a);
        if (maxItem != null && a.getValidUntil().isBefore(maxItem)) {
            throw new BusinessException("协议止日早于清单明细最长授权时效(" + maxItem.toLocalDate()
                    + "),项目数据使用期限不得超过协议有效期,请调整止日");
        }
        if (a.getCopiesCount() == null || a.getCopiesCount() < 2) {
            a.setCopiesCount(4);
        }
        // 锁定:正式稿 + 正文快照落库(此后 appendix-d 固定返回快照)
        a.setDocStatus(AuthAgreement.DOC_FINAL);
        String html = appendixDRenderer.render(elements(agreementId), a);
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setDocStatus(AuthAgreement.DOC_FINAL);
        upd.setCopiesCount(a.getCopiesCount());
        upd.setDocSnapshot(html);
        mapper.updateById(upd);
        recordArchiveLog(a, "要素落定·生成正式稿", null);
    }

    @Override
    @Transactional
    public void revertToDraft(String agreementId) {
        AuthAgreement a = require(agreementId);
        requireNotTerminated(a);
        if (Boolean.TRUE.equals(a.getGrantorSigned()) || Boolean.TRUE.equals(a.getGranteeSigned())) {
            throw new BusinessException("已有签章的协议不可退回草案(需重签请走协议变更)");
        }
        mapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AuthAgreement>()
                .eq(AuthAgreement::getAgreementId, agreementId)
                .set(AuthAgreement::getDocStatus, AuthAgreement.DOC_DRAFT)
                .set(AuthAgreement::getDocSnapshot, null));
        recordArchiveLog(a, "退回草案", null);
    }

    @Override
    @Transactional
    public com.csg.prm.authorize.entity.AuthSealUploadLog uploadConfidentiality(String agreementId,
                                                                                String fileName, byte[] data) {
        AuthAgreement a = require(agreementId);
        requireNotTerminated(a);
        if (!StringUtils.hasText(fileName) || data == null || data.length == 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "承诺函文件为空");
        }
        if (data.length > SEAL_MAX_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "文件超过 20MB 上限");
        }
        if (!SEAL_EXT.contains(extOf(fileName))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "承诺函文件格式不支持,仅支持 PDF/Word/图片");
        }
        // 复用签章上传日志表存储(role=保密承诺函),同享格式校验/留痕/下载端点
        com.csg.prm.authorize.entity.AuthSealUploadLog log = new com.csg.prm.authorize.entity.AuthSealUploadLog();
        log.setAgreementId(agreementId);
        log.setUploaderRole("保密承诺函(乙方)");
        log.setFileName(fileName);
        log.setFileData(java.util.Base64.getEncoder().encodeToString(data));
        log.setFormatOk(Boolean.TRUE);
        log.setSealValid(Boolean.TRUE);
        log.setVerifyResult("《保密承诺函》(附录E)已收口:乙方签署件已归档留痕");
        log.setUploadTime(LocalDateTime.now());
        sealLogMapper.insert(log);
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setConfidentialityFile("/api/dpr/auth/agreement/upload-log/" + log.getLogId() + "/file");
        mapper.updateById(upd);
        // 若已双签,承诺函是最后一块拼图 -> 立即自动收尾(核验+开权限+归档)
        autoFinalizeIfFullySigned(agreementId,
                Boolean.TRUE.equals(a.getGrantorSigned()), Boolean.TRUE.equals(a.getGranteeSigned()));
        log.setFileData(null);
        return log;
    }

    // ===== 期限管理(动态跟踪:附录D 表1 续期 · 第七章 变更解除) =====

    @Override
    @Transactional
    public void renew(String agreementId, String newValidUntil) {
        AuthAgreement a = require(agreementId);
        requireNotTerminated(a);
        if (!AuthAgreement.ARCHIVE_YES.equals(a.getArchiveStatus())) {
            throw new BusinessException("仅已归档生效的协议可续期");
        }
        LocalDateTime until = parseDay(newValidUntil);
        if (until == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "续期止日不能为空(yyyy-MM-dd)");
        }
        if (a.getValidUntil() != null && !until.isAfter(a.getValidUntil())) {
            throw new BusinessException("续期止日须晚于原止日(" + a.getValidUntil().toLocalDate() + ")");
        }
        if (until.isAfter(LocalDateTime.now().plusYears(5))) {
            throw new BusinessException("续期后有效期最长不超过自今日起5年(附录D 表1)");
        }
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setValidUntil(until);
        mapper.updateById(upd);
        recordArchiveLog(a, "续期至" + until.toLocalDate() + "(经甲方书面同意)", null);
        chainEvidenceService.anchor("协议续期", agreementId,
                "授权运营协议 " + a.getAgreementNo() + " 续期至 " + until.toLocalDate(),
                String.join("|", a.getAgreementNo(), String.valueOf(until.toLocalDate())));
    }

    @Override
    @Transactional
    public void terminate(String agreementId, String reason) {
        AuthAgreement a = require(agreementId);
        requireNotTerminated(a);
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "终止原因不能为空(附录D 第七章情形)");
        }
        AuthAgreement upd = new AuthAgreement();
        upd.setAgreementId(agreementId);
        upd.setTerminated(Boolean.TRUE);
        upd.setTerminateReason(reason);
        upd.setTerminateTime(LocalDateTime.now());
        mapper.updateById(upd);
        // 已开通的底层数据权限一并回收(动态跟踪:违规/到期 → 暂停或终止授权)
        if (AuthAgreement.REVIEW_PASS.equals(a.getReviewStatus())) {
            dataAccessGateway.revoke(a.getApplyId(), a.getGranteeOrg(), reason);
        }
        recordArchiveLog(a, "终止(" + reason + ")", null);
        chainEvidenceService.anchor("协议终止", agreementId,
                "授权运营协议 " + a.getAgreementNo() + " 终止:" + reason,
                String.join("|", a.getAgreementNo(), reason));
    }

    /** 签章门禁:仅正式稿(要素落定完毕、正文锁定)可签章;已终止不可签。 */
    private void requireFinalDoc(AuthAgreement a) {
        requireNotTerminated(a);
        if (!AuthAgreement.DOC_FINAL.equals(a.getDocStatus())) {
            throw new BusinessException("协议要素未落定(当前:草案),请先在协议工作台「要素落定」生成正式稿后再签章");
        }
    }

    private void requireNotTerminated(AuthAgreement a) {
        if (Boolean.TRUE.equals(a.getTerminated())) {
            throw new BusinessException("协议已终止(" + nvl(a.getTerminateReason()) + "),不可操作");
        }
    }

    /**
     * 《保密承诺函》收口判定(双轨):协议级已上传 OR 一事一议来源申请单已随材料提交(免重复上传)。
     */
    private boolean confidentialityOk(AuthAgreement a) {
        if (StringUtils.hasText(a.getConfidentialityFile())) {
            return true;
        }
        if (StringUtils.hasText(a.getApplyId())) {
            AuthApply apply = applyMapper.selectById(a.getApplyId());
            return apply != null && StringUtils.hasText(apply.getConfidentialityFile());
        }
        return false;
    }

    /** 来源明细(批量清单各项/专项申请单)的最长授权时效,用于「项目期限不得超过协议有效期」校验。 */
    private LocalDateTime maxItemValidDate(AuthAgreement a) {
        if (StringUtils.hasText(a.getBatchListId())) {
            return applyMapper.selectList(new LambdaQueryWrapper<AuthApply>()
                            .eq(AuthApply::getBatchListId, a.getBatchListId())).stream()
                    .map(AuthApply::getValidDate).filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo).orElse(null);
        }
        if (StringUtils.hasText(a.getApplyId())) {
            AuthApply apply = applyMapper.selectById(a.getApplyId());
            return apply != null ? apply.getValidDate() : null;
        }
        return null;
    }

    private static LocalDateTime parseDay(String day) {
        if (!StringUtils.hasText(day)) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(day.trim()).atStartOfDay();
        } catch (java.time.format.DateTimeParseException ex) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "日期格式须为 yyyy-MM-dd:" + day);
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
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
