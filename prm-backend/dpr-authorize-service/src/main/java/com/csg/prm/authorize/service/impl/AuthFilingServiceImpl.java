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

    public AuthFilingServiceImpl(AuthFilingMapper mapper, AuthAgreementMapper agreementMapper,
                                 AuthApplyMapper applyMapper) {
        this.mapper = mapper;
        this.agreementMapper = agreementMapper;
        this.applyMapper = applyMapper;
    }

    @Override
    @Transactional
    public String create(AuthFiling filing) {
        // 附录G:备案对象=一份已生效经营权授权协议。关联协议时,快照 协议编号/授权期限/被授权方/产权类型,
        // 令备案记录自包含(列表零 join 即显附录G 备案表列),并堵手填与真实授权背离。
        if (StringUtils.hasText(filing.getAgreementId())) {
            AuthAgreement ag = agreementMapper.selectById(filing.getAgreementId());
            if (ag != null) {
                filing.setAgreementNo(ag.getAgreementNo());
                if (!StringUtils.hasText(filing.getGranteeOrg())) {
                    filing.setGranteeOrg(ag.getGranteeOrg());
                }
                if (!StringUtils.hasText(filing.getApplyId())) {
                    filing.setApplyId(ag.getApplyId());
                }
                AuthApply apply = StringUtils.hasText(ag.getApplyId()) ? applyMapper.selectById(ag.getApplyId()) : null;
                if (apply != null) {
                    filing.setValidDate(apply.getValidDate());
                    if (!StringUtils.hasText(filing.getRightType())) {
                        filing.setRightType(apply.getRightType());
                    }
                }
            }
        }
        if (!StringUtils.hasText(filing.getGranteeOrg())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "被授权方不能为空");
        }
        // 附录F §3.4.6:仅数据产品经营权对外授权需备案
        if (!"数据产品经营权".equals(filing.getRightType())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "仅数据产品经营权对外授权需备案(附录G)");
        }
        if (!StringUtils.hasText(filing.getFilingNo())) {
            filing.setFilingNo("BA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }
        filing.setFilingStatus(AuthFiling.STATUS_PENDING);
        mapper.insert(filing);
        return filing.getFilingId();
    }

    @Override
    @Transactional
    public void file(String filingId) {
        AuthFiling f = require(filingId);
        if (!AuthFiling.STATUS_PENDING.equals(f.getFilingStatus())) {
            throw new BusinessException("仅待备案记录可完成备案");
        }
        AuthFiling upd = new AuthFiling();
        upd.setFilingId(filingId);
        upd.setFilingStatus(AuthFiling.STATUS_FILED);
        upd.setFilingTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AuthFiling> page(long current, long size, String filingStatus) {
        LambdaQueryWrapper<AuthFiling> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(filingStatus), AuthFiling::getFilingStatus, filingStatus)
                .orderByDesc(AuthFiling::getCreateTime);
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
