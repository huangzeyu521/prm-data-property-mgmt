package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitDocSegment;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.service.AitAccuracyService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.1.1.1 材料智能解析 — 资料接入与智能文档处理(#1~#7)能力测试。
 * 测试 profile=test → 解析网关为本地确定性桩(无网络),保证可重复。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitMaterialAdvancedTest {

    @Autowired private AitMaterialService service;
    @Autowired private AitAccuracyService accuracyService;

    /** #4 资产级归集:上传带 assetId 落库,支撑资产级材料归集/查重(确权向导→aitool 材料入口贯通)。 */
    @Test
    void upload_persists_asset_and_apply_id() throws Exception {
        String id = service.uploadBinary("非车险理赔-确权证明.xlsx", xlsxBytes(), "AP-DEMO", "ASSET-NCLAIM-2026", null);
        AitMaterial m = service.getMaterial(id);
        assertEquals("ASSET-NCLAIM-2026", m.getAssetId(), "材料应持久化关联资产ID");
        assertEquals("AP-DEMO", m.getApplyId(), "材料应持久化关联申请ID");
    }

    /** #1 Excel 导入 + 抽取正文;#5 按单元格多粒度切片。 */
    @Test
    void excel_import_and_cell_segments() throws Exception {
        byte[] xlsx = xlsxBytes();
        String id = service.uploadBinary("数据资产清单.xlsx", xlsx, null, null, null);
        AitMaterial m = service.getMaterial(id);
        assertEquals("EXCEL", m.getFileType(), "应识别为 Excel");
        assertTrue(m.getContent() != null && m.getContent().contains("电力计量数据"), "应抽取出 Excel 单元格正文");

        service.parse(id);
        assertEquals(AitMaterial.PARSE_SUCCESS, service.getMaterial(id).getParseStatus());
        List<AitDocSegment> cells = service.segments(id, AitDocSegment.G_CELL);
        assertFalse(cells.isEmpty(), "Excel 应产出单元格粒度片段");
        assertTrue(cells.stream().anyMatch(c -> c.getRowIdx() != null && c.getColIdx() != null),
                "单元格片段应带行列定位");
    }

    /** #5 Word 段落多粒度切片。 */
    @Test
    void docx_paragraph_segments() throws Exception {
        byte[] docx = docxBytes("数据持有权确权证明,数据来源自行生产,有效期3年,已加盖公章。");
        String id = service.uploadBinary("确权证明.docx", docx, null, null, null);
        service.parse(id);
        List<AitDocSegment> paras = service.segments(id, AitDocSegment.G_PARAGRAPH);
        assertFalse(paras.isEmpty(), "Word 应产出段落粒度片段");
    }

    /** #2 图片走 OCR(本地桩):解析成功且标记 ocrUsed;#3 版面分析检出印章区域。 */
    @Test
    void image_goes_through_ocr_and_layout() throws Exception {
        byte[] png = pngBytes();
        String id = service.uploadBinary("盖章扫描件.png", png, null, null, null);
        service.parse(id);
        AitMaterial m = service.getMaterial(id);
        assertEquals(AitMaterial.PARSE_SUCCESS, m.getParseStatus(), "图片应经 OCR 解析成功");
        assertEquals(Integer.valueOf(1), m.getOcrUsed(), "图片应标记经过 OCR");
        assertNotNull(m.getLayoutJson(), "应产出版面分析结果");
        assertTrue(m.getLayoutJson().contains("公章"), "文件名含'章' → 版面应检出公章区域");
    }

    /** #4 材料类别归集:按内容/文件名归类。 */
    @Test
    void category_classified() throws Exception {
        String id = service.uploadBinary("数据授权材料.xlsx", xlsxBytes(), null, null, null);
        assertEquals("授权材料", service.getMaterial(id).getCategory());
    }

    /** #6 重复检测:同内容指纹的第二份标记 duplicateOf。 */
    @Test
    void duplicate_detection_by_content_hash() throws Exception {
        byte[] xlsx = uniqueXlsxBytes("DEDUP-MARKER-" + System.nanoTime()); // 独有内容,避免与其他用例哈希相同
        String first = service.uploadBinary("清单A.xlsx", xlsx, null, null, null);
        String second = service.uploadBinary("清单A-副本.xlsx", xlsx, null, null, null);
        assertNull(service.getMaterial(first).getDuplicateOf(), "首份不应判重");
        assertEquals(first, service.getMaterial(second).getDuplicateOf(), "同内容第二份应命中首份为重复");
        assertEquals(service.getMaterial(first).getFileHash(), service.getMaterial(second).getFileHash(),
                "同内容指纹应一致(内容哈希,与文件名/路径无关)");
    }

    /** #7 解析准确度评测:标注样本集逐字段比对,整体准确率应 ≥ 95%。 */
    @Test
    void accuracy_meets_threshold() {
        AitAccuracyService.AccuracyReport r = accuracyService.evaluate();
        assertTrue(r.sampleCount() >= 5, "应有标注样本");
        assertTrue(r.overall() >= 0.95, "整体准确率应 ≥95%,实际=" + r.overall());
        assertTrue(r.pass(), "应判定达标");
    }

    /** #4 附件↔主表关联索引:一表多附件(同表多材料归一组)+ 多表共附件(一材料挂多表分组)。 */
    @Test
    void aggregate_one_table_many_attachments_and_shared_attachment() {
        String tag = "AGG-" + System.nanoTime();
        String tA = tag + "_TableA";
        String tB = tag + "_TableB";
        String m1 = uploadMeta("元数据A.xlsx", "内容1-" + tag, tA);              // 一表多附件:T_A
        String m2 = uploadMeta("制度附件A.docx", "内容2-" + tag, tA);            // 一表多附件:T_A
        String shared = uploadMeta("共用来源说明.pdf", "内容3-" + tag, tA + ";" + tB); // 多表共附件:T_A 与 T_B

        List<AitMaterialService.MaterialGroup> groups = service.aggregate(null, null);
        var gA = groups.stream().filter(g -> tA.equals(g.dataTableRef())).findFirst().orElseThrow();
        var gB = groups.stream().filter(g -> tB.equals(g.dataTableRef())).findFirst().orElseThrow();
        var ids = gA.materials().stream().map(AitMaterial::getMaterialId).toList();
        assertTrue(ids.contains(m1) && ids.contains(m2) && ids.contains(shared),
                "一表多附件:TableA 组应含全部 3 份材料");
        assertTrue(gB.materials().stream().anyMatch(x -> shared.equals(x.getMaterialId())),
                "多表共附件:共享附件应同时出现在 TableB 组");

        // 指定表过滤:只回该表分组,且共享附件命中
        List<AitMaterialService.MaterialGroup> onlyB = service.aggregate(null, tB);
        assertEquals(1, onlyB.size(), "按 TableB 过滤应只回一个分组");
        assertTrue(onlyB.get(0).materials().stream().anyMatch(x -> shared.equals(x.getMaterialId())),
                "按 TableB 过滤应命中共享附件");
    }

    private String uploadMeta(String fileName, String content, String dataTableRef) {
        AitMaterial m = new AitMaterial();
        m.setFileName(fileName);
        m.setContent(content);
        m.setDataTableRef(dataTableRef);
        return service.upload(m);
    }

    // ---- 测试夹具 ----

    private byte[] xlsxBytes() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("资产清单");
            Row r0 = s.createRow(0);
            r0.createCell(0).setCellValue("资产名称");
            r0.createCell(1).setCellValue("权属类型");
            Row r1 = s.createRow(1);
            r1.createCell(0).setCellValue("电力计量数据");
            r1.createCell(1).setCellValue("数据持有权");
            wb.write(bos);
        }
        return bos.toByteArray();
    }

    /** 含独有标记的 xlsx,保证内容指纹唯一(用于重复检测用例)。 */
    private byte[] uniqueXlsxBytes(String marker) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("唯一");
            s.createRow(0).createCell(0).setCellValue(marker);
            wb.write(bos);
        }
        return bos.toByteArray();
    }

    private byte[] docxBytes(String text) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText(text);
            doc.write(bos);
        }
        return bos.toByteArray();
    }

    /** 生成带渐变(高熵,>1KB)的 PNG,模拟扫描件/截图。 */
    private byte[] pngBytes() throws Exception {
        int w = 256, h = 256;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, ((x * 7) & 0xFF) << 16 | ((y * 5) & 0xFF) << 8 | ((x + y) & 0xFF));
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }
}
