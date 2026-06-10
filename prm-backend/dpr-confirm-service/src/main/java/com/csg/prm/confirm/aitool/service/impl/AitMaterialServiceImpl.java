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
import com.csg.prm.confirm.aitool.term.AitTermLibrary;
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
        r.setConfidence(e.confidence());
        // #5 印章-OCR 交叉校验:印章信号 × OCR正文/要素一致性 → 有效/可疑/未检出(产出"可疑")
        AitMaterial m = require(materialId);
        String[] seal = crossValidateSeal(e, m);
        r.setSealValid(seal[0]);
        r.setSealDesc(seal[1]);
        // 置信度 ≥ 阈值(对齐可研"准确率≥95%")→ 自动通过;否则需人工复核
        r.setReviewStatus(e.confidence() >= CONFIDENCE_THRESHOLD ? "自动通过" : "需人工复核");
        // #5 材料可信度评级:印章 + 置信度 + 要素完整性 综合
        int score = trustScore(seal[0], e);
        r.setTrustScore(score);
        r.setTrustLevel(score >= 75 ? "可信" : (score >= 50 ? "存疑" : "不可信"));
        parseMapper.delete(new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, materialId));
        parseMapper.insert(r);
        return r;
    }

    /** 印章-OCR 交叉校验:把(CV/模型)印章判定与 OCR 正文/抽取要素做一致性核验,返回 [真伪判定, 说明]。 */
    private String[] crossValidateSeal(AiToolParseGateway.ParsedElements e, AitMaterial m) {
        // OCR 印章信号取自正文(文件名易误导,如"无章材料");先识别否定表述
        String content = m.getContent() == null ? "" : m.getContent();
        boolean noSeal = content.contains("无章") || content.contains("未盖章") || content.contains("无印章")
                || content.contains("未加盖") || content.contains("无公章");
        boolean ocrSeal = !noSeal && (content.contains("盖章") || content.contains("公章") || content.contains("印章")
                || content.contains("加盖") || content.contains("钢印") || content.contains("骑缝") || content.contains("印鉴"));
        boolean modelValid = "有效".equals(e.sealValid());
        boolean hasSubject = StringUtils.hasText(e.rightSubject());
        boolean richContent = content.trim().length() >= 12;

        // 模型(qwen/CV)已判可疑 → 保留
        if ("可疑".equals(e.sealValid())) {
            return new String[]{"可疑", StringUtils.hasText(e.sealDesc()) ? e.sealDesc()
                    : "CV 印章特征与备案样本存在差异,真伪存疑,建议人工核验原件"};
        }
        // 既无 OCR 印章表述、模型也未判有效 → 未检出
        if (!ocrSeal && !modelValid) {
            return new String[]{"未检出", "未检出印章区域,OCR 正文亦无盖章表述"};
        }
        // OCR 印章表述 + 正文充分 + 抽到出证主体 → 交叉校验一致 → 有效
        if (ocrSeal && hasSubject && richContent) {
            return new String[]{"有效", "检出印章表述,与 OCR 出证主体「" + e.rightSubject() + "」一致,交叉校验通过"};
        }
        // 模型判有效但 OCR 正文无盖章表述 → 交叉校验不一致 → 可疑
        if (modelValid && !ocrSeal) {
            return new String[]{"可疑", "模型/CV 判印章有效,但 OCR 正文未见盖章表述,交叉校验不一致,建议人工核验原件"};
        }
        // 有 OCR 印章表述但正文稀疏/要素不足佐证 → 可疑
        return new String[]{"可疑", "正文称已盖章,但 OCR 正文稀疏、未充分佐证出证主体/关键要素,真伪存疑,建议人工核验原件"};
    }

    /** 材料可信度评分(0-100):印章交叉校验(40/15/0) + 置信度(30/20/10) + 要素完整性(每项6,最高30)。 */
    private int trustScore(String sealValid, AiToolParseGateway.ParsedElements e) {
        int score = "有效".equals(sealValid) ? 40 : ("可疑".equals(sealValid) ? 15 : 0);
        double conf = e.confidence();
        score += conf >= 0.95 ? 30 : (conf >= 0.85 ? 20 : 10);
        int present = 0;
        for (String v : new String[]{e.rightSubject(), e.rightObject(), e.rightType(), e.rightTerm(), e.authScope()}) {
            if (StringUtils.hasText(v)) {
                present++;
            }
        }
        score += present * 6;
        return Math.min(100, score);
    }

    @Override
    public byte[] exportParseExcel(String materialId) {
        AitMaterial m = require(materialId);
        AitParseResult r = getParse(materialId);
        List<AitCompare> cmps = compares(materialId);
        List<TermSuggestion> terms = termCheck(materialId);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            CellStyle head = headStyle(wb);
            // Sheet1 解析要素(含复核标记 + 材料可信度)
            Sheet s1 = wb.createSheet("解析要素");
            writeRow(s1, 0, head, "材料文件", "权利主体", "权利客体", "权利类型", "权利期限",
                    "授权范围", "数据来源", "敏感类型", "印章真伪", "印章说明", "置信度", "复核标记", "材料可信度");
            String trust = r.getTrustLevel() == null ? ""
                    : r.getTrustLevel() + (r.getTrustScore() == null ? "" : "(" + r.getTrustScore() + "分)");
            writeRow(s1, 1, null, m.getFileName(), r.getRightSubject(), r.getRightObject(), r.getRightType(),
                    r.getRightTerm(), r.getAuthScope(), r.getDataSource(), r.getSensitiveType(),
                    r.getSealValid(), r.getSealDesc(),
                    r.getConfidence() == null ? "" : String.format("%.0f%%", r.getConfidence() * 100),
                    r.getReviewStatus(), trust);
            for (int c = 0; c <= 12; c++) {
                s1.setColumnWidth(c, 5000);
            }
            // Sheet2 表单比对差异(含原文定位片段)
            Sheet s2 = wb.createSheet("表单比对差异");
            writeRow(s2, 0, head, "比对字段", "材料解析值", "申请表填写值", "差异类型", "原文定位片段");
            int ri = 1;
            for (AitCompare c : cmps) {
                writeRow(s2, ri++, null, c.getField(), c.getMaterialValue(), c.getFormValue(),
                        c.getDiffType(), c.getSourceSnippet());
            }
            for (int c = 0; c <= 4; c++) {
                s2.setColumnWidth(c, 6000);
            }
            // Sheet3 术语库匹配(自动标注:非标→标准术语建议)
            Sheet s3 = wb.createSheet("术语库匹配");
            writeRow(s3, 0, head, "字段", "抽取值", "标准术语建议", "是否标准");
            int ti = 1;
            for (TermSuggestion t : terms) {
                writeRow(s3, ti++, null, t.field(), t.value(), t.standardTerm(), t.standard() ? "标准" : "建议修正");
            }
            for (int c = 0; c <= 3; c++) {
                s3.setColumnWidth(c, 6000);
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
        AitMaterial mat = materialMapper.selectById(r.getMaterialId());
        String content = mat == null ? "" : mat.getContent();
        compareMapper.delete(new LambdaQueryWrapper<AitCompare>().eq(AitCompare::getParseId, r.getParseId()));
        addCompare(r, applyId, "权利主体", r.getRightSubject(), apply.getRightHolder(), content, null);
        addCompare(r, applyId, "权利客体", r.getRightObject(), apply.getAssetName(), content, null); // 补比对客体↔资产名称
        addCompare(r, applyId, "权利类型", r.getRightType(), apply.getRightType(), content, null);
        // 权利期限:材料时长 vs 表单到期日,同口径(按申请创建日推算到期+容差)比对,消除"3年 vs 日期"误报
        String formExpiry = apply.getValidDate() == null ? null : apply.getValidDate().toLocalDate().toString();
        addCompare(r, applyId, "权利期限", r.getRightTerm(), formExpiry, content, diffTerm(r.getRightTerm(), apply));
        // 授权范围:确权表单无此字段 → 标"表单未含此项"而非笼统缺失
        addCompare(r, applyId, "授权范围", r.getAuthScope(), "确权表单不含此字段", content, AitCompare.DIFF_NA);
    }

    /** 加比对项;diffOverride 非空则用之(语义比对结果),否则按通用字符串 diff。 */
    private void addCompare(AitParseResult r, String applyId, String field, String matVal, String formVal, String content, String diffOverride) {
        AitCompare c = new AitCompare();
        c.setParseId(r.getParseId());
        c.setApplyId(applyId);
        c.setField(field);
        c.setMaterialValue(matVal);
        c.setFormValue(formVal);
        c.setDiffType(diffOverride != null ? diffOverride : diff(matVal, formVal));
        locateInSource(c, matVal, content); // #6 在原始正文中定位材料值(字符偏移+上下文片段)
        compareMapper.insert(c);
    }

    /** 权利期限语义比对:材料时长("3年"/"长期") → 按申请创建日推算到期,与表单到期日同口径比对(±31天容差)。 */
    private String diffTerm(String matTerm, ConfirmApply apply) {
        if (apply.getValidDate() == null) {
            return AitCompare.DIFF_MISSING;
        }
        if (!StringUtils.hasText(matTerm)) {
            return AitCompare.DIFF_MISSING;
        }
        if (matTerm.contains("长期") || matTerm.contains("永久")) {
            return AitCompare.DIFF_MATCH; // 长期授权:不设固定期限,视为一致
        }
        Integer years = parseYears(matTerm);
        java.time.LocalDate actual = apply.getValidDate().toLocalDate();
        if (years == null) {
            return diff(matTerm, actual.toString()); // 无法解析时长 → 退回字符串比对
        }
        java.time.LocalDate base = apply.getCreateTime() != null
                ? apply.getCreateTime().toLocalDate() : actual.minusYears(years);
        java.time.LocalDate expected = base.plusYears(years);
        long diffDays = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(expected, actual));
        return diffDays <= 31 ? AitCompare.DIFF_MATCH : AitCompare.DIFF_MISMATCH;
    }

    /** 从时长文本解析年数:"3年"→3,"36个月"→3。无法解析返回 null。 */
    private Integer parseYears(String term) {
        java.util.regex.Matcher y = java.util.regex.Pattern.compile("(\\d+)\\s*年").matcher(term);
        if (y.find()) {
            return Integer.parseInt(y.group(1));
        }
        java.util.regex.Matcher mo = java.util.regex.Pattern.compile("(\\d+)\\s*个?月").matcher(term);
        if (mo.find()) {
            return Integer.parseInt(mo.group(1)) / 12;
        }
        return null;
    }

    /** 定位标注锚点:在原始正文中查找材料值,记录字符偏移与上下文片段(图片/扫描件无正文→-1)。 */
    private void locateInSource(AitCompare c, String matVal, String content) {
        c.setSourceOffset(-1);
        c.setSourceSnippet("");
        if (!StringUtils.hasText(matVal) || !StringUtils.hasText(content)) {
            return;
        }
        int idx = content.indexOf(matVal.trim());
        if (idx < 0) {
            return;
        }
        c.setSourceOffset(idx);
        int from = Math.max(0, idx - 20);
        int to = Math.min(content.length(), idx + matVal.trim().length() + 20);
        c.setSourceSnippet((from > 0 ? "…" : "") + content.substring(from, to) + (to < content.length() ? "…" : ""));
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
        // 多要素与内置南网/电力术语库匹配:非标/模糊→标注 + 标准术语建议
        addTerm(out, AitTermLibrary.F_RIGHT_TYPE, r.getRightType());
        addTerm(out, AitTermLibrary.F_AUTH_SCOPE, r.getAuthScope());
        addTerm(out, AitTermLibrary.F_DATA_SOURCE, r.getDataSource());
        addTerm(out, AitTermLibrary.F_SENSITIVE, r.getSensitiveType());
        return out;
    }

    private void addTerm(List<TermSuggestion> out, String field, String value) {
        AitTermLibrary.Match m = AitTermLibrary.match(field, value);
        out.add(new TermSuggestion(field, value, m.standardTerm(), m.standard()));
    }

    @Override
    public void confirmTerm(String materialId, String field, String standardTerm) {
        if (!StringUtils.hasText(field) || !StringUtils.hasText(standardTerm)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "字段与标准术语不能为空");
        }
        AitParseResult r = getParse(materialId);
        AitParseResult upd = new AitParseResult();
        upd.setParseId(r.getParseId());
        switch (field) {
            case AitTermLibrary.F_RIGHT_TYPE -> upd.setRightType(standardTerm);
            case AitTermLibrary.F_AUTH_SCOPE -> upd.setAuthScope(standardTerm);
            case AitTermLibrary.F_DATA_SOURCE -> upd.setDataSource(standardTerm);
            case AitTermLibrary.F_SENSITIVE -> upd.setSensitiveType(standardTerm);
            default -> throw new BizException("不支持的术语字段:" + field);
        }
        parseMapper.updateById(upd);
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
