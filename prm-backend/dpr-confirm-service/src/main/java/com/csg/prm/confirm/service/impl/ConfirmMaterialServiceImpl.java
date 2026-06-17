package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.dto.MaterialCheckReport;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import com.csg.prm.confirm.mapper.ConfirmMaterialMapper;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmMaterialRuleService;
import com.csg.prm.confirm.service.ConfirmMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfirmMaterialServiceImpl implements ConfirmMaterialService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png");
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    private final ConfirmMaterialMapper mapper;
    private final ConfirmApplyService applyService;
    private final ConfirmMaterialRuleService ruleService;
    private final com.csg.prm.confirm.aitool.gateway.AiToolParseGateway aiGateway;

    public ConfirmMaterialServiceImpl(ConfirmMaterialMapper mapper, ConfirmApplyService applyService,
                                      ConfirmMaterialRuleService ruleService,
                                      com.csg.prm.confirm.aitool.gateway.AiToolParseGateway aiGateway) {
        this.mapper = mapper;
        this.applyService = applyService;
        this.ruleService = ruleService;
        this.aiGateway = aiGateway;
    }

    /**
     * 材料 AI 校验:抽取每份材料正文,连同申请要素交大模型(qwen3-max)逐份校验
     * 完整性/合规性/与表单一致性;模型不可用时回退规则桩。返回严格 JSON 字符串。
     */
    @Override
    public String aiCheck(String applyId) {
        ConfirmApply apply = applyService.getById(applyId);
        List<ConfirmMaterial> mats = listByApply(applyId);
        if (mats.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "尚未上传材料,无法 AI 校验");
        }
        StringBuilder ctx = new StringBuilder("【申请要素】资产:").append(apply.getAssetName())
                .append('(').append(apply.getAssetId()).append(");权属类型:").append(apply.getRightType())
                .append(";申报主体:").append(apply.getRightHolder())
                .append(";管制属性:").append(apply.getRegulated() == null ? "未填" : apply.getRegulated())
                .append(";登记类型:").append(apply.getRegisterType()).append('\n');
        for (ConfirmMaterial m : mats) {
            // listByApply 出于响应瘦身置空了 fileData,抽正文须按 ID 重取完整记录
            String text = extractText(mapper.selectById(m.getMaterialId()));
            ctx.append("【材料】名称=").append(m.getMaterialName())
                    .append(";类型=").append(m.getMaterialType())
                    .append(";正文=").append(text.isEmpty() ? "(图片/无法抽取正文,需人工核验)"
                            : text.substring(0, Math.min(text.length(), 1200)))
                    .append('\n');
        }
        String result = aiGateway.reviewMaterials(ctx.toString());
        if (!StringUtils.hasText(result)) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "AI 校验暂不可用,请稍后重试或走规则校验");
        }
        return result;
    }

    /** 抽取材料正文(PDF/docx;图片与旧版doc留待OCR返回空),异常优雅降级 */
    private String extractText(ConfirmMaterial m) {
        if (!StringUtils.hasText(m.getFileData())) {
            return "";
        }
        try {
            byte[] data = Base64.getDecoder().decode(m.getFileData());
            String url = m.getFileName() == null ? "" : m.getFileName().toLowerCase();
            if (url.endsWith(".pdf")) {
                try (org.apache.pdfbox.pdmodel.PDDocument doc = org.apache.pdfbox.pdmodel.PDDocument.load(data)) {
                    String t = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
                    return t == null ? "" : t.trim();
                }
            }
            if (url.endsWith(".docx")) {
                // 直读 zip 内 word/document.xml 去标签(不依赖 POI 包关系校验,对精简 docx 更鲁棒)
                try (java.util.zip.ZipInputStream zip =
                             new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(data))) {
                    java.util.zip.ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        if ("word/document.xml".equals(entry.getName())) {
                            String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                            return xml.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
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
    public String uploadFile(ConfirmMaterial meta, String fileName, byte[] data) {
        if (meta == null || !StringUtils.hasText(meta.getApplyId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "关联申请ID不能为空");
        }
        if (data == null || data.length == 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "上传文件为空");
        }
        // 格式验证:扩展名 + 大小
        String ext = extOf(fileName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("不支持的文件格式:" + ext + ",仅支持 PDF/Word/JPG/PNG");
        }
        if (data.length > MAX_BYTES) {
            throw new BizException("文件过大(" + (data.length / 1024 / 1024) + "MB),单文件不超过 50MB");
        }
        meta.setFileName(fileName);
        meta.setFileData(Base64.getEncoder().encodeToString(data));
        meta.setUploadTime(LocalDateTime.now());
        meta.setCheckResult(ConfirmMaterial.CHECK_PASS);
        meta.setAbnormalDesc("格式校验通过(" + ext.toUpperCase() + "," + (data.length / 1024) + "KB)");
        mapper.insert(meta);
        // 回填下载地址(insert 后才有 id)
        ConfirmMaterial upd = new ConfirmMaterial();
        upd.setMaterialId(meta.getMaterialId());
        upd.setFileUrl("/api/dpr/confirm/material/" + meta.getMaterialId() + "/file");
        mapper.updateById(upd);
        return meta.getMaterialId();
    }

    @Override
    public byte[] download(String materialId) {
        ConfirmMaterial m = mapper.selectById(materialId);
        if (m == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "材料不存在");
        }
        if (!StringUtils.hasText(m.getFileData())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该材料无可预览/下载的原件");
        }
        return Base64.getDecoder().decode(m.getFileData());
    }

    private String extOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int i = fileName.lastIndexOf('.');
        return i < 0 ? "" : fileName.substring(i + 1).toLowerCase();
    }

    @Override
    @Transactional
    public String upload(ConfirmMaterial m) {
        if (!StringUtils.hasText(m.getApplyId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "关联申请ID不能为空");
        }
        if (!StringUtils.hasText(m.getMaterialName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "材料名称不能为空");
        }
        m.setUploadTime(LocalDateTime.now());
        m.setCheckResult(ConfirmMaterial.CHECK_PENDING);
        mapper.insert(m);
        return m.getMaterialId();
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
    public List<ConfirmMaterial> listByApply(String applyId) {
        LambdaQueryWrapper<ConfirmMaterial> w = new LambdaQueryWrapper<>();
        w.eq(ConfirmMaterial::getApplyId, applyId).orderByDesc(ConfirmMaterial::getUploadTime);
        List<ConfirmMaterial> list = mapper.selectList(w);
        list.forEach(m -> m.setFileData(null));
        return list;
    }

    @Override
    @Transactional
    public void check(String materialId, boolean pass, String abnormalDesc) {
        ConfirmMaterial m = mapper.selectById(materialId);
        if (m == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "材料不存在");
        }
        ConfirmMaterial upd = new ConfirmMaterial();
        upd.setMaterialId(materialId);
        upd.setCheckResult(pass ? ConfirmMaterial.CHECK_PASS : ConfirmMaterial.CHECK_FAIL);
        upd.setAbnormalDesc(pass ? null : abnormalDesc);
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public MaterialCheckReport runCheck(String applyId) {
        ConfirmApply apply = applyService.getById(applyId);
        List<String> required = requiredMaterials(apply);
        List<ConfirmMaterial> mats = mapper.selectList(
                new LambdaQueryWrapper<ConfirmMaterial>().eq(ConfirmMaterial::getApplyId, applyId));
        Map<String, ConfirmMaterial> byName = mats.stream()
                .collect(Collectors.toMap(ConfirmMaterial::getMaterialName, m -> m, (a, b) -> a));

        List<String> missing = new ArrayList<>();
        List<String> nonCompliant = new ArrayList<>();
        int pass = 0;
        for (String r : required) {
            ConfirmMaterial m = byName.get(r);
            if (m == null) {
                missing.add(r);
            } else if (StringUtils.hasText(m.getFileName())) {
                updateCheck(m.getMaterialId(), ConfirmMaterial.CHECK_PASS, "完整性+格式合规");
                pass++;
            } else {
                updateCheck(m.getMaterialId(), ConfirmMaterial.CHECK_FAIL, "未上传真实原件(仅占位登记)");
                nonCompliant.add(r + "(缺真实原件)");
            }
        }

        MaterialCheckReport rep = new MaterialCheckReport();
        rep.setApplyId(applyId);
        rep.setRequiredCount(required.size());
        rep.setUploadedCount(mats.size());
        rep.setPassCount(pass);
        rep.setFailCount(nonCompliant.size());
        rep.setMissing(missing);
        rep.setNonCompliant(nonCompliant);
        rep.setAllPass(missing.isEmpty() && nonCompliant.isEmpty());
        rep.setSummary("应交 " + required.size() + " 项,已交 " + mats.size() + ",通过 " + pass
                + ",缺失 " + missing.size() + ",不合规 " + nonCompliant.size()
                + (rep.isAllPass() ? " — 完整且合规,可推送审核" : " — 存在缺失/不合规,补齐后方可推送审核"));
        return rep;
    }

    @Override
    @Transactional
    public void pushReview(String applyId) {
        MaterialCheckReport rep = runCheck(applyId);
        if (!rep.isAllPass()) {
            throw new BizException("材料校验未通过,无法推送审核。缺失:" + rep.getMissing()
                    + ";不合规:" + rep.getNonCompliant());
        }
        applyService.submit(applyId);
    }

    @Override
    public byte[] exportCheck(String applyId) {
        List<ConfirmMaterial> mats = listByApply(applyId);
        StringBuilder sb = new StringBuilder("﻿"); // UTF-8 BOM,Excel 正常显示中文
        sb.append("材料名称,类型,校验结果,异常说明,原件文件名\n");
        for (ConfirmMaterial m : mats) {
            sb.append(csv(m.getMaterialName())).append(',')
                    .append(csv(m.getMaterialType())).append(',')
                    .append(csv(m.getCheckResult())).append(',')
                    .append(csv(m.getAbnormalDesc())).append(',')
                    .append(csv(m.getFileName())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<String> requiredMaterials(ConfirmApply apply) {
        return ruleService.requiredNames(apply);
    }

    private void updateCheck(String materialId, String result, String desc) {
        ConfirmMaterial upd = new ConfirmMaterial();
        upd.setMaterialId(materialId);
        upd.setCheckResult(result);
        upd.setAbnormalDesc(desc);
        mapper.updateById(upd);
    }

    private String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @Override
    public PageResult<ConfirmMaterial> page(long current, long size, String applyId, String checkResult) {
        LambdaQueryWrapper<ConfirmMaterial> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(applyId), ConfirmMaterial::getApplyId, applyId)
                .eq(StringUtils.hasText(checkResult), ConfirmMaterial::getCheckResult, checkResult)
                .orderByDesc(ConfirmMaterial::getUploadTime);
        IPage<ConfirmMaterial> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }
}
