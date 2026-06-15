package com.csg.prm.confirm.aitool;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.dto.AitCleanRequest;
import com.csg.prm.confirm.aitool.entity.AitAuditBase;
import com.csg.prm.confirm.aitool.entity.AitDocTemplate;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseConfig;
import com.csg.prm.confirm.aitool.entity.AitParseRecord;
import com.csg.prm.confirm.aitool.service.AitCleanService;
import com.csg.prm.confirm.aitool.service.AitDocTemplateService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.aitool.service.AitParseConfigService;
import com.csg.prm.confirm.aitool.service.AitParseRecordService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.4 材料解析与管理(#1~#4)能力测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitParseMgmtTest {

    @Autowired private AitMaterialService materialService;
    @Autowired private AitParseRecordService recordService;
    @Autowired private AitDocTemplateService templateService;
    @Autowired private AitParseConfigService configService;
    @Autowired private AitCleanService cleanService;

    /** #1 解析记录档:解析后逐字段留档(字段/值/置信度/操作人),可查询与导出。 */
    @Test
    void parse_records_archived_queryable_exportable() {
        AitMaterial m = new AitMaterial();
        m.setFileName("确权材料-记录.docx");
        m.setContent("数据加工使用权,自行生产,电网生产数据,有效期3年,已盖章");
        String id = materialService.upload(m);
        materialService.parse(id);

        PageResult<AitParseRecord> page = recordService.page(new PageQuery(), "确权材料-记录", null, null);
        assertTrue(page.getTotal() >= 1, "解析后应有记录档");
        AitParseRecord rt = page.getRecords().stream()
                .filter(r -> "权利类型".equals(r.getField())).findFirst().orElse(null);
        assertNotNull(rt, "应留档权利类型");
        assertEquals("数据加工使用权", rt.getFieldValue());
        assertNotNull(rt.getConfidence(), "应记录置信度");
        assertNotNull(rt.getOperatorId(), "应记录操作人");

        byte[] csv = recordService.exportCsv("确权材料-记录", null, null);
        String text = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("解析时间,文档名称,提取字段"), "CSV 应含表头");
        assertTrue(text.contains("数据加工使用权"), "CSV 应含留档值");
    }

    /** #2 批量解析:同批次排队解析,聚合进度可见。 */
    @Test
    void batch_parse_queues_and_aggregates_progress() throws Exception {
        String batch = "BATCH-MGMT-" + System.nanoTime();
        materialService.uploadBinary("批量A.docx", docx("数据持有权,自行生产,3年,已盖章 A"), null, batch);
        materialService.uploadBinary("批量B.docx", docx("数据加工使用权,交易采购,5年,已盖章 B"), null, batch);

        int dispatched = materialService.batchParse(batch);
        assertEquals(2, dispatched, "应派发 2 个材料解析");

        AitMaterialService.BatchProgress bp = materialService.batchProgress(batch);
        assertEquals(2, bp.total(), "批次聚合应含 2 个材料");
    }

    /** #3 资料模板库:标准模板已种入;新建+版本管理+下载。 */
    @Test
    void doc_template_library_versioning_and_download() {
        // 启动种入的标准模板
        PageResult<AitDocTemplate> seeded = templateService.page(new PageQuery(), null, null, true);
        assertTrue(seeded.getTotal() >= 3, "应种入确权书/授权函/权属证明标准模板");

        AitDocTemplate t = new AitDocTemplate();
        t.setTemplateType("确权书");
        t.setTemplateName("自定义确权书-" + System.nanoTime());
        t.setContent("v1 正文");
        String id = templateService.create(t);
        assertEquals("v1", templateService.getById(id).getVersion());

        AitDocTemplate v2 = new AitDocTemplate();
        v2.setTemplateType("确权书");
        v2.setTemplateName(t.getTemplateName());
        v2.setContent("v2 正文");
        templateService.newVersion(v2);

        List<AitDocTemplate> versions = templateService.versions(t.getTemplateName());
        assertEquals(2, versions.size(), "应有两个版本");
        long latest = versions.stream().filter(x -> Boolean.TRUE.equals(x.getIsLatest())).count();
        assertEquals(1, latest, "只应有一个最新版");

        assertTrue(templateService.download(id).length > 0, "应可下载");
    }

    /** #4 解析元数据配置:默认阈值 + 自定义场景阈值/字段映射;字段映射并入清洗对齐。 */
    @Test
    void parse_config_threshold_and_field_mapping() {
        assertEquals(0.95, configService.threshold(AitParseConfig.DEFAULT_SCENE), 0.0001, "默认阈值 0.95");

        AitParseConfig c = new AitParseConfig();
        c.setScene("测试场景-" + System.nanoTime());
        c.setConfidenceThreshold(0.8);
        c.setFieldMappingJson("{\"我的表名\":\"tableName\"}");
        c.setEnabled(1);
        configService.save(c);
        assertEquals(0.8, configService.threshold(c.getScene()), 0.0001, "自定义场景阈值生效");
        assertEquals("tableName", configService.fieldMapping(c.getScene()).get("我的表名"));

        // 把字段映射加入默认配置 → 清洗对齐应识别自定义原始字段名
        AitParseConfig def = configService.effective(AitParseConfig.DEFAULT_SCENE);
        def.setFieldMappingJson("{\"我的表名\":\"tableName\"}");
        configService.save(def);

        AitMaterial m = new AitMaterial();
        m.setFileName("配置映射.xlsx");
        m.setContent("占位");
        String id = materialService.upload(m);
        Map<String, String> row = new LinkedHashMap<>();
        row.put("我的表名", "ORDER_T"); // 非内置别名,靠配置映射对齐
        AitCleanRequest req = new AitCleanRequest();
        req.setRows(List.of(row));
        req.setUseModel(false);
        AitCleanService.CleanResult r = cleanService.clean(id, req);
        AitAuditBase tableName = r.auditBase().stream()
                .filter(a -> "tableName".equals(a.getTemplateField())).findFirst().orElse(null);
        assertNotNull(tableName);
        assertEquals("ORDER_T", tableName.getCleanValue(), "配置字段映射应使自定义字段名对齐到表名");
    }

    private byte[] docx(String text) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText(text);
            doc.write(bos);
        }
        return bos.toByteArray();
    }
}
