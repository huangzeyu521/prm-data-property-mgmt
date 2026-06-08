package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthApplyTemplate;
import com.csg.prm.authorize.mapper.AuthApplyTemplateMapper;
import com.csg.prm.authorize.service.AuthApplyTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthApplyTemplateServiceImpl implements AuthApplyTemplateService {

    private final AuthApplyTemplateMapper mapper;

    public AuthApplyTemplateServiceImpl(AuthApplyTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String create(AuthApplyTemplate t) {
        if (!StringUtils.hasText(t.getTemplateName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "模板名称不能为空");
        }
        if (!StringUtils.hasText(t.getAuthType())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "授权类型不能为空");
        }
        t.setTemplateVersion("v1");
        t.setTemplateStatus(AuthApplyTemplate.STATUS_ACTIVE);
        mapper.insert(t);
        return t.getTemplateId();
    }

    @Override
    @Transactional
    public void update(AuthApplyTemplate t) {
        AuthApplyTemplate exist = require(t.getTemplateId());
        t.setTemplateVersion(nextVersion(exist.getTemplateVersion()));
        mapper.updateById(t);
    }

    @Override
    @Transactional
    public void delete(String templateId) {
        require(templateId);
        mapper.deleteById(templateId);
    }

    @Override
    @Transactional
    public void enable(String templateId) {
        require(templateId);
        AuthApplyTemplate upd = new AuthApplyTemplate();
        upd.setTemplateId(templateId);
        upd.setTemplateStatus(AuthApplyTemplate.STATUS_ACTIVE);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void disable(String templateId) {
        require(templateId);
        AuthApplyTemplate upd = new AuthApplyTemplate();
        upd.setTemplateId(templateId);
        upd.setTemplateStatus(AuthApplyTemplate.STATUS_DISABLED);
        mapper.updateById(upd);
    }

    @Override
    public AuthApplyTemplate getById(String templateId) {
        return require(templateId);
    }

    @Override
    public PageResult<AuthApplyTemplate> page(long current, long size, String templateName, String authType, String templateStatus) {
        LambdaQueryWrapper<AuthApplyTemplate> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(templateName), AuthApplyTemplate::getTemplateName, templateName)
                .eq(StringUtils.hasText(authType), AuthApplyTemplate::getAuthType, authType)
                .eq(StringUtils.hasText(templateStatus), AuthApplyTemplate::getTemplateStatus, templateStatus)
                .orderByDesc(AuthApplyTemplate::getCreateTime);
        IPage<AuthApplyTemplate> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    private AuthApplyTemplate require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "模板ID不能为空");
        }
        AuthApplyTemplate t = mapper.selectById(id);
        if (t == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "申请模板不存在");
        }
        return t;
    }

    private String nextVersion(String cur) {
        if (cur != null && cur.startsWith("v")) {
            try {
                return "v" + (Integer.parseInt(cur.substring(1)) + 1);
            } catch (NumberFormatException ignored) {
                // 非 vN 版本号忽略
            }
        }
        return "v2";
    }
}
