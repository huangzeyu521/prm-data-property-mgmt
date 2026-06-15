package com.csg.prm.confirm.aitool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能确权辅助工具-测试材料生成器(离线,复用 POI/PDFBox/ImageIO,无外部依赖)。
 * 仅当传 -Dgen.base=<宿主test目录> 时运行;产出落到 <gen.base>/智能确权辅助工具/。
 * 用法(docker 内):mvn -pl ...,dpr-confirm-service -am test -Dtest=TestMaterialGenerator \
 *   -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dgen.base=/host-test
 */
@EnabledIfSystemProperty(named = "gen.base", matches = ".+")
class TestMaterialGenerator {

    private static final ObjectMapper OM = new ObjectMapper();
    private final List<Map<String, Object>> manifest = new ArrayList<>();

    @Test
    void generate() throws Exception {
        File root = new File(System.getProperty("gen.base"), "智能确权辅助工具");
        // 覆盖式:清空旧材料子目录(保留 root 下其他内容)
        root.mkdirs();

        // 01 格式覆盖(Excel/Word/PDF文本层/扫描PDF/图片)
        String d1 = "01_格式覆盖";
        docx(root, d1, "确权证明.docx",
                List.of("数据确权证明",
                        "权利主体:广东电网有限责任公司",
                        "数据客体:客户用电信息表",
                        "权利类型:数据资源持有权",
                        "数据来源:自行生产",
                        "授权范围:全字段",
                        "有效期:3年",
                        "本证明已加盖公章。"),
                "材料解析#1 Word导入", "成功", "标准确权证明,含盖章表述");
        xlsxAsset(root, d1, "数据资产清单.xlsx",
                "材料解析#1 Excel导入;按单元格多粒度切片", "成功");
        pdfText(root, d1, "确权证明_文本层.pdf",
                List.of("Data Ownership Certificate (text-layer test material)",
                        "Right Subject: Guangdong Power Grid Co., Ltd.",
                        "Right Type: data holding right",
                        "Data Source: self-production",
                        "Auth Scope: all fields",
                        "Validity: 3 years",
                        "Sealed: yes"),
                "材料解析#1 PDF文本层;走 PDFTextStripper 抽取", "成功");
        pdfScanned(root, d1, "确权书_扫描件.pdf",
                List.of("数据确权书(扫描件)", "权利主体:广州供电局", "权利类型:数据加工使用权",
                        "数据来源:交易采购", "有效期:5年", "(公章)"),
                "材料解析#2 扫描版PDF;走 OCR(qwen-vl/本地桩)", "成功(OCR)");
        png(root, d1, "权属证明_盖章扫描.png",
                List.of("数据权属证明(图片/扫描)", "权属主体:深圳供电局", "权利类型:数据产品经营权",
                        "数据来源:公开采集", "(已盖章)"),
                "材料解析#2 图片;走 OCR", "成功(OCR)");
        // OCR 正样本:纯 ASCII 清晰内容(容器可清晰渲染)→ qwen-vl 应逐字读出,含唯一标记便于校验
        List<String> ocrSample = List.of(
                "DATA OWNERSHIP CERTIFICATE",
                "Asset: Customer Power Usage Table",
                "Right Subject: Guangdong Power Grid Co Ltd",
                "Right Type: Data Holding Right",
                "Data Source: Self Production",
                "Validity: 3 Years    Scope: All Fields",
                "Certificate No: OCR-SAMPLE-20260615",
                "Sealed: YES");
        png(root, d1, "OCR正样本_英文清晰.png", ocrSample,
                "材料解析#2 OCR 正样本(清晰ASCII);qwen-vl 应逐字读出唯一标记 OCR-SAMPLE-20260615", "成功(OCR)");
        pdfScanned(root, d1, "OCR正样本_英文清晰_扫描.pdf", ocrSample,
                "材料解析#2 OCR 正样本(扫描PDF,清晰ASCII)", "成功(OCR)");

        // 02 数据清洗(脏数据)
        String d2 = "02_数据清洗";
        dirtyXlsx(root, d2, "脏数据清单.xlsx", "数据清洗1.2 去噪/全半角/空值/非标枚举/重复行/布尔", "—");
        fieldMapXlsx(root, d2, "自定义字段名清单.xlsx", "数据清洗1.2 字段映射对齐(非内置别名)", "—");

        // 03 要素抽取(五主体/五约束/来源/特征)
        String d3 = "03_要素抽取";
        docx(root, d3, "要素齐全_持有权.docx", elementRich(
                "数据持有权", "自行生产", "电网生产数据",
                "广东电网有限责任公司", "南方电网", "广州供电局", "南网数字研究院", "无"),
                "要素抽取1.3 五类主体+五类约束+来源+特征", "成功", "要素齐全,无个人/敏感");
        docx(root, d3, "要素齐全_经营权个人信息.docx", elementRich(
                "数据产品经营权", "公开采集", "个人信息",
                "中国南方电网有限责任公司", "省能源局", "电网营销中心", "数据加工方", "第三方科研机构"),
                "要素抽取1.3 经营权+个人信息(高分级/法律校验)", "成功", "含个人信息,数据级别敏感");
        docx(root, d3, "要素齐全_使用权商密.docx", elementRich(
                "数据加工使用权", "共同生产", "商业秘密",
                "广东电网", "集团公司", "数据使用单位", "数据加工方", "合作方"),
                "要素抽取1.3 使用权+商业秘密(核心级)", "成功", "含商业秘密,数据级别核心");

        // 04 批量解析(同批多文件)
        String d4 = "04_批量解析";
        docx(root, d4, "批量_01.docx", List.of("数据确权证明 批量01", "权利类型:数据持有权",
                "数据来源:自行生产", "有效期:3年", "已盖章。"), "材料管理1.4 批量解析(同批)", "成功");
        xlsxAsset(root, d4, "批量_02.xlsx", "材料管理1.4 批量解析(同批)", "成功");
        docx(root, d4, "批量_03.docx", List.of("数据确权证明 批量03", "权利类型:数据加工使用权",
                "数据来源:交易采购", "有效期:2年", "合同章齐全。"), "材料管理1.4 批量解析(同批)", "成功");

        // 05 冲突场景(对立主张)
        String d5 = "05_冲突场景";
        docx(root, d5, "历史持有权证明.docx", List.of("历史确权记录",
                "权利主体:深圳供电局", "权利类型:数据所有权", "授权范围:全字段", "排他:是",
                "有效期:至2027-01-01", "已盖章。"), "冲突识别 历史排他主张", "成功", "与当前主体冲突");
        docx(root, d5, "当前持有权申请.docx", List.of("当前确权申请",
                "权利主体:广东电网有限责任公司", "权利类型:数据资源持有权", "授权范围:全字段", "排他:是",
                "有效期:至2028-01-01", "已盖章。"), "冲突识别 当前主张(与历史对立)", "成功", "触发主体/范围/时效冲突");

        // 06 重复检测(同内容副本)
        String d6 = "06_重复检测";
        byte[] dup = docxBytes(List.of("数据确权证明(用于重复检测)", "权利类型:数据持有权",
                "数据来源:自行生产", "有效期:3年", "已盖章。"));
        write(root, d6, "原件.docx", dup, "材料解析1.1#6 内容指纹查重(原件)", "成功", "与副本内容一致");
        write(root, d6, "原件_副本.docx", dup, "材料解析1.1#6 内容指纹查重(副本)", "成功(标记重复)", "应命中原件为重复");

        // 07 异常/负面
        String d7 = "07_异常负面";
        docx(root, d7, "无章证明.docx", List.of("数据确权证明", "权利主体:某单位",
                "权利类型:数据持有权", "数据来源:自行生产", "本材料未加盖公章。"),
                "材料解析 印章交叉校验(无章→存疑)", "成功", "印章应判可疑/未检出");
        docx(root, d7, "空白文件.docx", List.of(""),
                "负面:空内容→解析失败(需OCR/损坏)", "失败", "正文为空,应判失败");

        // 清单 + 说明
        Files.write(new File(root, "INDEX.json").toPath(),
                OM.writerWithDefaultPrettyPrinter().writeValueAsString(manifest)
                        .getBytes(StandardCharsets.UTF_8));
        Files.write(new File(root, "README.md").toPath(), readme().getBytes(StandardCharsets.UTF_8));

        System.out.println("[GEN] 测试材料生成完成,共 " + manifest.size() + " 个文件 → " + root.getAbsolutePath());
    }

