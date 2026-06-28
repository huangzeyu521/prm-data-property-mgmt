package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthAgreementTemplate;
import com.csg.prm.authorize.mapper.AuthAgreementTemplateMapper;
import com.csg.prm.authorize.service.AuthAgreementTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Set;

@Service
public class AuthAgreementTemplateServiceImpl implements AuthAgreementTemplateService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "png", "jpg", "jpeg");
    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private final AuthAgreementTemplateMapper mapper;

    public AuthAgreementTemplateServiceImpl(AuthAgreementTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String create(AuthAgreementTemplate t) {
        if (!StringUtils.hasText(t.getTemplateName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "模板名称不能为空");
        }
        t.setTemplateVersion("v1");
        t.setTemplateStatus(AuthAgreementTemplate.STATUS_ACTIVE);
        mapper.insert(t);
        return t.getTemplateId();
    }

    @Override
    @Transactional
    public void update(AuthAgreementTemplate t) {
        AuthAgreementTemplate exist = require(t.getTemplateId());
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
        AuthAgreementTemplate upd = new AuthAgreementTemplate();
        upd.setTemplateId(templateId);
        upd.setTemplateStatus(AuthAgreementTemplate.STATUS_ACTIVE);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void disable(String templateId) {
        require(templateId);
        AuthAgreementTemplate upd = new AuthAgreementTemplate();
        upd.setTemplateId(templateId);
        upd.setTemplateStatus(AuthAgreementTemplate.STATUS_DISABLED);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void uploadFile(String templateId, String fileName, byte[] data) {
        require(templateId);
        if (!StringUtils.hasText(fileName) || data == null || data.length == 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "套版文件为空");
        }
        if (data.length > MAX_BYTES) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "文件超过 20MB 上限");
        }
        if (!ALLOWED_EXT.contains(extOf(fileName))) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "仅支持 PDF/Word/图片 套版文件");
        }
        AuthAgreementTemplate upd = new AuthAgreementTemplate();
        upd.setTemplateId(templateId);
        upd.setFileName(fileName);
        upd.setFileData(Base64.getEncoder().encodeToString(data));
        mapper.updateById(upd);
    }

    @Override
    public byte[] download(String templateId) {
        AuthAgreementTemplate t = require(templateId);
        if (!StringUtils.hasText(t.getFileData())) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "该模板未上传套版文件");
        }
        return Base64.getDecoder().decode(t.getFileData());
    }

    @Override
    public AuthAgreementTemplate getById(String templateId) {
        return require(templateId);
    }

    @Override
    public PageResult<AuthAgreementTemplate> page(long current, long size, String templateName, String authType,
                                                  String purpose, String status) {
        LambdaQueryWrapper<AuthAgreementTemplate> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(templateName), AuthAgreementTemplate::getTemplateName, templateName)
                .eq(StringUtils.hasText(authType), AuthAgreementTemplate::getAuthType, authType)
                .eq(StringUtils.hasText(purpose), AuthAgreementTemplate::getPurpose, purpose)
                .eq(StringUtils.hasText(status), AuthAgreementTemplate::getTemplateStatus, status)
                .orderByDesc(AuthAgreementTemplate::getCreateTime);
        IPage<AuthAgreementTemplate> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        p.getRecords().forEach(t -> t.setFileData(null)); // 列表不回传重负载
        return PageResult.of(p);
    }

    private AuthAgreementTemplate require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "模板ID不能为空");
        }
        AuthAgreementTemplate t = mapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "协议模板不存在");
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

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase() : "";
    }
}
