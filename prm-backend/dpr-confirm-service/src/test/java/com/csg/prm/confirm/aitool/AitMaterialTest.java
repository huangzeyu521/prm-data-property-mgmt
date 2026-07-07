package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.controller.AitMaterialController;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.aitool.term.AitTermLibrary;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 智能确权辅助工具 M1 材料智能解析测试:上传→解析要素抽取→印章→术语匹配→与确权表单比对。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitMaterialTest {

    @Autowired
    private AitMaterialService aitService;
    @Autowired
    private ConfirmApplyService applyService;

    @Test
    void upload_parse_extract_and_compare() {
        // 先建一笔确权申请(供比对)
        ConfirmApply apply = new ConfirmApply();
        apply.setAssetId("DA-AIT-1");
        apply.setAssetName("客户用电信息表");
        apply.setRightType("持有权");
        apply.setRightHolder("广东电网");
        String applyId = applyService.saveDraft(apply);

        AitMaterial m = new AitMaterial();
        m.setFileName("客户用电信息表-确权证明-盖章.pdf");
        m.setApplyId(applyId);
        m.setSizeKb(2048L);
        m.setContent("权利主体广东电网,持有权,有效期3年,授权范围约定字段,自行生产,已盖章");
        String id = aitService.upload(m);

        AitMaterial saved = aitService.page(new com.csg.prm.common.query.PageRequest(), null, null, applyId)
                .getRecords().get(0);
        assertEquals("PDF", saved.getFileType());
        assertNotNull(saved.getFileHash());
        assertEquals(AitMaterial.PARSE_PENDING, saved.getParseStatus());

        aitService.parse(id);
        AitParseResult r = aitService.getParse(id);
        // #3 全 5 类要素抽取:主体/客体/类型/期限/授权范围 + 置信度
        assertNotNull(r.getRightSubject(), "应抽取权利主体");
        assertNotNull(r.getRightObject(), "应抽取权利客体");
        assertTrue(r.getRightObject().contains("客户用电信息表"), "权利客体应来自文件名,实际:" + r.getRightObject());
        assertEquals("持有权", r.getRightType());
        assertEquals("3年", r.getRightTerm());
        assertEquals("约定字段", r.getAuthScope());
        assertTrue(r.getConfidence() > 0.9);
        // #5 印章-OCR 交叉校验:盖章 + 正文充分 + 抽到主体 → 有效;材料可信度=可信
        assertEquals("有效", r.getSealValid(), "盖章且OCR佐证充分应判印章有效");
        assertEquals("可信", r.getTrustLevel(), "有效印章+完整要素应评 可信,实际:" + r.getTrustLevel());
        assertNotNull(r.getTrustScore());
        // 低置信度(桩 0.92 < 0.95)→ 标"需人工复核"
        assertEquals("需人工复核", r.getReviewStatus(), "置信度低于 0.95 应标需人工复核,实际:" + r.getReviewStatus());

        // 术语库匹配:持有权为标准术语
        List<AitMaterialService.TermSuggestion> terms = aitService.termCheck(id);
        assertTrue(terms.get(0).standard());

        // 与表单比对:主体/客体/类型/期限/授权范围 五项逐项比对
        List<AitCompare> cmp = aitService.compares(id);
        assertEquals(5, cmp.size());
        AitCompare rtype = cmp.stream().filter(c -> "权利类型".equals(c.getField())).findFirst().orElseThrow();
        assertEquals(AitCompare.DIFF_MATCH, rtype.getDiffType());
        // #6 标注定位锚点:权利类型"持有权"在原文可定位(字符偏移 ≥0 + 上下文片段含该值)
        assertNotNull(rtype.getSourceOffset());
        assertTrue(rtype.getSourceOffset() >= 0, "权利类型应在原始正文定位到,offset=" + rtype.getSourceOffset());
        assertTrue(rtype.getSourceSnippet() != null && rtype.getSourceSnippet().contains("持有权"),
                "定位片段应含材料值,实际:" + rtype.getSourceSnippet());
        // #7 补比对权利客体:rightObject(文件名) 含 assetName → 一致
        AitCompare obj = cmp.stream().filter(c -> "权利客体".equals(c.getField())).findFirst().orElseThrow();
        assertEquals(AitCompare.DIFF_MATCH, obj.getDiffType(), "客体应与资产名称比对一致");
        // #7 授权范围:确权表单不含此字段 → 标"表单未含此项"而非缺失
        AitCompare scope = cmp.stream().filter(c -> "授权范围".equals(c.getField())).findFirst().orElseThrow();
        assertEquals(AitCompare.DIFF_NA, scope.getDiffType());
    }

    /** #7 权利期限语义对齐:材料时长"3年" → 按申请创建日推算到期,与 validDate 同口径比对(±31天容差)→ 一致,消除误报。 */
    @Test
    void termCompare_semantic_alignment_no_false_mismatch() {
        ConfirmApply apply = new ConfirmApply();
        apply.setAssetId("DA-TERM-1");
        apply.setAssetName("线路资产数据");
        apply.setRightType("使用权");
        apply.setRightHolder("广东电网");
        apply.setValidDate(java.time.LocalDateTime.now().plusYears(3)); // 到期=今+3年
        String applyId = applyService.saveDraft(apply);

        AitMaterial m = new AitMaterial();
        m.setFileName("授权证明.pdf");
        m.setApplyId(applyId);
        m.setContent("使用权,有效期3年,授权范围约定字段"); // 材料时长=3年
        String id = aitService.upload(m);
        aitService.parse(id);

        AitCompare term = aitService.compares(id).stream()
                .filter(c -> "权利期限".equals(c.getField())).findFirst().orElseThrow();
        assertEquals(AitCompare.DIFF_MATCH, term.getDiffType(),
                "材料3年与表单到期日(今+3年)同口径应判一致,而非误报不一致;实际:" + term.getDiffType());
    }

    @Test
    void parse_extracts_operation_right_and_sensitive() {
        AitMaterial m = new AitMaterial();
        m.setFileName("充电桩运营数据-经营授权.pdf");
        m.setContent("经营权,对外经营,涉及个人信息隐私,交易采购");
        String id = aitService.upload(m);
        aitService.parse(id);
        AitParseResult r = aitService.getParse(id);
        assertEquals("经营权", r.getRightType());
        assertEquals("个人信息", r.getSensitiveType());
        assertEquals("交易采购", r.getDataSource());
    }

    /** #1 多格式与批量上传:真实二进制上传的 格式/大小 强校验 + 元数据(哈希/大小/存储)。 */
    @Test
    void uploadBinary_validates_format_size_and_records_metadata() {
        byte[] ok = new byte[120 * 1024]; // 120KB(常规扫描件量级)

        // 合法 PNG 上传 → 元数据齐全、原件可回读
        String id = aitService.uploadBinary("客户用电信息表-盖章.png", ok, null, null, null);
        AitMaterial m = aitService.getMaterial(id);
        assertEquals("PNG", m.getFileType());
        assertEquals(120L, m.getSizeKb());
        assertNotNull(m.getFileHash(), "应记录 SM3 哈希");
        assertNotNull(m.getStoragePath(), "应真实存储(磁盘)");
        assertNotNull(m.getBatchNo(), "应生成批次号");
        assertEquals(AitMaterial.PARSE_PENDING, m.getParseStatus());
        assertEquals(ok.length, aitService.loadFile(id).length, "原件字节应可完整回读");

        // 非法格式拒绝(仅 pdf/doc/docx/jpg/jpeg/png)
        assertThrows(BusinessException.class, () -> aitService.uploadBinary("木马.exe", ok, null, null, null),
                "非法格式应拒绝");
        // 小文件合法(下限仅 1KB,防空文件;质量交解析分类判定)
        String smallId = aitService.uploadBinary("授权函-简短版.pdf", new byte[50 * 1024], null, null, null);
        assertNotNull(smallId, "50KB 合法材料不应被体积下限误拦");
        // 过小拒绝(< 1KB)
        assertThrows(BusinessException.class, () -> aitService.uploadBinary("tiny.pdf", new byte[512], null, null, null),
                "低于 1KB 应拒绝(防空文件)");
        // 空内容拒绝
        assertThrows(BusinessException.class, () -> aitService.uploadBinary("empty.pdf", new byte[0], null, null, null),
                "空文件应拒绝");
    }

    /** #1 批量上限:单次批量上传超过 50 个直接拒绝(控制器层,先于落库)。 */
    @Test
    void parse_missing_material_rejected_synchronously() {
        // @Async 解析派发前必须同步校验材料存在,否则异常被异步线程吞掉、前端误得"成功"
        AitMaterialController controller = new AitMaterialController(aitService, null);
        assertThrows(BusinessException.class, () -> controller.parse("NO-SUCH-MATERIAL"));
    }

    @Test
    void uploadBatch_rejects_more_than_50() {
        MultipartFile[] tooMany = new MultipartFile[51];
        AitMaterialController controller = new AitMaterialController(aitService, null);
        assertThrows(BusinessException.class, () -> controller.uploadBatch(tooMany, null, null),
                "单次批量超过 50 个应拒绝");
    }

    /** #5 印章-OCR 交叉校验:有印章信号但 OCR 正文稀疏未佐证 → 判"可疑"(三态产出),并给材料可信度评级。 */
    @Test
    void seal_crossValidation_flags_suspicious_when_ocr_insufficient() {
        AitMaterial m = new AitMaterial();
        m.setFileName("仅盖章无内容.pdf");
        m.setContent("已盖章"); // 有印章信号,但正文稀疏(<12字),OCR 不充分佐证
        String id = aitService.upload(m);
        aitService.parse(id);
        AitParseResult r = aitService.getParse(id);
        assertEquals("可疑", r.getSealValid(), "印章信号但OCR佐证不足应判可疑,实际:" + r.getSealValid());
        assertNotNull(r.getSealDesc());
        assertNotNull(r.getTrustLevel(), "应产出材料可信度评级");
        assertNotNull(r.getTrustScore());
    }

    /** #4 内置术语库:标准命中 + 别名/模糊 → 标准术语建议。 */
    @Test
    void termLibrary_matches_standard_and_suggests() {
        assertTrue(AitTermLibrary.match(AitTermLibrary.F_RIGHT_TYPE, "经营权").standard(), "经营权为标准术语");
        AitTermLibrary.Match rt = AitTermLibrary.match(AitTermLibrary.F_RIGHT_TYPE, "运营权");
        assertFalse(rt.standard(), "运营权(别名)应判为非标");
        assertEquals("经营权", rt.standardTerm());
        assertEquals("全字段", AitTermLibrary.match(AitTermLibrary.F_AUTH_SCOPE, "全网").standardTerm());
        assertEquals("个人信息", AitTermLibrary.match(AitTermLibrary.F_SENSITIVE, "隐私").standardTerm());
    }

    /** #4 多要素匹配 + 人工确认修改写回:非标授权范围"全网"→建议"全字段"→采用后写回解析结果。 */
    @Test
    void termCheck_multiField_and_confirm_writeback() {
        AitMaterial m = new AitMaterial();
        m.setFileName("术语测试材料.pdf");
        m.setContent("持有权,授权范围全网,自行生产"); // 桩解析 authScope="全网"(非标)
        String id = aitService.upload(m);
        aitService.parse(id);

        List<AitMaterialService.TermSuggestion> terms = aitService.termCheck(id);
        assertEquals(4, terms.size(), "应对 权利类型/授权范围/数据来源/敏感类型 四要素匹配");
        AitMaterialService.TermSuggestion scope = terms.stream()
                .filter(t -> "授权范围".equals(t.field())).findFirst().orElseThrow();
        assertFalse(scope.standard(), "全网应判为非标");
        assertEquals("全字段", scope.standardTerm());

        aitService.confirmTerm(id, "授权范围", "全字段");
        assertEquals("全字段", aitService.getParse(id).getAuthScope(), "采用标准术语应写回解析结果");
    }

    /** #2 失败原因分类:PDF 抽取不到正文(损坏/纯图)→ 解析失败,原因归类为"文件损坏/无法解析"。 */
    @Test
    void parse_classifies_broken_file_when_text_empty() {
        byte[] garbage = new byte[120 * 1024]; // 非有效 PDF,上传时抽取不到正文 → content 为空
        String id = aitService.uploadBinary("损坏的确权证明.pdf", garbage, null, null, null);

        assertThrows(BusinessException.class, () -> aitService.parse(id), "无法解析的文件应抛出失败");
        AitMaterial m = aitService.getMaterial(id);
        assertEquals(AitMaterial.PARSE_FAILED, m.getParseStatus());
        assertEquals(100, m.getProgress());
        assertNotNull(m.getFailReason());
        assertTrue(m.getFailReason().contains("损坏") || m.getFailReason().contains("无法解析"),
                "失败原因应分类为 文件损坏/无法解析,实际:" + m.getFailReason());
    }

    /** #8 导出 Excel:三 Sheet(解析要素/表单比对差异/术语库匹配),含复核标记/可信度/定位片段/术语标注。 */
    @Test
    void exportExcel_three_sheets_with_enrichments() throws Exception {
        AitMaterial m = new AitMaterial();
        m.setFileName("导出测试.pdf");
        m.setContent("持有权,授权范围全网,自行生产,已加盖公章,有效期3年");
        String id = aitService.upload(m);
        aitService.parse(id);

        byte[] xlsx = aitService.exportParseExcel(id);
        assertNotNull(xlsx);
        assertTrue(xlsx.length > 0);
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))) {
            assertEquals("解析要素", wb.getSheetName(0));
            assertEquals("表单比对差异", wb.getSheetName(1));
            assertEquals("术语库匹配", wb.getSheetName(2));
            java.util.List<String> h1 = new java.util.ArrayList<>();
            wb.getSheetAt(0).getRow(0).forEach(c -> h1.add(c.getStringCellValue()));
            assertTrue(h1.contains("复核标记") && h1.contains("材料可信度"), "Sheet1 应含 复核标记/材料可信度,实际:" + h1);
            java.util.List<String> h2 = new java.util.ArrayList<>();
            wb.getSheetAt(1).getRow(0).forEach(c -> h2.add(c.getStringCellValue()));
            assertTrue(h2.contains("原文定位片段"), "Sheet2 应含 原文定位片段,实际:" + h2);
            assertEquals("标准术语建议", wb.getSheetAt(2).getRow(0).getCell(2).getStringCellValue());
            assertTrue(wb.getSheetAt(2).getLastRowNum() >= 1, "术语库匹配应有数据行");
        }
    }
}
