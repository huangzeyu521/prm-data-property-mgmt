package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.authorize.mapper.AuthAgreementMapper;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.mapper.AuthFilingMapper;
import com.csg.prm.authorize.service.AuthFilingService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthFilingServiceImpl implements AuthFilingService {

    private final AuthFilingMapper mapper;
    private final AuthAgreementMapper agreementMapper;
    private final AuthApplyMapper applyMapper;
    private final com.csg.prm.authorize.mapper.AuthMaterialMapper materialMapper;

    public AuthFilingServiceImpl(AuthFilingMapper mapper, AuthAgreementMapper agreementMapper,
                                 AuthApplyMapper applyMapper,
                                 com.csg.prm.authorize.mapper.AuthMaterialMapper materialMapper) {
        this.mapper = mapper;
        this.agreementMapper = agreementMapper;
        this.applyMapper = applyMapper;
        this.materialMapper = materialMapper;
    }

    @Override
    @Transactional
    public String create(AuthFiling filing) {
        if (!StringUtils.hasText(filing.getFilingType())) {
            filing.setFilingType(AuthFiling.TYPE_AUTH); // 兼容存量:缺省附录G授权备案
        }
        // 备案对象=一份已生效经营权授权协议。关联协议时,快照 协议编号/授权期限/被授权方/产权类型,
        // 令备案记录自包含(列表零 join 即显附录G 备案表列),并堵手填与真实授权背离。
        AuthAgreement ag = null;
        if (StringUtils.hasText(filing.getAgreementId())) {
            ag = agreementMapper.selectById(filing.getAgreementId());
            if (ag != null) {
                filing.setAgreementNo(ag.getAgreementNo());
                if (!StringUtils.hasText(filing.getGranteeOrg())) {
                    filing.setGranteeOrg(ag.getGranteeOrg());
                }
                if (!StringUtils.hasText(filing.getApplyId())) {
                    filing.setApplyId(ag.getApplyId());
                }
                // 授权期限:协议要素落定的止日优先(清单级单一真源),次取来源申请单
                if (ag.getValidUntil() != null) {
                    filing.setValidDate(ag.getValidUntil());
                }
                AuthApply apply = StringUtils.hasText(ag.getApplyId()) ? applyMapper.selectById(ag.getApplyId()) : null;
                if (apply != null) {
                    if (filing.getValidDate() == null) {
                        filing.setValidDate(apply.getValidDate());
                    }
                    if (!StringUtils.hasText(filing.getRightType())) {
                        filing.setRightType(apply.getRightType());
                    }
                }
                if (!StringUtils.hasText(filing.getRightType())) {
                    filing.setRightType(ag.getAgreementType());
                }
            }
        }
        if (!StringUtils.hasText(filing.getGranteeOrg())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "被授权方不能为空");
        }
        // 附录F §3.4.6:仅经营权对外授权/产品需备案
        if (!"经营权".equals(filing.getRightType())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "仅经营权对外授权需备案(附录G)");
        }
        if (AuthFiling.TYPE_PRODUCT.equals(filing.getFilingType())) {
            validateProductFiling(filing, ag);
        }
        if (!StringUtils.hasText(filing.getFilingNo())) {
            String prefix = AuthFiling.TYPE_PRODUCT.equals(filing.getFilingType()) ? "CP-" : "BA-";
            filing.setFilingNo(prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        filing.setFilingStatus(AuthFiling.STATUS_PENDING);
        mapper.insert(filing);
        return filing.getFilingId();
    }

    /**
     * 数据产品备案(附录D 附件2 表2)校验:产品要素齐 + 涉及数据表须落在关联协议附件1《数据授权清单》内
     * (备案产品不得使用清单外数据——天然的越权校验)。
     */
    private void validateProductFiling(AuthFiling filing, AuthAgreement ag) {
        if (ag == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "产品备案须关联一份已归档的经营权授权协议(附录D 第四章(三))");
        }
        if (!AuthAgreement.ARCHIVE_YES.equals(ag.getArchiveStatus())) {
            throw new BusinessException("关联协议尚未归档生效,不能进行产品备案");
        }
        if (!StringUtils.hasText(filing.getProductName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "产品名称不能为空(附件2)");
        }
        if (!StringUtils.hasText(filing.getInvolvedTables())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "涉及授权数据表不能为空(附件2,多选)");
        }
        // 协议覆盖的数据表集合:批量=清单各项;专项=来源申请单
        java.util.Set<String> allowed = new java.util.HashSet<>();
        if (StringUtils.hasText(ag.getBatchListId())) {
            applyMapper.selectList(new LambdaQueryWrapper<AuthApply>()
                            .eq(AuthApply::getBatchListId, ag.getBatchListId()))
                    .forEach(a -> allowed.add(a.getAssetName()));
        } else if (StringUtils.hasText(ag.getApplyId())) {
            AuthApply apply = applyMapper.selectById(ag.getApplyId());
            if (apply != null) {
                allowed.add(apply.getAssetName());
            }
        }
        java.util.List<String> outside = java.util.Arrays.stream(filing.getInvolvedTables().split("[、,;，；]"))
                .map(String::trim).filter(StringUtils::hasText)
                .filter(t -> !allowed.contains(t))
                .toList();
        if (!outside.isEmpty()) {
            throw new BusinessException("以下数据表不在协议附件1《数据授权清单》内,产品不得使用清单外数据:"
                    + String.join("、", outside));
        }
    }

    @Override
    @Transactional
    public void file(String filingId) {
        AuthFiling f = require(filingId);
        if (!AuthFiling.STATUS_PENDING.equals(f.getFilingStatus())) {
            throw new BusinessException("仅待备案记录可完成备案");
        }
        // 产品备案门禁:须已上传《安全合规评审意见》附件(附件2 要求提供评审意见;材料挂 filingId)
        if (AuthFiling.TYPE_PRODUCT.equals(f.getFilingType())) {
            Long cnt = materialMapper.selectCount(
                    new LambdaQueryWrapper<com.csg.prm.authorize.entity.AuthMaterial>()
                            .eq(com.csg.prm.authorize.entity.AuthMaterial::getApplyId, filingId));
            if (cnt == null || cnt == 0) {
                throw new BusinessException("产品备案须先上传《安全合规评审意见》附件(附件2),再完成备案");
            }
        }
        AuthFiling upd = new AuthFiling();
        upd.setFilingId(filingId);
        upd.setFilingStatus(AuthFiling.STATUS_FILED);
        upd.setFilingTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AuthFiling> page(long current, long size, String filingStatus, String filingType) {
        LambdaQueryWrapper<AuthFiling> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(filingStatus), AuthFiling::getFilingStatus, filingStatus)
                .orderByDesc(AuthFiling::getCreateTime);
        if (StringUtils.hasText(filingType)) {
            if (AuthFiling.TYPE_AUTH.equals(filingType)) {
                // 存量记录 filingType 为空,按授权备案归类
                w.and(q -> q.eq(AuthFiling::getFilingType, filingType).or().isNull(AuthFiling::getFilingType));
            } else {
                w.eq(AuthFiling::getFilingType, filingType);
            }
        }
        IPage<AuthFiling> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    private AuthFiling require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "备案ID不能为空");
        }
        AuthFiling f = mapper.selectById(id);
        if (f == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "备案记录不存在");
        }
        return f;
    }
}
