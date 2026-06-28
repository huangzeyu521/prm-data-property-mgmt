package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.dto.CertRenderVO;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCert;
import com.csg.prm.confirm.entity.EquityCertTemplate;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.mapper.EquityCertMapper;
import com.csg.prm.confirm.mapper.EquityCertTemplateMapper;
import com.csg.prm.confirm.service.EquityCertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EquityCertServiceImpl implements EquityCertService {

    private final EquityCertMapper mapper;
    private final EquityCertTemplateMapper templateMapper;
    private final EquityCardMapper cardMapper;

    public EquityCertServiceImpl(EquityCertMapper mapper, EquityCertTemplateMapper templateMapper,
                                 EquityCardMapper cardMapper) {
        this.mapper = mapper;
        this.templateMapper = templateMapper;
        this.cardMapper = cardMapper;
    }

    @Override
    @Transactional
    public String autoIssueForCard(EquityCard card) {
        if (card == null || !StringUtils.hasText(card.getCardId())) {
            return null;
        }
        // 幂等:卡片已签发证书则不重复签
        List<EquityCert> existing = mapper.selectList(new LambdaQueryWrapper<EquityCert>()
                .eq(EquityCert::getCardId, card.getCardId()));
        if (!existing.isEmpty()) {
            return existing.get(0).getCertId();
        }
        // 按权益类型选生效模板(各类权益类型对应格式证书)
        List<EquityCertTemplate> tpls = templateMapper.selectList(new LambdaQueryWrapper<EquityCertTemplate>()
                .eq(EquityCertTemplate::getRightType, card.getRightType())
                .eq(EquityCertTemplate::getTemplateStatus, EquityCertTemplate.STATUS_ACTIVE)
                .orderByDesc(EquityCertTemplate::getCreateTime));
        EquityCertTemplate tpl = tpls.isEmpty() ? null : tpls.get(0);
        return issue(card.getCardId(), null,
                tpl != null ? tpl.getTemplateId() : null,
                tpl != null ? tpl.getTemplateName() : "标准权益证书(默认)");
    }

    @Override
    public CertRenderVO render(String certId) {
        EquityCert cert = getById(certId);
        CertRenderVO vo = new CertRenderVO();
        vo.setCertNo(cert.getCertNo());
        vo.setIssueUnit(cert.getIssueUnit());
        vo.setIssueTime(cert.getIssueTime());
        vo.setCertStatus(cert.getCertStatus());
        vo.setTemplateName(cert.getTemplateName());
        if (StringUtils.hasText(cert.getTemplateId())) {
            EquityCertTemplate tpl = templateMapper.selectById(cert.getTemplateId());
            if (tpl != null) {
                vo.setTemplateContent(tpl.getTemplateContent());
                vo.setTemplateName(tpl.getTemplateName());
            }
        }
        EquityCard card = cardMapper.selectById(cert.getCardId());
        if (card != null) {
            vo.setCardNo(card.getCardNo());
            vo.setAssetId(card.getAssetId());
            vo.setAssetName(card.getAssetName());
            vo.setRightType(card.getRightType());
            vo.setRightOwner(card.getRightOwner());
            vo.setValidDate(card.getValidDate());
        }
        return vo;
    }

    @Override
    @Transactional
    public String issue(String cardId, String issueUnit, String templateId, String templateName) {
        if (!StringUtils.hasText(cardId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "权益卡片ID不能为空");
        }
        EquityCert cert = new EquityCert();
        cert.setCertNo("ZQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        cert.setCardId(cardId);
        cert.setIssueUnit(StringUtils.hasText(issueUnit) ? issueUnit : "中国南方电网有限责任公司");
        cert.setIssueTime(LocalDateTime.now());
        cert.setCertStatus(EquityCert.STATUS_EFFECTIVE);
        cert.setTemplateId(templateId);
        cert.setTemplateName(templateName);
        mapper.insert(cert);
        return cert.getCertId();
    }

    @Override
    public EquityCert getById(String certId) {
        EquityCert c = mapper.selectById(certId);
        if (c == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "证书不存在");
        }
        return c;
    }

    @Override
    @Transactional
    public void revoke(String certId) {
        getById(certId);
        EquityCert upd = new EquityCert();
        upd.setCertId(certId);
        upd.setCertStatus(EquityCert.STATUS_REVOKED);
        mapper.updateById(upd);
    }

    @Override
    public PageResult<EquityCert> page(long current, long size, String cardId) {
        LambdaQueryWrapper<EquityCert> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(cardId), EquityCert::getCardId, cardId)
                .orderByDesc(EquityCert::getCreateTime);
        IPage<EquityCert> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }
}
