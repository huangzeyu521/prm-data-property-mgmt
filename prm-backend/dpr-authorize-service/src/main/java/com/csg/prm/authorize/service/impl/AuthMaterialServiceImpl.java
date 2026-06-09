package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.authorize.entity.AuthMaterial;
import com.csg.prm.authorize.mapper.AuthMaterialMapper;
import com.csg.prm.authorize.service.AuthMaterialService;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;

@Service
public class AuthMaterialServiceImpl implements AuthMaterialService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png");
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private final AuthMaterialMapper mapper;

    public AuthMaterialServiceImpl(AuthMaterialMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String uploadFile(AuthMaterial meta, String fileName, byte[] data) {
        if (meta == null || !StringUtils.hasText(meta.getApplyId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "缺少关联申请ID");
        }
        if (!StringUtils.hasText(fileName) || data == null || data.length == 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "上传文件为空");
        }
        if (data.length > MAX_BYTES) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件超过 50MB 上限");
        }
        if (!ALLOWED_EXT.contains(extOf(fileName))) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "仅支持 PDF/Word/图片 材料");
        }
        meta.setFileName(fileName);
        meta.setFileData(Base64.getEncoder().encodeToString(data));
        meta.setUploadTime(LocalDateTime.now());
        if (!StringUtils.hasText(meta.getMaterialName())) {
            meta.setMaterialName(fileName);
        }
        mapper.insert(meta);
        return meta.getMaterialId();
    }

    @Override
    public byte[] download(String materialId) {
        AuthMaterial m = getById(materialId);
        if (!StringUtils.hasText(m.getFileData())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该材料无原件");
        }
        return Base64.getDecoder().decode(m.getFileData());
    }

    @Override
    public AuthMaterial getById(String materialId) {
        AuthMaterial m = mapper.selectById(materialId);
        if (m == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "材料不存在");
        }
        return m;
    }

    @Override
    @Transactional
    public void delete(String materialId) {
        if (mapper.selectById(materialId) == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "材料不存在");
        }
        mapper.deleteById(materialId);
    }

    @Override
    public List<AuthMaterial> listByApply(String applyId) {
        List<AuthMaterial> list = mapper.selectList(new LambdaQueryWrapper<AuthMaterial>()
                .eq(AuthMaterial::getApplyId, applyId)
                .orderByDesc(AuthMaterial::getUploadTime));
        list.forEach(m -> m.setFileData(null));
        return list;
    }

    private String extOf(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1).toLowerCase() : "";
    }
}
