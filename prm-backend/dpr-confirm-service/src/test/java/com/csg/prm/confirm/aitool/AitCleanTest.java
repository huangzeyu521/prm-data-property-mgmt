package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.dto.AitCleanRequest;
import com.csg.prm.confirm.aitool.entity.AitAuditBase;
import com.csg.prm.confirm.aitool.entity.AitCleanLog;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.service.AitCleanService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.2 数据清洗与标准化处理(#1~#6)能力测试。useModel=false → 纯规则,确定性可重复。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitCleanTest {

    @Autowired private AitCleanService cleanService;
    @Autowired private AitMaterialService materialService;

    private String newMaterial(String name) {
        AitMaterial m = new AitMaterial();
        m.setFileName(name);
        m.setContent("占位");
        return materialService.upload(m);
    }

    private AitAuditBase find(List<AitAuditBase> base, String field) {
        return base.stream().filter(a -> field.equals(a.getTemplateField())).findFirst().orElse(null);
    }

    /** #1/#2/#3:去噪+全半角+命名规范化+枚举术语归一+布尔识别+空值识别。 */
    @Test
    void clean_normalizes_noise_enum_bool_and_missing() {
        String id = newMaterial("用户表清洗.xlsx");
        Map<String, String> row = new LinkedHashMap<>();
        row.put("表名", " 用户  表 ");
        row.put("字段名", "ｕｓｅｒ＿ｉｄ");           // 全角
        row.put("权利类型", "持有权");                 // 非标 → 术语归一
        row.put("数据来源", "自产");                   // 非标 → 术语归一
        row.put("是否个人信息", "Y");                  // 布尔识别
        row.put("系统名称", "");                       // 空值
        AitCleanRequest req = new AitCleanRequest();
        req.setRows(List.of(row));
        req.setUseModel(false);

        AitCleanService.CleanResult r = cleanService.clean(id, req);
        List<AitAuditBase> base = r.auditBase();

        assertEquals("用户_表", find(base, "tableName").getCleanValue(), "去空白+命名规范化");
        assertEquals("user_id", find(base, "fieldName").getCleanValue(), "全角→半角");
        assertEquals("数据持有权", find(base, "rightType").getCleanValue(), "权利类型术语归一");
        assertEquals("自行生产", find(base, "dataSource").getCleanValue(), "数据来源术语归一");
        assertEquals("是", find(base, "isPersonalInfo").getCleanValue(), "布尔项识别");
        assertEquals(AitAuditBase.ST_MISSING, find(base, "systemName").getStatus(), "空值→缺失");
        assertEquals(AitAuditBase.ST_MISSING, find(base, "fieldDesc").getStatus(), "未提供字段→缺失");
    }

    /** #3:枚举越界 → 异常 + 待补正建议。 */
    @Test
    void clean_flags_out_of_enum_as_abnormal() {
        String id = newMaterial("异常枚举.xlsx");
        Map<String, String> row = new LinkedHashMap<>();
        row.put("敏感类型", "火星数据");
        AitCleanRequest req = new AitCleanRequest();
        req.setRows(List.of(row));
        req.setUseModel(false);

        AitCleanService.CleanResult r = cleanService.clean(id, req);
        AitAuditBase st = find(r.auditBase(), "sensitiveType");
        assertEquals(AitAuditBase.ST_ABNORMAL, st.getStatus(), "未知枚举值应判异常");
        assertNotNull(st.getSuggestion(), "应给出待补正建议(标准值域)");
    }

    /** #3:字段对齐冲突识别(两原始键映射同一模板字段且取值不一致)。 */
    @Test
    void clean_detects_field_alignment_conflict() {
        String id = newMaterial("冲突.xlsx");
        Map<String, String> row = new LinkedHashMap<>();
        row.put("表名", "USER_A");
        row.put("数据表", "USER_B"); // 与"表名"对齐到同一字段,值冲突
        AitCleanRequest req = new AitCleanRequest();
        req.setRows(List.of(row));
        req.setUseModel(false);

        AitCleanService.CleanResult r = cleanService.clean(id, req);
        assertEquals(AitAuditBase.ST_CONFLICT, find(r.auditBase(), "tableName").getStatus());
    }

    /** #2:重复记录识别。 */
    @Test
    void clean_detects_duplicate_records() {
        String id = newMaterial("重复.xlsx");
        Map<String, String> row = new LinkedHashMap<>();
        row.put("表名", "ORDER_T");
        row.put("字段名", "order_id");
        row.put("系统名称", "营销系统");
        AitCleanRequest req = new AitCleanRequest();
        req.setRows(List.of(row, new LinkedHashMap<>(row))); // 两行完全相同
        req.setUseModel(false);

        AitCleanService.CleanResult r = cleanService.clean(id, req);
        assertTrue(r.auditBase().stream().anyMatch(a -> AitAuditBase.ST_DUPLICATE.equals(a.getStatus())),
                "完全相同的第二行应判重复");
    }

    /** #4/#5/#6:统一审核底表 + 待补正清单 + 清洗日志可追溯。 */
    @Test
    void clean_produces_auditbase_pending_and_log() {
        String id = newMaterial("底表日志.xlsx");
        Map<String, String> row = new LinkedHashMap<>();
        row.put("表名", "CUST");
        row.put("权利类型", "经营权"); // 归一→数据产品经营权
        row.put("敏感类型", "未知敏感"); // 异常
        AitCleanRequest req = new AitCleanRequest();
        req.setRows(List.of(row));
        req.setUseModel(false);
        cleanService.clean(id, req);

        // #4 审核底表
        List<AitAuditBase> base = cleanService.auditBase(id);
        assertFalse(base.isEmpty(), "应生成统一审核底表");
        assertEquals("数据产品经营权", find(base, "rightType").getCleanValue());
        // #5 待补正清单(只含非正常项)
        List<AitAuditBase> pending = cleanService.pending(id);
        assertFalse(pending.isEmpty(), "应有待补正项");
        assertTrue(pending.stream().noneMatch(a -> AitAuditBase.ST_OK.equals(a.getStatus())),
                "待补正清单不应含正常项");
        // #6 清洗日志可追溯(原始值→规则→结果→方式)
        List<AitCleanLog> logs = cleanService.cleanLog(id);
        AitCleanLog rtLog = logs.stream().filter(l -> "rightType".equals(l.getField())).findFirst().orElse(null);
        assertNotNull(rtLog, "应有权利类型清洗日志");
        assertEquals("经营权", rtLog.getOriginalValue());
        assertEquals("数据产品经营权", rtLog.getCleanedValue());
        assertNotNull(rtLog.getRule());
        assertEquals(AitCleanLog.METHOD_RULE, rtLog.getMethod());
    }

    /** 1.1.1.1#4 结构化模板上传 → 与材料抽取内容自动关联审核 → 对比日志 + 下载。 */
    @Test
    void template_compare_maps_audits_and_logs() {
        AitMaterial m = new AitMaterial();
        m.setFileName("确权材料-模板对比.docx");
        m.setContent("权利主体:广东电网有限责任公司,数据持有权,自行生产,电网生产数据,有效期3年,已盖章");
        String id = materialService.upload(m);
        materialService.parse(id);

        String tpl = "权利类型：数据持有权\n数据来源：自行生产\n敏感类型：电网生产数据\n授权范围：不存在的范围XYZ";
        AitCleanService.TplCompareResult r = cleanService.templateCompare(
                id, "结构化模板.txt", tpl.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertNotNull(r);
        assertFalse(r.rows().isEmpty(), "应生成对比行");
        assertTrue(r.matched() >= 1, "权利类型/数据来源应判一致");

        List<com.csg.prm.confirm.aitool.entity.AitTplCompare> log = cleanService.templateCompareLog(id);
        assertEquals(r.rows().size(), log.size(), "对比日志应落库");
        var rtRow = log.stream().filter(x -> x.getTplField().contains("权利类型")).findFirst().orElse(null);
        assertNotNull(rtRow, "应含权利类型对比行");
        assertEquals(com.csg.prm.confirm.aitool.entity.AitTplCompare.C_MATCH, rtRow.getConsistency());

        cleanService.templateCompare(id, "模板2.txt",
                "权利主体：广东电网有限责任公司".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(cleanService.templateCompareLog(id).size() > log.size(), "多模板应累积");

        byte[] csv = cleanService.exportTemplateCompare(id);
        String text = new String(csv, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(text.contains("模板名称,模板字段,模板值,材料抽取值,一致性,材料中所在位置"), "CSV 应含表头");
        assertTrue(text.contains("数据持有权"), "CSV 应含对比值");
    }

    /** 1.3#1 来源字段:termCheck 返回值在材料中的定位(sourceLocation)。 */
    @Test
    void term_check_returns_source_location() {
        AitMaterial m = new AitMaterial();
        m.setFileName("来源定位.docx");
        m.setContent("本材料数据来源为自行生产,涉及电网生产数据,数据持有权,有效期3年。");
        String id = materialService.upload(m);
        materialService.parse(id);
        List<AitMaterialService.TermSuggestion> terms = materialService.termCheck(id);
        assertFalse(terms.isEmpty());
        assertTrue(terms.stream().allMatch(t -> t.sourceLocation() != null), "每项应带来源定位");
        var ds = terms.stream().filter(t -> "数据来源".equals(t.field())).findFirst().orElse(null);
        assertNotNull(ds);
        assertTrue(ds.sourceLocation().contains("偏移") || ds.sourceLocation().contains("未在"),
                "来源应标注所在位置,实际=" + ds.sourceLocation());
    }
}
