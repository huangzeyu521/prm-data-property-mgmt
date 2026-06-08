package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthCertTemplate;
import com.csg.prm.authorize.mapper.AuthCertTemplateMapper;
import com.csg.prm.authorize.service.AuthCertTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthCertTemplateServiceImpl implements AuthCertTemplateService {

    private final AuthCertTemplateMapper mapper;

    public AuthCertTemplateServiceImpl(AuthCertTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String create(AuthCertTemplate t) {
        if (!StringUtils.hasText(t.getTemplateName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "模板名称不能为空");
        }
        if (!StringUtils.hasText(t.getCertType())) {
            t.setCertType(AuthCertTemplate.TYPE_SPECIAL);
        }
        t.setTemplateVersion("v1");
        t.setTemplateStatus(AuthCertTemplate.STATUS_ACTIVE);
        mapper.insert(t);
        return t.getTemplateId();
    }

    @Override
    @Transactional
    public void update(AuthCertTemplate t) {
        AuthCertTemplate exist = require(t.getTemplateId());
        t.setTemplateVersion(nextVersion(exist.getTemplateVersion()));
        mapper.updateById(t);
    }

    @Override
    @Transactional
    public void enable(String templateId) {
        require(templateId);
        AuthCertTemplate upd = new AuthCertTemplate();
        upd.setTemplateId(templateId);
        upd.setTemplateStatus(AuthCertTemplate.STATUS_ACTIVE);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void disable(String templateId) {
        require(templateId);
        AuthCertTemplate upd = new AuthCertTemplate();
        upd.setTemplateId(templateId);
        upd.setTemplateStatus(AuthCertTemplate.STATUS_DISABLED);
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AuthCertTemplate> page(long current, long size, String templateName,
                                             String certType, String templateStatus) {
        LambdaQueryWrapper<AuthCertTemplate> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(templateName), AuthCertTemplate::getTemplateName, templateName)
                .eq(StringUtils.hasText(certType), AuthCertTemplate::getCertType, certType)
                .eq(StringUtils.hasText(templateStatus), AuthCertTemplate::getTemplateStatus, templateStatus)
                .orderByDesc(AuthCertTemplate::getCreateTime);
        IPage<AuthCertTemplate> p = mapper.selectPage(
                new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    private AuthCertTemplate require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "模板ID不能为空");
        }
        AuthCertTemplate t = mapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "授权证书模板不存在");
        }
        return t;
    }

    private String nextVersion(String cur) {
        if (cur != null && cur.startsWith("v")) {
            try {
                return "v" + (Integer.parseInt(cur.substring(1)) + 1);
            } catch (NumberFormatException ignored) {
            }
        }
        return "v2";
    }
}
