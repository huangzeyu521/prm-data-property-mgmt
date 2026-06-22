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
    private final com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper;
    private final com.csg.prm.common.ai.DawatAiGateway ai;
    private final com.csg.prm.common.aitrace.AiRunLogService aiRunLogService;

    public AuthMaterialServiceImpl(AuthMaterialMapper mapper,
                                   com.csg.prm.authorize.mapper.AuthApplyMapper applyMapper,
                                   com.csg.prm.common.ai.DawatAiGateway ai,
                                   com.csg.prm.common.aitrace.AiRunLogService aiRunLogService) {
        this.mapper = mapper;
        this.applyMapper = applyMapper;
        this.ai = ai;
        this.aiRunLogService = aiRunLogService;
    }

    /**
     * 授权材料 AI 校验:抽取每份材料正文,连同申请要素交大模型(qwen3-max)逐份校验;
     * Local 桩确定性回退。listByApply 置空 fileData,须按 ID 重取完整记录。
     */
    @Override
    public String aiCheck(String applyId) {
        com.csg.prm.authorize.entity.AuthApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "授权申请不存在");
        }
        List<AuthMaterial> mats = listByApply(applyId);
        if (mats.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "尚未上传材料,无法 AI 校验");
        }
        StringBuilder ctx = new StringBuilder("【申请要素】资产:").append(apply.getAssetName())
                .append('(').append(apply.getAssetId()).append(");被授权方:").append(apply.getGranteeOrg())
                .append(";权益类型:").append(apply.getRightType()).append(";场景:").append(apply.getScenario())
                .append(";范围:").append(apply.getScope())
                .append(";隐私/商密:").append(apply.getSensitiveType() == null ? "无" : apply.getSensitiveType())
                .append('\n');
        for (AuthMaterial m : mats) {
            String text = extractText(mapper.selectById(m.getMaterialId()));
            ctx.append("【材料】名称=").append(m.getMaterialName() == null ? m.getFileName() : m.getMaterialName())
                    .append(";正文=").append(text.isEmpty() ? "(图片/无法抽取正文,需人工核验)"
                            : text.substring(0, Math.min(text.length(), 1200)))
                    .append('\n');
        }
        long t0 = System.currentTimeMillis();
        String result = ai.reviewAuthMaterials(ctx.toString());
        // 逐次留痕(南网全流程留痕追溯):授权材料校验 模型/输入摘要/输出/耗时/SM3/触发人
        aiRunLogService.record(com.csg.prm.common.aitrace.AiRunLog.BIZ_AUTHORIZE, applyId,
                com.csg.prm.common.aitrace.AiRunLog.CAP_AUTH_MATERIAL_CHECK, ai.modelName(),
                "资产:" + apply.getAssetName() + ";被授权方:" + apply.getGranteeOrg() + ";材料 " + mats.size() + " 份",
                result, System.currentTimeMillis() - t0);
        if (!StringUtils.hasText(result)) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "AI 校验暂不可用,请稍后重试");
        }
        return result;
    }

    /** 抽取材料正文:docx 直读 zip 内 document.xml 去标签;PDF/图片留待 OCR 返回空 */
    private String extractText(AuthMaterial m) {
        if (m == null || !StringUtils.hasText(m.getFileData())) {
            return "";
        }
        try {
            byte[] data = Base64.getDecoder().decode(m.getFileData());
            String name = m.getFileName() == null ? "" : m.getFileName().toLowerCase();
            if (name.endsWith(".docx")) {
                try (java.util.zip.ZipInputStream zip =
                             new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(data))) {
                    java.util.zip.ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if ("word/document.xml".equals(entry.getName())) {
                            String xml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                            return xml.replaceAll("<[^>]+>", " ").replaceAll("\s+", " ").trim();
                        }
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
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
