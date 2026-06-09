package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.controller.AitMaterialController;
import com.csg.prm.confirm.aitool.entity.AitCompare;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        apply.setRightType("数据持有权");
        apply.setRightHolder("广东电网");
        String applyId = applyService.saveDraft(apply);

        AitMaterial m = new AitMaterial();
        m.setFileName("客户用电信息表-确权证明-盖章.pdf");
        m.setApplyId(applyId);
        m.setSizeKb(2048L);
        m.setContent("权利主体广东电网,数据持有权,有效期3年,授权范围约定字段,自行生产,已盖章");
        String id = aitService.upload(m);

        AitMaterial saved = aitService.page(new com.csg.prm.common.query.PageQuery(), null, null, applyId)
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
        assertEquals("数据持有权", r.getRightType());
        assertEquals("3年", r.getRightTerm());
        assertEquals("约定字段", r.getAuthScope());
        assertTrue(r.getConfidence() > 0.9);
        assertEquals("有效", r.getSealValid(), "盖章材料应识别印章有效");
        // 低置信度(桩 0.92 < 0.95)→ 标"需人工复核"
        assertEquals("需人工复核", r.getReviewStatus(), "置信度低于 0.95 应标需人工复核,实际:" + r.getReviewStatus());

        // 术语库匹配:持有权为标准术语
        List<AitMaterialService.TermSuggestion> terms = aitService.termCheck(id);
        assertTrue(terms.get(0).standard());

        // 与表单比对:权利主体/类型一致;授权范围表单缺失
        List<AitCompare> cmp = aitService.compares(id);
        assertEquals(4, cmp.size());
        AitCompare rtype = cmp.stream().filter(c -> "权利类型".equals(c.getField())).findFirst().orElseThrow();
        assertEquals(AitCompare.DIFF_MATCH, rtype.getDiffType());
        AitCompare scope = cmp.stream().filter(c -> "授权范围".equals(c.getField())).findFirst().orElseThrow();
        assertEquals(AitCompare.DIFF_MISSING, scope.getDiffType());
    }

    @Test
    void parse_extracts_operation_right_and_sensitive() {
        AitMaterial m = new AitMaterial();
        m.setFileName("充电桩运营数据-经营授权.pdf");
        m.setContent("数据产品经营权,对外经营,涉及个人信息隐私,交易采购");
        String id = aitService.upload(m);
        aitService.parse(id);
        AitParseResult r = aitService.getParse(id);
        assertEquals("数据产品经营权", r.getRightType());
        assertEquals("个人信息", r.getSensitiveType());
        assertEquals("交易采购", r.getDataSource());
    }

    /** #1 多格式与批量上传:真实二进制上传的 格式/大小 强校验 + 元数据(哈希/大小/存储)。 */
    @Test
    void uploadBinary_validates_format_size_and_records_metadata() {
        byte[] ok = new byte[120 * 1024]; // 120KB ≥ 100KB 下限

        // 合法 PNG 上传 → 元数据齐全、原件可回读
        String id = aitService.uploadBinary("客户用电信息表-盖章.png", ok, null, null);
        AitMaterial m = aitService.getMaterial(id);
        assertEquals("PNG", m.getFileType());
        assertEquals(120L, m.getSizeKb());
        assertNotNull(m.getFileHash(), "应记录 SM3 哈希");
        assertNotNull(m.getStoragePath(), "应真实存储(磁盘)");
        assertNotNull(m.getBatchNo(), "应生成批次号");
        assertEquals(AitMaterial.PARSE_PENDING, m.getParseStatus());
        assertEquals(ok.length, aitService.loadFile(id).length, "原件字节应可完整回读");

        // 非法格式拒绝(仅 pdf/doc/docx/jpg/jpeg/png)
        assertThrows(BizException.class, () -> aitService.uploadBinary("木马.exe", ok, null, null),
                "非法格式应拒绝");
        // 过小拒绝(< 100KB)
        assertThrows(BizException.class, () -> aitService.uploadBinary("small.pdf", new byte[50 * 1024], null, null),
                "单文件低于 100KB 应拒绝");
        // 空内容拒绝
        assertThrows(BizException.class, () -> aitService.uploadBinary("empty.pdf", new byte[0], null, null),
                "空文件应拒绝");
    }

    /** #1 批量上限:单次批量上传超过 50 个直接拒绝(控制器层,先于落库)。 */
    @Test
    void uploadBatch_rejects_more_than_50() {
        MultipartFile[] tooMany = new MultipartFile[51];
        AitMaterialController controller = new AitMaterialController(aitService);
        assertThrows(BizException.class, () -> controller.uploadBatch(tooMany, null),
                "单次批量超过 50 个应拒绝");
    }

    /** #2 失败原因分类:PDF 抽取不到正文(损坏/纯图)→ 解析失败,原因归类为"文件损坏/无法解析"。 */
    @Test
    void parse_classifies_broken_file_when_text_empty() {
        byte[] garbage = new byte[120 * 1024]; // 非有效 PDF,上传时抽取不到正文 → content 为空
        String id = aitService.uploadBinary("损坏的确权证明.pdf", garbage, null, null);

        assertThrows(BizException.class, () -> aitService.parse(id), "无法解析的文件应抛出失败");
        AitMaterial m = aitService.getMaterial(id);
        assertEquals(AitMaterial.PARSE_FAILED, m.getParseStatus());
        assertEquals(100, m.getProgress());
        assertNotNull(m.getFailReason());
        assertTrue(m.getFailReason().contains("损坏") || m.getFailReason().contains("无法解析"),
                "失败原因应分类为 文件损坏/无法解析,实际:" + m.getFailReason());
    }
}
