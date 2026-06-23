package com.csg.prm.confirm.aitool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.service.AitParseConfigService;
import com.csg.prm.confirm.aitool.service.AitParseRecordService;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitDocSegment;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.enums.MaterialDataType;
import com.csg.prm.confirm.aitool.term.AitTermLibrary;
import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import com.csg.prm.confirm.aitool.mapper.AitCompareMapper;
import com.csg.prm.confirm.aitool.mapper.AitDocSegmentMapper;
import com.csg.prm.confirm.aitool.mapper.AitMaterialMapper;
import com.csg.prm.confirm.aitool.mapper.AitParseResultMapper;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.aitool.storage.FileStorageGateway;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AitMaterialServiceImpl implements AitMaterialService {

    /** 允许的文件格式(#1:Excel/Word/PDF/扫描PDF/图片) */
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");
    /** 图片格式(#2 走 OCR) */
    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png");
    /** OCR 渲染最大页数(扫描 PDF):防超大文件拖垮;超出部分不 OCR 并提示。 */
    private static final int OCR_MAX_PAGES = 10;
    /** 单元格粒度片段上限(#5):防超大表格爆量;超出截断并提示。 */
    private static final int MAX_CELL_SEGMENTS = 2000;
    /** 单文件大小:下限 1KB 仅防空文件(质量交解析分类判定),上限 50MB 与确权材料一致 */
    private static final long MIN_BYTES = 1024L;
    private static final long MAX_BYTES = 50L * 1024 * 1024;
    /** 抽取置信度阈值(#3 对齐可研"准确率≥95%"):低于则标"需人工复核"。 */
    private static final double CONFIDENCE_THRESHOLD = 0.95;

    private static final ObjectMapper OM = new ObjectMapper();

    private final AitMaterialMapper materialMapper;
    private final AitParseResultMapper parseMapper;
    private final AitCompareMapper compareMapper;
    private final AiToolParseGateway parseGateway;
    private final ConfirmApplyMapper applyMapper;
    private final FileStorageGateway storage;
    private final AitDocSegmentMapper segmentMapper;
    private final AitParseRecordService recordService;
    private final AitParseConfigService configService;

    public AitMaterialServiceImpl(AitMaterialMapper materialMapper, AitParseResultMapper parseMapper,
                                  AitCompareMapper compareMapper, AiToolParseGateway parseGateway,
                                  ConfirmApplyMapper applyMapper, FileStorageGateway storage,
                                  AitDocSegmentMapper segmentMapper, AitParseRecordService recordService,
                                  AitParseConfigService configService) {
        this.materialMapper = materialMapper;
        this.parseMapper = parseMapper;
        this.compareMapper = compareMapper;
        this.parseGateway = parseGateway;
        this.applyMapper = applyMapper;
        this.storage = storage;
        this.segmentMapper = segmentMapper;
        this.recordService = recordService;
        this.configService = configService;
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
    public String uploadBinary(String fileName, byte[] data, String applyId, String assetId, String batchNo) {
        if (!StringUtils.hasText(fileName)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件名不能为空");
        }
        if (data == null || data.length == 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "文件内容为空");
        }
        String ext = extOf(fileName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("不支持的文件格式:" + ext + ",仅支持 Excel/Word/PDF/JPG/PNG");
        }
        if (data.length < MIN_BYTES) {
            throw new BizException("文件过小(" + data.length + "B),单文件不低于 1KB");
        }
        if (data.length > MAX_BYTES) {
            throw new BizException("文件过大(" + (data.length / 1024 / 1024) + "MB),单文件不超过 50MB");
        }
        // #6 内容指纹(SHA-256 over 原始字节):路径无关、可判真重复
        String contentHash = sha256(data);
        // 存储二进制
        String storagePath = storage.save(fileName, data);
        // 抽取正文(PDF/Word/Excel;图片/扫描件 留待解析阶段 OCR,正文暂空)
        String content = extractText(fileName, data);

        AitMaterial m = new AitMaterial();
        m.setFileName(fileName);
        m.setFileType(inferType(fileName));
        m.setSizeKb(Math.max(1L, data.length / 1024L));
        m.setApplyId(applyId);
        m.setAssetId(assetId);
        m.setStoragePath(storagePath);
        m.setContent(content);
        m.setFileHash(contentHash);
        // #6 重复检测:同内容指纹已存在 → 记录命中的原材料ID(不拦截上传,标注供前端提示/去重)
        m.setDuplicateOf(findDuplicate(contentHash));
        // #4 归集:材料类别 + 所属数据表标识
        m.setCategory(safeClassify(fileName, content));
        m.setDataTableRef(resolveDataTableRef(applyId, fileName));
        m.setBatchNo(StringUtils.hasText(batchNo) ? batchNo
                : "BATCH-" + m.getFileHash().substring(0, 8).toUpperCase());
        m.setOcrUsed(0);
        m.setParseStatus(AitMaterial.PARSE_PENDING);
        m.setProgress(0);
        materialMapper.insert(m);
        return m.getMaterialId();
    }

    /** #6 内容指纹 SHA-256(hex)。 */
    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return Sm3Util.hashHex(String.valueOf(data.length));
        }
    }

    /** #6 按内容指纹查重:返回最早一条同指纹材料ID,无则 null。 */
    private String findDuplicate(String contentHash) {
        if (!StringUtils.hasText(contentHash)) {
            return null;
        }
        AitMaterial dup = materialMapper.selectOne(new LambdaQueryWrapper<AitMaterial>()
                .eq(AitMaterial::getFileHash, contentHash)
                .orderByAsc(AitMaterial::getCreateTime)
                .last("LIMIT 1"));
        return dup == null ? null : dup.getMaterialId();
    }

    /** #4 资料类型(CEC_DATA_TYPE):优先模型(qwen),失败回退规则;统一存规范编码 01–07。 */
    private String safeClassify(String fileName, String content) {
        try {
            String c = parseGateway.classifyCategory(fileName, content);
            return StringUtils.hasText(c) ? MaterialDataType.codeOf(c) : MaterialDataType.OTHER.getCode();
        } catch (RuntimeException e) {
            return MaterialDataType.OTHER.getCode();
        }
    }

    /** #4 所属数据表标识:有确权申请则取其资产(assetId:assetName),否则用文件名主干。 */
    private String resolveDataTableRef(String applyId, String fileName) {
        if (StringUtils.hasText(applyId)) {
            ConfirmApply a = applyMapper.selectById(applyId);
            if (a != null && StringUtils.hasText(a.getAssetId())) {
                return a.getAssetId() + (StringUtils.hasText(a.getAssetName()) ? ":" + a.getAssetName() : "");
            }
        }
        return StringUtils.hasText(fileName) ? fileName.replaceAll("\\.[a-zA-Z]+$", "") : null;
    }

    @Override
    public byte[] loadFile(String materialId) {
        AitMaterial m = require(materialId);
        if (!StringUtils.hasText(m.getStoragePath())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该材料无可下载的原件(非真实文件上传)");
        }
        return storage.load(m.getStoragePath());
    }

    /** 抽取文件正文:PDF=PDFBox,.docx=POI,.xls/.xlsx=POI;图片/扫描件 留待解析阶段 OCR,返回空。异常优雅降级。 */
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
            if ("xls".equals(ext) || "xlsx".equals(ext)) {
                return extractExcelText(data);
            }
        } catch (Exception e) {
            // 抽取失败不阻断上传(正文留空,后续可由 OCR/人工补充)
            return "";
        }
        return "";
    }

    /** Excel 正文抽取(#1):WorkbookFactory 兼容 .xls/.xlsx,逐 sheet/行/单元格拼接为可读文本。 */
    private String extractExcelText(byte[] data) {
        DataFormatter fmt = new DataFormatter();
        StringBuilder sb = new StringBuilder();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            for (int si = 0; si < wb.getNumberOfSheets(); si++) {
                Sheet sheet = wb.getSheetAt(si);
                sb.append("【工作表】").append(sheet.getSheetName()).append('\n');
                for (Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (Cell cell : row) {
                        String v = fmt.formatCellValue(cell);
                        if (StringUtils.hasText(v)) {
                            if (line.length() > 0) {
                                line.append('\t');
                            }
                            line.append(v.trim());
                        }
                    }
                    if (line.length() > 0) {
                        sb.append(line).append('\n');
                    }
                }
            }
        } catch (Exception e) {
            return "";
        }
        return sb.toString().trim();
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
            // #2/#3 确保正文 + 版面分析:文本层为空且为图片/扫描PDF → OCR;Excel/Word 无正文 → 损坏
            ensureContentAndLayout(m);
            if (!StringUtils.hasText(m.getContent())) {
                throw new ParseBrokenException("文件损坏或无法解析正文:未能从 " + inferType(m.getFileName())
                        + " 提取到文字,且 OCR 未识别出内容,可能文件损坏或不含可识别文字");
            }
            tick(materialId, async, 35);   // 解析中(要素抽取)
            AiToolParseGateway.ParsedElements e = parseWithTimeout(m.getFileName(), m.getContent());
            tick(materialId, async, 55);   // 要素抽取完成
            AitParseResult r = saveResult(materialId, e);
            // 1.4#1 解析记录档:逐字段留档(时间/文档名/字段/值/置信度/操作人)
            recordParse(m, r);
            // #5 多粒度片段(按页/段/表格单元),统一文档对象的标准化切片
            buildSegments(m);
            tick(materialId, async, 70);
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

    /**
     * #2/#3 确保材料正文与版面分析就绪:
     * - 有文本层(PDF/Word/Excel 已抽取)→ 仅做轻量版面(印章关键词/页类型)并落库;
     * - 无文本层 + 图片 → 调 qwen-vl OCR;无文本层 + PDF → 逐页渲染后 OCR(扫描件)。
     * 就地更新 m 的 content/layoutJson/ocrUsed/pageCount 并持久化。
     */
    private void ensureContentAndLayout(AitMaterial m) {
        String ext = extOf(m.getFileName());
        if (StringUtils.hasText(m.getContent())) {
            persistExtract(m, m.getContent(), deriveTextLayoutJson(m), 0, pageCountSafe(m, null));
            return;
        }
        if (!StringUtils.hasText(m.getStoragePath())) {
            return; // 无原件可 OCR;由 runParse 判损坏
        }
        byte[] bytes;
        try {
            bytes = storage.load(m.getStoragePath());
        } catch (RuntimeException e) {
            return;
        }
        // 1.4#4 提取逻辑配置:OCR(qwen-vl 视觉模型)仅在 enableOcr 且 enableModel 同时开启时执行
        if (IMAGE_EXT.contains(ext) || "pdf".equals(ext)) {
            AitParseConfigService.ExtractLogic logic =
                    configService.extractLogic(com.csg.prm.confirm.aitool.entity.AitParseConfig.DEFAULT_SCENE);
            if (!logic.enableOcr() || !logic.enableModel()) {
                throw new ParseBrokenException("OCR 已按解析配置关闭(enableOcr/enableModel),"
                        + "图片/扫描件无法提取正文,如需识别请在解析配置中开启");
            }
        }
        if (IMAGE_EXT.contains(ext)) {
            AiToolParseGateway.OcrLayout o = parseGateway.ocrAndLayout(bytes, mimeOf(ext), m.getFileName());
            if (o != null) {
                persistExtract(m, o.text(), layoutJson(o, "ocr"), 1, 1);
            }
            return;
        }
        if ("pdf".equals(ext)) {
            ocrScannedPdf(m, bytes);
        }
        // 其余(xls/xlsx/doc/docx)无正文 → 不 OCR,保持空 → 判损坏
    }

    /** 扫描 PDF:逐页渲染为图片 → qwen-vl OCR,合并正文与版面(印章/标题)。超 OCR_MAX_PAGES 页截断并提示。 */
    private void ocrScannedPdf(AitMaterial m, byte[] bytes) {
        try (PDDocument doc = PDDocument.load(bytes)) {
            int pages = doc.getNumberOfPages();
            PDFRenderer renderer = new PDFRenderer(doc);
            StringBuilder text = new StringBuilder();
            List<AiToolParseGateway.Seal> seals = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            String pageType = "正文页";
            double conf = 0.0;
            int n = Math.min(pages, OCR_MAX_PAGES);
            for (int i = 0; i < n; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, 150);
                byte[] png = toPng(img);
                AiToolParseGateway.OcrLayout o = parseGateway.ocrAndLayout(png, "image/png",
                        m.getFileName() + " 第" + (i + 1) + "页");
                if (o != null) {
                    if (StringUtils.hasText(o.text())) {
                        text.append("【第").append(i + 1).append("页】\n").append(o.text()).append('\n');
                    }
                    if (o.seals() != null) {
                        seals.addAll(o.seals());
                    }
                    if (o.titles() != null) {
                        titles.addAll(o.titles());
                    }
                    if (i == 0 && StringUtils.hasText(o.pageType())) {
                        pageType = o.pageType();
                    }
                    conf = Math.max(conf, o.confidence());
                }
            }
            if (pages > OCR_MAX_PAGES) {
                text.append("\n（注:共 ").append(pages).append(" 页,超过 ").append(OCR_MAX_PAGES)
                        .append(" 页上限,其余页未 OCR)");
            }
            AiToolParseGateway.OcrLayout merged = new AiToolParseGateway.OcrLayout(
                    text.toString().trim(), titles, List.of(), seals, pageType, 1, conf);
            persistExtract(m, merged.text(), layoutJson(merged, "ocr"), 1, pages);
        } catch (Exception e) {
            // OCR/渲染失败 → 正文留空,runParse 判损坏
        }
    }

    private static byte[] toPng(BufferedImage img) throws java.io.IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    private static String mimeOf(String ext) {
        return ("png".equals(ext)) ? "image/png" : "image/jpeg";
    }

    /** 文本层材料的轻量版面 JSON:印章关键词 + 页类型(正文页),供 #3 展示与印章交叉校验。 */
    private String deriveTextLayoutJson(AitMaterial m) {
        String content = m.getContent() == null ? "" : m.getContent();
        List<AiToolParseGateway.Seal> seals = new ArrayList<>();
        if (content.contains("骑缝")) {
            seals.add(new AiToolParseGateway.Seal("骑缝章", "跨页接缝处", "正文检出骑缝章表述"));
        }
        if (content.contains("合同章")) {
            seals.add(new AiToolParseGateway.Seal("合同章", "落款区", "正文检出合同章表述"));
        }
        if (content.contains("公章") || content.contains("盖章") || content.contains("加盖") || content.contains("印鉴")) {
            seals.add(new AiToolParseGateway.Seal("公章", "落款区", "正文检出公章/盖章表述"));
        }
        String firstLine = content.lines().filter(StringUtils::hasText).findFirst().orElse("");
        AiToolParseGateway.OcrLayout o = new AiToolParseGateway.OcrLayout(
                "", firstLine.isEmpty() ? List.of() : List.of(firstLine.length() > 60 ? firstLine.substring(0, 60) : firstLine),
                List.of(), seals, "正文页", 1, 0.95);
        return layoutJson(o, "text-layer");
    }

    /** 序列化版面分析为 JSON(含来源标记 source=ocr|text-layer);text 字段不入库(正文另存 content)。 */
    private String layoutJson(AiToolParseGateway.OcrLayout o, String source) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("source", source);
        map.put("titles", o.titles());
        map.put("tables", o.tables());
        List<Map<String, String>> seals = new ArrayList<>();
        if (o.seals() != null) {
            for (AiToolParseGateway.Seal s : o.seals()) {
                Map<String, String> sm = new LinkedHashMap<>();
                sm.put("type", s.type());
                sm.put("location", s.location());
                sm.put("desc", s.desc());
                seals.add(sm);
            }
        }
        map.put("seals", seals);
        map.put("pageType", o.pageType());
        map.put("columnCount", o.columnCount());
        map.put("confidence", o.confidence());
        try {
            return OM.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Integer pageCountSafe(AitMaterial m, Integer dft) {
        if ("pdf".equals(extOf(m.getFileName())) && StringUtils.hasText(m.getStoragePath())) {
            try (PDDocument doc = PDDocument.load(storage.load(m.getStoragePath()))) {
                return doc.getNumberOfPages();
            } catch (Exception e) {
                return dft;
            }
        }
        return dft;
    }

    private void persistExtract(AitMaterial m, String content, String layoutJson, int ocrUsed, Integer pageCount) {
        m.setContent(content);
        m.setLayoutJson(layoutJson);
        m.setOcrUsed(ocrUsed);
        m.setPageCount(pageCount);
        AitMaterial upd = new AitMaterial();
        upd.setMaterialId(m.getMaterialId());
        upd.setContent(content);
        upd.setLayoutJson(layoutJson);
        upd.setOcrUsed(ocrUsed);
        upd.setPageCount(pageCount);
        materialMapper.updateById(upd);
    }

    /**
     * #5 生成多粒度片段(覆盖式:先清旧):
     * PDF=按页(PAGE)+段(PARAGRAPH);Excel=按单元格(CELL);Word=段+表格单元;图片/扫描件=OCR行(PARAGRAPH)。
     * 另把版面标题落为 TITLE 片段。统一文档对象=AitMaterial,其下挂标准化片段。
     */
    private void buildSegments(AitMaterial m) {
        segmentMapper.delete(new LambdaQueryWrapper<AitDocSegment>()
                .eq(AitDocSegment::getMaterialId, m.getMaterialId()));
        String ext = extOf(m.getFileName());
        byte[] bytes = null;
        if (StringUtils.hasText(m.getStoragePath())) {
            try {
                bytes = storage.load(m.getStoragePath());
            } catch (RuntimeException ignore) {
                bytes = null;
            }
        }
        int[] seq = {0};
        try {
            if ("pdf".equals(ext) && bytes != null && (m.getOcrUsed() == null || m.getOcrUsed() == 0)) {
                segPdf(m, bytes, seq);
            } else if (("xls".equals(ext) || "xlsx".equals(ext)) && bytes != null) {
                segExcel(m, bytes, seq);
            } else if ("docx".equals(ext) && bytes != null) {
                segDocx(m, bytes, seq);
            } else {
                segByParagraph(m, m.getContent(), seq); // 图片/扫描PDF(OCR正文)/兜底
            }
        } catch (Exception e) {
            segByParagraph(m, m.getContent(), seq); // 结构化失败兜底为段粒度
        }
        // 版面标题 → TITLE 片段
        addTitleSegments(m, seq);
    }

    private void segPdf(AitMaterial m, byte[] bytes, int[] seq) throws Exception {
        try (PDDocument doc = PDDocument.load(bytes)) {
            int pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int p = 1; p <= pages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(doc);
                if (StringUtils.hasText(pageText)) {
                    addSeg(m, AitDocSegment.G_PAGE, p, seq[0]++, null, null, null, pageText.trim());
                    for (String para : pageText.split("\\r?\\n\\s*\\r?\\n")) {
                        if (StringUtils.hasText(para)) {
                            addSeg(m, AitDocSegment.G_PARAGRAPH, p, seq[0]++, null, null, null, para.trim());
                        }
                    }
                }
            }
        }
    }

    private void segExcel(AitMaterial m, byte[] bytes, int[] seq) throws Exception {
        DataFormatter fmt = new DataFormatter();
        int cells = 0;
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            for (int si = 0; si < wb.getNumberOfSheets() && cells < MAX_CELL_SEGMENTS; si++) {
                Sheet sheet = wb.getSheetAt(si);
                String sheetName = sheet.getSheetName();
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        String v = fmt.formatCellValue(cell);
                        if (StringUtils.hasText(v)) {
                            addSeg(m, AitDocSegment.G_CELL, null, seq[0]++, sheetName,
                                    row.getRowNum() + 1, cell.getColumnIndex() + 1, v.trim());
                            if (++cells >= MAX_CELL_SEGMENTS) {
                                addSeg(m, AitDocSegment.G_PARAGRAPH, null, seq[0]++, sheetName, null, null,
                                        "(注:单元格超过 " + MAX_CELL_SEGMENTS + " 上限,其余未切片)");
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    private void segDocx(AitMaterial m, byte[] bytes, int[] seq) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String t = p.getText();
                if (StringUtils.hasText(t)) {
                    addSeg(m, AitDocSegment.G_PARAGRAPH, null, seq[0]++, null, null, null, t.trim());
                }
            }
            int ti = 0;
            for (XWPFTable table : doc.getTables()) {
                ti++;
                int rIdx = 0;
                for (XWPFTableRow row : table.getRows()) {
                    rIdx++;
                    int cIdx = 0;
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cIdx++;
                        String t = cell.getText();
                        if (StringUtils.hasText(t)) {
                            addSeg(m, AitDocSegment.G_CELL, null, seq[0]++, "表格" + ti, rIdx, cIdx, t.trim());
                        }
                    }
                }
            }
        }
    }

    private void segByParagraph(AitMaterial m, String content, int[] seq) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        for (String line : content.split("\\r?\\n")) {
            if (StringUtils.hasText(line)) {
                addSeg(m, AitDocSegment.G_PARAGRAPH, null, seq[0]++, null, null, null, line.trim());
            }
        }
    }

    private void addTitleSegments(AitMaterial m, int[] seq) {
        if (!StringUtils.hasText(m.getLayoutJson())) {
            return;
        }
        try {
            com.fasterxml.jackson.databind.JsonNode titles = OM.readTree(m.getLayoutJson()).get("titles");
            if (titles != null && titles.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode t : titles) {
                    if (StringUtils.hasText(t.asText())) {
                        addSeg(m, AitDocSegment.G_TITLE, null, seq[0]++, null, null, null, t.asText());
                    }
                }
            }
        } catch (Exception ignore) {
            // 标题片段非关键,失败忽略
        }
    }

    private void addSeg(AitMaterial m, String granularity, Integer pageNo, int idx,
                        String sheetName, Integer rowNum, Integer colNum, String content) {
        AitDocSegment s = new AitDocSegment();
        s.setMaterialId(m.getMaterialId());
        s.setGranularity(granularity);
        s.setPageNo(pageNo);
        s.setSegIndex(idx);
        s.setSheetName(sheetName);
        s.setRowIdx(rowNum);
        s.setColIdx(colNum);
        s.setContent(content.length() > 4000 ? content.substring(0, 4000) : content);
        segmentMapper.insert(s);
    }

    @Override
    public List<AitDocSegment> segments(String materialId, String granularity) {
        LambdaQueryWrapper<AitDocSegment> w = new LambdaQueryWrapper<AitDocSegment>()
                .eq(AitDocSegment::getMaterialId, materialId)
                .eq(StringUtils.hasText(granularity), AitDocSegment::getGranularity, granularity)
                .orderByAsc(AitDocSegment::getSegIndex);
        return segmentMapper.selectList(w);
    }

    @Override
    public List<MaterialGroup> aggregate(String applyId, String dataTableRef) {
        // #4 多表共附件:dataTableRef 可含多个数据表(分隔符 ;；,，、|),指定表过滤用 LIKE 命中其一
        LambdaQueryWrapper<AitMaterial> w = new LambdaQueryWrapper<AitMaterial>()
                .eq(StringUtils.hasText(applyId), AitMaterial::getApplyId, applyId)
                .like(StringUtils.hasText(dataTableRef), AitMaterial::getDataTableRef, dataTableRef)
                .orderByDesc(AitMaterial::getCreateTime);
        List<AitMaterial> all = materialMapper.selectList(w);
        // #4 附件↔主表关联索引:按数据表标识归集 —— 一表多附件(同表多材料归一组)+ 多表共附件(一材料拆挂其所属每个表分组)
        Map<String, MaterialGroup> groups = new LinkedHashMap<>();
        for (AitMaterial m : all) {
            m.setContent(null); // 列表不回传大正文
            m.setLayoutJson(null);
            for (String key : splitTables(m.getDataTableRef())) {
                if (StringUtils.hasText(dataTableRef)
                        && !key.contains(dataTableRef) && !dataTableRef.contains(key)) {
                    continue; // 指定表过滤时,多表共附件只保留命中的表分组
                }
                groups.computeIfAbsent(key, k -> new MaterialGroup(k, new ArrayList<>())).materials().add(m);
            }
        }
        return new ArrayList<>(groups.values());
    }

    /** #4 拆分材料所属数据表(多表共附件):按 ;；,，、| 分隔;空则归"(未归集)"。 */
    private static List<String> splitTables(String ref) {
        if (!StringUtils.hasText(ref)) {
            return List.of("(未归集)");
        }
        List<String> out = new ArrayList<>();
        for (String p : ref.split("[;；,，、|]")) {
            String t = p.trim();
            if (StringUtils.hasText(t)) {
                out.add(t);
            }
        }
        return out.isEmpty() ? List.of("(未归集)") : out;
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
            return "格式不支持:仅支持 Excel/Word/PDF/JPG/PNG(当前 ." + ext + ")";
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
        // 置信度 ≥ 阈值(1.4#4 管理员可配,默认 0.95)→ 自动通过;否则需人工复核
        double threshold = configThreshold();
        r.setReviewStatus(e.confidence() >= threshold ? "自动通过" : "需人工复核");
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
        AitMaterial m = require(materialId);
        String content = m.getContent() == null ? "" : m.getContent();
        List<TermSuggestion> out = new ArrayList<>();
        // 多要素与内置南网/电力术语库匹配 + 来源字段在材料中所在位置(1.3#1)
        addTerm(out, AitTermLibrary.F_RIGHT_TYPE, r.getRightType(), content);
        addTerm(out, AitTermLibrary.F_AUTH_SCOPE, r.getAuthScope(), content);
        addTerm(out, AitTermLibrary.F_DATA_SOURCE, r.getDataSource(), content);
        addTerm(out, AitTermLibrary.F_SENSITIVE, r.getSensitiveType(), content);
        return out;
    }

    private void addTerm(List<TermSuggestion> out, String field, String value, String content) {
        AitTermLibrary.Match m = AitTermLibrary.match(field, value);
        out.add(new TermSuggestion(field, value, m.standardTerm(), m.standard(), locateValue(content, value)));
    }

    /** 1.3#1 来源字段定位:返回值在材料正文中的所在位置(上下文片段 + 字符偏移);未命中给出说明。 */
    private String locateValue(String content, String value) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(content)) {
            return "未在材料正文中定位(可能来自文件名/OCR版面)";
        }
        int idx = content.indexOf(value.trim());
        if (idx < 0) {
            return "未在材料正文中定位";
        }
        int from = Math.max(0, idx - 12);
        int to = Math.min(content.length(), idx + value.trim().length() + 12);
        String snippet = (from > 0 ? "…" : "") + content.substring(from, to) + (to < content.length() ? "…" : "");
        return "「" + snippet.replaceAll("\\s+", " ") + "」(偏移 " + idx + ")";
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

    private double configThreshold() {
        try {
            return configService.threshold(com.csg.prm.confirm.aitool.entity.AitParseConfig.DEFAULT_SCENE);
        } catch (RuntimeException e) {
            return CONFIDENCE_THRESHOLD;
        }
    }

    private void recordParse(AitMaterial m, AitParseResult r) {
        try {
            var ctx = UserContextHolder.get();
            String opId = ctx == null ? null : ctx.getUserId();
            String opName = ctx == null ? null : ctx.getUserName();
            recordService.record(m, r, opId, opName);
        } catch (RuntimeException ignore) {
            // 留档失败不阻断解析
        }
    }

    @Override
    public int batchParse(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            throw new BizException("批次号不能为空");
        }
        List<AitMaterial> list = materialMapper.selectList(new LambdaQueryWrapper<AitMaterial>()
                .eq(AitMaterial::getBatchNo, batchNo)
                .ne(AitMaterial::getParseStatus, AitMaterial.PARSE_SUCCESS));
        for (AitMaterial m : list) {
            submitParse(m.getMaterialId()); // @Async:自动排队逐个解析
        }
        return list.size();
    }

    @Override
    public BatchProgress batchProgress(String batchNo) {
        List<AitMaterial> list = materialMapper.selectList(new LambdaQueryWrapper<AitMaterial>()
                .eq(AitMaterial::getBatchNo, batchNo)
                .orderByAsc(AitMaterial::getFileName));
        int done = 0;
        int failed = 0;
        int running = 0;
        int pending = 0;
        List<BatchItem> items = new ArrayList<>();
        for (AitMaterial m : list) {
            switch (m.getParseStatus() == null ? "" : m.getParseStatus()) {
                case AitMaterial.PARSE_SUCCESS -> done++;
                case AitMaterial.PARSE_FAILED -> failed++;
                case AitMaterial.PARSE_RUNNING -> running++;
                default -> pending++;
            }
            items.add(new BatchItem(m.getMaterialId(), m.getFileName(), m.getParseStatus(),
                    m.getProgress() == null ? 0 : m.getProgress(), m.getFailReason()));
        }
        return new BatchProgress(list.size(), done, failed, running, pending, items);
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
        if (f.endsWith(".xls") || f.endsWith(".xlsx")) {
            return "EXCEL";
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
