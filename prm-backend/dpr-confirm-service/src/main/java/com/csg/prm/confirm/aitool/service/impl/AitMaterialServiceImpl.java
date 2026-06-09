package com.csg.prm.confirm.aitool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import com.csg.prm.confirm.aitool.mapper.AitCompareMapper;
import com.csg.prm.confirm.aitool.mapper.AitMaterialMapper;
import com.csg.prm.confirm.aitool.mapper.AitParseResultMapper;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.aitool.storage.FileStorageGateway;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AitMaterialServiceImpl implements AitMaterialService {

    private static final Set<String> STD_RIGHT_TYPES = Set.of(
            "数据持有权", "数据加工使用权", "数据产品经营权");

    /** 允许的文件格式(#1) */
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "jpg", "jpeg", "png");
    /** 单文件大小下限 100KB、上限 500MB(#1) */
    private static final long MIN_BYTES = 100L * 1024;
    private static final long MAX_BYTES = 500L * 1024 * 1024;
    /** 抽取置信度阈值(#3 对齐可研"准确率≥95%"):低于则标"需人工复核"。 */
    private static final double CONFIDENCE_THRESHOLD = 0.95;

    private final AitMaterialMapper materialMapper;
    private final AitParseResultMapper parseMapper;
    private final AitCompareMapper compareMapper;
    private final AiToolParseGateway parseGateway;
    private final ConfirmApplyMapper applyMapper;
    private final FileStorageGateway storage;

    public AitMaterialServiceImpl(AitMaterialMapper materialMapper, AitParseResultMapper parseMapper,
                                  AitCompareMapper compareMapper, AiToolParseGateway parseGateway,
                                  ConfirmApplyMapper applyMapper, FileStorageGateway storage) {
        this.materialMapper = materialMapper;
        this.parseMapper = parseMapper;
        this.compareMapper = compareMapper;
        this.parseGateway = parseGateway;
        this.applyMapper = applyMapper;
        this.storage = storage;
    }

    @Override
    @Transactional
    public String upload(AitMaterial m) {
        if (m == null || !StringUtils.hasText(m.getFileName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件名不能为空");
        }
        m.setFileType(inferType(m.getFileName()));
        m.setFileHash(Sm3Util.hashHex((m.getFileName() + "|" + (m.getContent() == null ? "" : m.getContent()))));
        if (!StringUtils.hasText(m.getBatchNo())) {
            m.setBatchNo("BATCH-" + m.getFileHash().substring(0, 8).toUpperCase());
        }
        m.setParseStatus(AitMaterial.PARSE_PENDING);
        m.setProgress(0);
        materialMapper.insert(m);
        return m.getMaterialId();
    }

    @Override
    @Transactional
    public String uploadBinary(String fileName, byte[] data, String applyId, String batchNo) {
        if (!StringUtils.hasText(fileName)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件名不能为空");
        }
        if (data == null || data.length == 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件内容为空");
        }
        String ext = extOf(fileName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("不支持的文件格式:" + ext + ",仅支持 PDF/Word/JPG/PNG");
        }
        if (data.length < MIN_BYTES) {
            throw new BizException("文件过小(" + (data.length / 1024) + "KB),单文件不低于 100KB");
        }
        if (data.length > MAX_BYTES) {
            throw new BizException("文件过大(" + (data.length / 1024 / 1024) + "MB),单文件不超过 500MB");
        }
        // 存储二进制
        String storagePath = storage.save(fileName, data);
        // 抽取正文(PDF/Word;图片/旧版doc 留待 OCR,正文暂空)
        String content = extractText(fileName, data);

        AitMaterial m = new AitMaterial();
        m.setFileName(fileName);
        m.setFileType(inferType(fileName));
        m.setSizeKb(Math.max(1L, data.length / 1024L));
        m.setApplyId(applyId);
        m.setStoragePath(storagePath);
        m.setContent(content);
        m.setFileHash(Sm3Util.hashHex(fileName + "|" + data.length + "|" + storagePath));
        m.setBatchNo(StringUtils.hasText(batchNo) ? batchNo
                : "BATCH-" + m.getFileHash().substring(0, 8).toUpperCase());
        m.setParseStatus(AitMaterial.PARSE_PENDING);
        m.setProgress(0);
        materialMapper.insert(m);
        return m.getMaterialId();
    }

    @Override
    public byte[] loadFile(String materialId) {
        AitMaterial m = require(materialId);
        if (!StringUtils.hasText(m.getStoragePath())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该材料无可下载的原件(非真实文件上传)");
        }
        return storage.load(m.getStoragePath());
    }

    /** 抽取文件正文:PDF 用 PDFBox,.docx 用 POI;其余(图片/旧版doc)留待 OCR,返回空。异常优雅降级。 */
    private String extractText(String fileName, byte[] data) {
        String ext = extOf(fileName);
        try {
            if ("pdf".equals(ext)) {
                try (PDDocument doc = PDDocument.load(data)) {
                    String text = new PDFTextStripper().getText(doc);
                    return text == null ? "" : text.trim();
                }
            }
            if ("docx".equals(ext)) {
                try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data));
                     XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                    String text = ex.getText();
                    return text == null ? "" : text.trim();
                }
            }
        } catch (Exception e) {
            // 抽取失败不阻断上传(正文留空,后续可由 OCR/人工补充)
            return "";
        }
        return "";
    }

    private String extOf(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /** 解析超时上限(秒):守卫真实 AI 解析(大瓦特/Qwen)耗时;本地解析瞬时不触发。 */
    private static final long PARSE_TIMEOUT_SEC = 60L;
    /** 异步各阶段间停顿(ms):让 0–100% 进度条对轮询可见地逐级走动。 */
    private static final long STAGE_PAUSE_MS = 240L;
    /** 可抽取正文的格式;抽取为空判定文件损坏/无法解析。 */
    private static final Set<String> TEXT_FORMATS = Set.of("pdf", "doc", "docx");

    @Override
    public void parse(String materialId) {
        // 不加 @Transactional:失败时各状态/进度独立提交,避免抛异常回滚掉"失败"状态(否则停留"待解析")。
        runParse(materialId, false);
    }

    /**
     * 异步解析(#2):分阶段提交进度(各步独立提交,前端轮询到 10→35→65→90→100 渐进),
     * 失败按 格式不支持 / 文件损坏 / 解析超时 分类记录原因。
     * 不加 @Transactional —— 让每步进度/结果写入独立提交,保证进度对轮询实时可见。
     */
    @Override
    @Async
    public void submitParse(String materialId) {
        runParse(materialId, true);
    }

    private void runParse(String materialId, boolean async) {
        AitMaterial m = require(materialId);
        try {
            tick(materialId, async, 10);   // 开始
            // 文件损坏检测:PDF/Word 正文抽取为空 → 损坏或纯图片(需OCR),无法解析
            if (TEXT_FORMATS.contains(extOf(m.getFileName())) && !StringUtils.hasText(m.getContent())) {
                throw new ParseBrokenException("文件损坏或无法解析正文:未能从 " + inferType(m.getFileName())
                        + " 提取到文字,可能文件损坏或为纯图片扫描件(需 OCR)");
            }
            tick(materialId, async, 35);   // 解析中(要素抽取)
            AiToolParseGateway.ParsedElements e = parseWithTimeout(m.getFileName(), m.getContent());
            tick(materialId, async, 65);   // 要素抽取完成
            AitParseResult r = saveResult(materialId, e);
            if (StringUtils.hasText(m.getApplyId())) {
                compareWithForm(r, m.getApplyId());
            }
            tick(materialId, async, 90);   // 表单比对完成
            updateStatus(materialId, AitMaterial.PARSE_SUCCESS, 100, null);
        } catch (RuntimeException ex) {
            String reason = classifyFailure(m, ex);
            updateStatus(materialId, AitMaterial.PARSE_FAILED, 100, reason);
            if (!async) {
                throw new BizException("材料解析失败:" + reason);
            }
        }
    }

    private void tick(String materialId, boolean async, int pct) {
        updateStatus(materialId, AitMaterial.PARSE_RUNNING, pct, null);
        if (async) {
            try {
                Thread.sleep(STAGE_PAUSE_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 解析超时守卫:超过上限取消并抛超时;解析包(PDFBox/POI 等)异常透传以判损坏。 */
    private AiToolParseGateway.ParsedElements parseWithTimeout(String fileName, String content) {
        java.util.concurrent.CompletableFuture<AiToolParseGateway.ParsedElements> f =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> parseGateway.parse(fileName, content));
        try {
            return f.get(PARSE_TIMEOUT_SEC, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException te) {
            f.cancel(true);
            throw new ParseTimeoutException("解析超时(超过 " + PARSE_TIMEOUT_SEC + " 秒),请重试或更换更清晰的材料");
        } catch (java.util.concurrent.ExecutionException ee) {
            Throwable c = ee.getCause();
            if (c instanceof RuntimeException re) {
                throw re;
            }
            throw new ParseBrokenException("文件损坏或无法解析:" + (c == null ? "未知错误" : c.getMessage()));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ParseTimeoutException("解析被中断");
        }
    }

    /** 失败原因分类(#2):格式不支持 / 文件损坏 / 解析超时 / 其他。 */
    private String classifyFailure(AitMaterial m, RuntimeException ex) {
        if (ex instanceof ParseTimeoutException) {
            return ex.getMessage();
        }
        String ext = extOf(m.getFileName());
        if (!ALLOWED_EXT.contains(ext)) {
            return "格式不支持:仅支持 PDF/Word/JPG/PNG(当前 ." + ext + ")";
        }
        if (ex instanceof ParseBrokenException) {
            return ex.getMessage();
        }
        return "文件损坏或无法解析:" + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
    }

    private static final class ParseTimeoutException extends RuntimeException {
        ParseTimeoutException(String msg) { super(msg); }
    }

    private static final class ParseBrokenException extends RuntimeException {
        ParseBrokenException(String msg) { super(msg); }
    }

    @Override
    public AitMaterial getMaterial(String materialId) {
        return require(materialId);
    }

    /** 抽取要素落库(覆盖式:同材料重复解析先清旧),返回解析结果 */
    private AitParseResult saveResult(String materialId, AiToolParseGateway.ParsedElements e) {
        AitParseResult r = new AitParseResult();
        r.setMaterialId(materialId);
        r.setRightSubject(e.rightSubject());
        r.setRightObject(e.rightObject());
        r.setRightType(e.rightType());
        r.setRightTerm(e.rightTerm());
        r.setAuthScope(e.authScope());
        r.setDataSource(e.dataSource());
        r.setSensitiveType(e.sensitiveType());
        r.setSealValid(e.sealValid());
        r.setSealDesc(e.sealDesc());
        r.setConfidence(e.confidence());
        // 置信度 ≥ 阈值(对齐可研"准确率≥95%")→ 自动通过;否则需人工复核
        r.setReviewStatus(e.confidence() >= CONFIDENCE_THRESHOLD ? "自动通过" : "需人工复核");
        parseMapper.delete(new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, materialId));
        parseMapper.insert(r);
        return r;
    }

    @Override
    public byte[] exportParseExcel(String materialId) {
        AitMaterial m = require(materialId);
        AitParseResult r = getParse(materialId);
        List<AitCompare> cmps = compares(materialId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CellStyle head = headStyle(wb);
            // Sheet1 解析要素
            Sheet s1 = wb.createSheet("解析要素");
            writeRow(s1, 0, head, "材料文件", "权利主体", "权利客体", "权利类型", "权利期限",
                    "授权范围", "数据来源", "敏感类型", "印章真伪", "印章说明", "置信度");
            writeRow(s1, 1, null, m.getFileName(), r.getRightSubject(), r.getRightObject(), r.getRightType(),
                    r.getRightTerm(), r.getAuthScope(), r.getDataSource(), r.getSensitiveType(),
                    r.getSealValid(), r.getSealDesc(),
                    r.getConfidence() == null ? "" : String.format("%.0f%%", r.getConfidence() * 100));
            for (int c = 0; c <= 10; c++) {
                s1.setColumnWidth(c, 5000);
            }
            // Sheet2 表单比对差异
            Sheet s2 = wb.createSheet("表单比对差异");
            writeRow(s2, 0, head, "比对字段", "材料解析值", "申请表填写值", "差异类型");
            int ri = 1;
            for (AitCompare c : cmps) {
                writeRow(s2, ri++, null, c.getField(), c.getMaterialValue(), c.getFormValue(), c.getDiffType());
            }
            for (int c = 0; c <= 3; c++) {
                s2.setColumnWidth(c, 6000);
            }
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new BizException("导出解析结果失败:" + ex.getMessage());
        }
    }

    private CellStyle headStyle(Workbook wb) {
        CellStyle st = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        st.setFont(f);
        return st;
    }

    private void writeRow(Sheet sheet, int rowIdx, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(values[i] == null ? "" : values[i]);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private void compareWithForm(AitParseResult r, String applyId) {
        ConfirmApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            return;
        }
        compareMapper.delete(new LambdaQueryWrapper<AitCompare>().eq(AitCompare::getParseId, r.getParseId()));
        addCompare(r, applyId, "权利主体", r.getRightSubject(), apply.getRightHolder());
        addCompare(r, applyId, "权利类型", r.getRightType(), apply.getRightType());
        addCompare(r, applyId, "权利期限", r.getRightTerm(),
                apply.getValidDate() == null ? null : String.valueOf(apply.getValidDate()).substring(0, 10));
        addCompare(r, applyId, "授权范围", r.getAuthScope(), null);
    }

    private void addCompare(AitParseResult r, String applyId, String field, String matVal, String formVal) {
        AitCompare c = new AitCompare();
        c.setParseId(r.getParseId());
        c.setApplyId(applyId);
        c.setField(field);
        c.setMaterialValue(matVal);
        c.setFormValue(formVal);
        c.setDiffType(diff(matVal, formVal));
        compareMapper.insert(c);
    }

    private String diff(String mat, String form) {
        if (!StringUtils.hasText(form)) {
            return AitCompare.DIFF_MISSING;
        }
        if (!StringUtils.hasText(mat)) {
            return AitCompare.DIFF_MISSING;
        }
        return (mat.contains(form) || form.contains(mat)) ? AitCompare.DIFF_MATCH : AitCompare.DIFF_MISMATCH;
    }

    @Override
    public List<TermSuggestion> termCheck(String materialId) {
        AitParseResult r = getParse(materialId);
        List<TermSuggestion> out = new ArrayList<>();
        boolean std = STD_RIGHT_TYPES.contains(r.getRightType());
        out.add(new TermSuggestion("权利类型", r.getRightType(),
                std ? r.getRightType() : normalizeRight(r.getRightType()), std));
        return out;
    }

    private String normalizeRight(String rt) {
        if (rt == null) {
            return "数据持有权";
        }
        if (rt.contains("经营")) {
            return "数据产品经营权";
        }
        if (rt.contains("使用") || rt.contains("加工")) {
            return "数据加工使用权";
        }
        return "数据持有权";
    }

    @Override
    public AitParseResult getParse(String materialId) {
        AitParseResult r = parseMapper.selectOne(
                new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, materialId));
        if (r == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "材料尚未解析或无解析结果");
        }
        return r;
    }

    @Override
    public List<AitCompare> compares(String materialId) {
        AitParseResult r = parseMapper.selectOne(
                new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, materialId));
        if (r == null) {
            return new ArrayList<>();
        }
        return compareMapper.selectList(
                new LambdaQueryWrapper<AitCompare>().eq(AitCompare::getParseId, r.getParseId()));
    }

    @Override
    public PageResult<AitMaterial> page(PageQuery query, String batchNo, String parseStatus, String applyId) {
        LambdaQueryWrapper<AitMaterial> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(batchNo), AitMaterial::getBatchNo, batchNo)
                .eq(StringUtils.hasText(parseStatus), AitMaterial::getParseStatus, parseStatus)
                .eq(StringUtils.hasText(applyId), AitMaterial::getApplyId, applyId)
                .orderByDesc(AitMaterial::getCreateTime);
        IPage<AitMaterial> p = materialMapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    private void updateStatus(String id, String status, Integer progress, String reason) {
        AitMaterial upd = new AitMaterial();
        upd.setMaterialId(id);
        upd.setParseStatus(status);
        upd.setProgress(progress);
        upd.setFailReason(reason);
        materialMapper.updateById(upd);
    }

    private String inferType(String fileName) {
        String f = fileName.toLowerCase();
        if (f.endsWith(".pdf")) {
            return "PDF";
        }
        if (f.endsWith(".doc") || f.endsWith(".docx")) {
            return "WORD";
        }
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) {
            return "JPG";
        }
        if (f.endsWith(".png")) {
            return "PNG";
        }
        return "SCAN";
    }

    private AitMaterial require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "材料ID不能为空");
        }
        AitMaterial m = materialMapper.selectById(id);
        if (m == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "材料不存在");
        }
        return m;
    }
}
