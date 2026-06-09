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

    private static final java.util.Set<String> ALLOWED_EXT = java.util.Set.of("pdf", "doc", "docx", "png", "jpg", "jpeg");
    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private final AuthCertTemplateMapper mapper;

    public AuthCertTemplateServiceImpl(AuthCertTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void delete(String templateId) {
        require(templateId);
        mapper.deleteById(templateId);
    }

    @Override
    @Transactional
    public void uploadFile(String templateId, String fileName, byte[] data) {
        require(templateId);
        if (!StringUtils.hasText(fileName) || data == null || data.length == 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "套版文件为空");
        }
        if (data.length > MAX_BYTES) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件超过 20MB 上限");
        }
        if (!ALLOWED_EXT.contains(extOf(fileName))) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "仅支持 PDF/Word/图片 套版文件");
        }
        AuthCertTemplate upd = new AuthCertTemplate();
        upd.setTemplateId(templateId);
        upd.setFileName(fileName);
        upd.setFileData(java.util.Base64.getEncoder().encodeToString(data));
        mapper.updateById(upd);
    }

    @Override
    public byte[] download(String templateId) {
        AuthCertTemplate t = require(templateId);
        if (!StringUtils.hasText(t.getFileData())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该模板未上传套版文件");
        }
        return java.util.Base64.getDecoder().decode(t.getFileData());
    }

    @Override
    public AuthCertTemplate getById(String templateId) {
        return require(templateId);
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase() : "";
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
        p.getRecords().forEach(t -> t.setFileData(null)); // 列表不回传重负载
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
