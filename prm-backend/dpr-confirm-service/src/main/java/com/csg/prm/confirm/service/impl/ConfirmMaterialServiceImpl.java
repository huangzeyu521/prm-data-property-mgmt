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
import com.csg.prm.confirm.service.ConfirmMaterialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfirmMaterialServiceImpl implements ConfirmMaterialService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png");
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    /** 应交材料预设规则:基础项 + 表2(涉第三方) + A–J 来源/关联识别对应材料(与申请填报清单一致)。 */
    private static final List<String> BASE_REQUIRED = List.of(
            "《表1 数据确权信息清单(系统级)》", "数据确权证明材料(权属/来源凭证)");
    private static final String TABLE2 = "《表2 数据确权信息清单(涉及第三方权益)》";
    private static final Map<String, String> CODE_MATERIAL = new LinkedHashMap<>();

    static {
        CODE_MATERIAL.put("A", "数据来源设备/系统建设投入情况说明");
        CODE_MATERIAL.put("B", "公共采集情况说明(方式/方法/来源)");
        CODE_MATERIAL.put("C", "公共数据授权说明");
        CODE_MATERIAL.put("D", "共享/共同生产情况说明");
        CODE_MATERIAL.put("E", "交易采购情况说明");
        CODE_MATERIAL.put("F", "其他来源情况说明");
        CODE_MATERIAL.put("G", "行政监管要求补充说明");
        CODE_MATERIAL.put("H", "个人/家庭隐私授权说明(如用户入网协议)");
        CODE_MATERIAL.put("I", "第三方商业机密授权说明");
        CODE_MATERIAL.put("J", "其他第三方机构协议");
    }

    private final ConfirmMaterialMapper mapper;
    private final ConfirmApplyService applyService;

    public ConfirmMaterialServiceImpl(ConfirmMaterialMapper mapper, ConfirmApplyService applyService) {
        this.mapper = mapper;
        this.applyService = applyService;
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
        List<String> req = new ArrayList<>(BASE_REQUIRED);
        if (Boolean.TRUE.equals(apply.getInvolvesThirdParty())) {
            req.add(TABLE2);
        }
        addCodes(req, apply.getSourceIdentification());
        addCodes(req, apply.getRelationIdentification());
        return req;
    }

    private void addCodes(List<String> req, String idents) {
        if (!StringUtils.hasText(idents)) {
            return;
        }
        for (String c : idents.split("[,，]")) {
            String mat = CODE_MATERIAL.get(c.trim());
            if (mat != null && !req.contains(mat)) {
                req.add(mat);
            }
        }
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