    // ---------- 内容模板 ----------

    private List<String> elementRich(String rightType, String source, String feature,
                                     String srcSubj, String authSubj, String useSubj, String procSubj, String shareObj) {
        return List.of("数据确权要素材料",
                "权利类型:" + rightType,
                "数据来源:" + source,
                "数据特征:" + feature,
                "来源主体:" + srcSubj,
                "授权主体:" + authSubj,
                "使用主体:" + useSubj,
                "加工主体:" + procSubj,
                "共享对象:" + shareObj,
                "授权范围:约定字段",
                "使用边界:仅限内部分析使用",
                "共享限制:禁止再共享",
                "保留期限:3年",
                "脱敏要求:手机号与身份证脱敏",
                "已加盖公章。");
    }

    // ---------- 生成器 ----------

    private void docx(File root, String dir, String name, List<String> lines,
                      String scenario, String expect, String... note) throws Exception {
        write(root, dir, name, docxBytes(lines), scenario, expect, note);
    }

    private byte[] docxBytes(List<String> lines) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            for (String line : lines) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText(line);
            }
            doc.write(bos);
        }
        return bos.toByteArray();
    }

    private void xlsxAsset(File root, String dir, String name, String scenario, String expect) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("数据资产清单");
            String[][] data = {
                    {"资产名称", "权利类型", "数据来源", "敏感类型", "授权范围", "是否盖章"},
                    {"客户用电信息表", "数据资源持有权", "自行生产", "电网生产数据", "全字段", "是"},
                    {"线损分析数据", "数据加工使用权", "共同生产", "内部运营数据", "约定字段", "是"},
            };
            for (int r = 0; r < data.length; r++) {
                Row row = s.createRow(r);
                for (int c = 0; c < data[r].length; c++) {
                    row.createCell(c).setCellValue(data[r][c]);
                }
            }
            wb.write(bos);
        }
        write(root, dir, name, bos.toByteArray(), scenario, expect);
    }

    private void dirtyXlsx(File root, String dir, String name, String scenario, String expect) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("脏数据");
            String[][] data = {
                    {"表名", "字段名", "权利类型", "数据来源", "敏感类型", "是否个人信息"},
                    {"  用户  表 ", "ｕｓｅｒ＿ｉｄ", "持有权", "自产", "火星数据", "Y"},      // 噪声/全角/非标/异常枚举/布尔
                    {"订单表", "order_id", "经营权", "采购", "", "否"},                       // 空值
                    {"订单表", "order_id", "经营权", "采购", "", "否"},                       // 重复行
            };
            for (int r = 0; r < data.length; r++) {
                Row row = s.createRow(r);
                for (int c = 0; c < data[r].length; c++) {
                    row.createCell(c).setCellValue(data[r][c]);
                }
            }
            wb.write(bos);
        }
        write(root, dir, name, bos.toByteArray(), scenario, expect);
    }

    private void fieldMapXlsx(File root, String dir, String name, String scenario, String expect) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("自定义字段");
            Row h = s.createRow(0);
            h.createCell(0).setCellValue("我的表名");   // 非内置别名,需配置映射
            h.createCell(1).setCellValue("我的字段");
            Row r1 = s.createRow(1);
            r1.createCell(0).setCellValue("ORDER_T");
            r1.createCell(1).setCellValue("order_id");
            wb.write(bos);
        }
        write(root, dir, name, bos.toByteArray(), scenario, expect);
    }

    private void pdfText(File root, String dir, String name, List<String> lines,
                         String scenario, String expect) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.setLeading(18);
                cs.newLineAtOffset(60, 780);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLine();
                }
                // 填充说明行(保证文件 >1KB,满足上传下限;并为文本抽取提供更多内容)
                cs.showText("---- Generated test material for aitool parse pipeline (text-layer PDF). ----");
                cs.newLine();
                for (int i = 1; i <= 12; i++) {
                    cs.showText("Note line " + i + ": this PDF carries an extractable text layer for PDFTextStripper.");
                    cs.newLine();
                }
                cs.endText();
            }
            doc.getDocumentInformation().setTitle("Data Ownership Certificate (test)");
            doc.getDocumentInformation().setAuthor("TestMaterialGenerator");
            doc.save(bos);
        }
        write(root, dir, name, bos.toByteArray(), scenario, expect, "纯 ASCII 文本层(容器无中文字体);中文内容见 INDEX/扫描件");
    }

    private void pdfScanned(File root, String dir, String name, List<String> lines,
                            String scenario, String expect) throws Exception {
        BufferedImage img = textImage(lines, 900, 1200);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(img.getWidth(), img.getHeight()));
            doc.addPage(page);
            PDImageXObject xo = LosslessFactory.createFromImage(doc, img);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(xo, 0, 0, img.getWidth(), img.getHeight());
            }
            doc.save(bos);
        }
        write(root, dir, name, bos.toByteArray(), scenario, expect, "图片页(无文本层),模拟扫描件");
    }

    private void png(File root, String dir, String name, List<String> lines,
                     String scenario, String expect) throws Exception {
        BufferedImage img = textImage(lines, 760, 560);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        write(root, dir, name, bos.toByteArray(), scenario, expect);
    }

    /** 白底黑字 + 红色"公章"圆章 + 轻噪声(高熵,>1KB);容器无中文字体则汉字为缺字框,仍为合法图像。 */
    private BufferedImage textImage(List<String> lines, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        // 轻噪声纹理(确保 PNG 体积/熵)
        for (int x = 0; x < w; x += 3) {
            for (int y = 0; y < h; y += 3) {
                if (((x * 31 + y * 17) & 7) == 0) {
                    img.setRGB(x, y, 0xF2F2F2);
                }
            }
        }
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.PLAIN, 26));
        int y = 70;
        for (String line : lines) {
            g.drawString(line, 50, y);
            y += 46;
        }
        // 模拟公章(红圈+文字)
        g.setColor(new Color(200, 30, 30));
        g.drawOval(w - 220, h - 220, 150, 150);
        g.drawOval(w - 218, h - 218, 146, 146);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        g.drawString("公章 SEAL", w - 210, h - 140);
        g.dispose();
        return img;
    }

    private void write(File root, String dir, String name, byte[] bytes,
                       String scenario, String expect, String... note) throws Exception {
        File d = new File(root, dir);
        d.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(new File(d, name))) {
            fos.write(bytes);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("path", dir + "/" + name);
        m.put("format", name.substring(name.lastIndexOf('.') + 1).toUpperCase());
        m.put("sizeKB", Math.max(1, bytes.length / 1024));
        m.put("scenario", scenario);
        m.put("expectParse", expect);
        if (note.length > 0) {
            m.put("note", note[0]);
        }
        manifest.add(m);
    }

    private String readme() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 智能确权辅助工具 · 自动测试材料\n\n");
        sb.append("由 `TestMaterialGenerator` 自动生成(复用 POI/PDFBox/ImageIO),覆盖可研 1.1.1.1~3.3 九大组的测试场景。\n");
        sb.append("每个文件的场景与**预期解析结果**见 `INDEX.json`。\n\n");
        sb.append("> 容器无中文字体:Word/Excel 为真中文(走文本抽取);PDF 文本层为 ASCII;扫描版 PDF 与 PNG 为图片页(走 OCR,汉字像素可能为缺字框,实际内容以 INDEX.json 为准)。\n\n");
        sb.append("## 目录\n");
        sb.append("| 目录 | 用途 |\n|---|---|\n");
        sb.append("| 01_格式覆盖 | Excel/Word/PDF文本层/扫描PDF/图片 五格式 |\n");
        sb.append("| 02_数据清洗 | 脏数据(噪声/全半角/空值/非标枚举/重复/布尔)+ 自定义字段映射 |\n");
        sb.append("| 03_要素抽取 | 五类主体+五类约束+来源方式+数据特征(含个人信息/商密分级) |\n");
        sb.append("| 04_批量解析 | 同批多文件 |\n");
        sb.append("| 05_冲突场景 | 历史 vs 当前 对立权属主张 |\n");
        sb.append("| 06_重复检测 | 同内容副本(内容指纹查重) |\n");
        sb.append("| 07_异常负面 | 无章/空内容(负面用例) |\n");
        sb.append("\n## 使用\n");
        sb.append("- 人工:前端 智能确权辅助工具 → 上传材料,逐场景验证。\n");
        sb.append("- 自动:`TestMaterialConsumeTest`(-Dverify.base=)把每个文件喂回 uploadBinary+parse 断言。\n");
        return sb.toString();
    }
}
