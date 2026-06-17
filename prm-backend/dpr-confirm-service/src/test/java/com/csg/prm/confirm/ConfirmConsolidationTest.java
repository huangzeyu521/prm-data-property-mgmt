package com.csg.prm.confirm;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmTableItem;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmConsolidationService;
import com.csg.prm.confirm.service.ConfirmConsolidationService.ConsolidationResult;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 表级确权清单(M02)与权益归集判定测试。
 * 规则与样例取自南网评审资料:《数据权益内部管理汇总表》说明页 + 真实补录工单(非车险理赔系统/黔电小智)。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmConsolidationTest {

    @Autowired
    private ConfirmConsolidationService service;
    @Autowired
    private ConfirmApplyService applyService;

    private String newApply(String asset, String rightType, String regulated, boolean third) {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId(asset);
        a.setAssetName(asset + "系统");
        a.setRightType(rightType);
        a.setRightHolder("贵州电网有限责任公司");
        a.setRegulated(regulated);
        a.setInvolvesThirdParty(third);
        return applyService.saveDraft(a);
    }

    private ConfirmTableItem item(String code, String name, String secret, String source,
                                  String gFlag, String hFlag) {
        ConfirmTableItem it = new ConfirmTableItem();
        it.setInstanceName("XC_ORA_ZH01");
        it.setSchemaName("NCLAIMUSER");
        it.setTableCode(code);
        it.setTableName(name);
        it.setTableComment(name);
        it.setSecretLevel(secret);
        it.setSourceType(source);
        it.setSourceSubject("鼎和保险");
        it.setGFlag(gFlag);
        it.setGSubject("是".equals(gFlag) ? "国家金融监督管理总局" : "");
        it.setHFlag(hFlag);
        it.setHSubject("是".equals(hFlag) ? "涉及用户个人信息,无授权协议" : "");
        return it;
    }

    @Test
    void apply_no_follows_mdau_format() {
        String id = newApply("DA-MDAU-1", "数据资源持有权", "非管制", false);
        String no = applyService.getById(id).getApplyNo();
        assertTrue(no.matches("MDAU-00-\\d{8}-\\d{5}"), "申请编号应符合 MDAU-00-日期-5位序号:" + no);
    }

    @Test
    void table_items_save_and_list() {
        String id = newApply("DA-M02-1", "数据资源持有权", "非管制", false);
        int n = service.saveTableItems(id, List.of(
                item("PRPLCASEHANDLEPERMISSION", "线下案件批量处理权限配置表", "不涉密", "A 自行生产数据", "是", "是"),
                item("PRPLNEGOTIATIONS", "谈判表", "敏感信息", "A 自行生产数据", "是", "是")));
        assertEquals(2, n);
        List<ConfirmTableItem> items = service.listTableItems(id);
        assertEquals(2, items.size());
        assertEquals("敏感信息", items.stream().filter(i -> "谈判表".equals(i.getTableName()))
                .findFirst().orElseThrow().getSecretLevel());
    }

    @Test
    void rule_1_1_self_produced_regulated_consolidates_operate_right() {
        // 黔电小智:自行生产/不涉三方/管制业务 → 经营权调整为有,归集网公司
        String id = newApply("DA-QXZ-1", "数据资源持有权、数据加工使用权", "管制业务", false);
        service.saveTableItems(id, List.of(item("MEETING_ROOM_INFO", "日程会议室表", "工作秘密", "A 自行生产数据", "否", "否")));
        ConsolidationResult r = service.judgeConsolidation(id);
        assertEquals("1.1", r.rule());
        assertEquals("有", r.operateRight(), "管制单位经营权应调整为有,确权时直接归属网公司");
        assertTrue(r.reason().contains("确权时直接归属网公司"), "应表述为确权直接归属(无转让):" + r.reason());
        assertFalse(r.reason().contains("可转移") || r.reason().contains("转移后") || r.reason().contains("转移判定"),
                "不应含转让/转移动作叙事:" + r.reason());
        assertTrue(r.reason().contains("确权授权工作指引"));
    }

    @Test
    void rule_1_2_self_produced_unregulated_syncs_rights() {
        String id = newApply("DA-R12-1", "数据资源持有权、数据产品经营权", "非管制", false);
        service.saveTableItems(id, List.of(item("T1", "表1", "不涉密", "A 自行生产数据", "否", "否")));
        ConsolidationResult r = service.judgeConsolidation(id);
        assertEquals("1.2", r.rule());
        assertEquals("有", r.operateRight(), "非管制有经营权应同步给网公司");
    }

    @Test
    void rule_2_2_third_party_unregulated_no_operate() {
        String id = newApply("DA-R22-1", "数据资源持有权", "非管制", false);
        service.saveTableItems(id, List.of(item("T2", "表2", "不涉密", "B 公开采集数据", "否", "否")));
        ConsolidationResult r = service.judgeConsolidation(id);
        assertEquals("2.2", r.rule());
        assertEquals("无", r.operateRight(), "涉三方非管制无经营权,网公司无经营权");
        assertTrue(r.involvesThird());
    }

    @Test
    void rule_3_1_third_party_regulated_judges_after_restore() {
        // 非车险理赔:涉监管(G)+个人隐私(H)/管制 → 恢复经营权后依权益判定
        String id = newApply("DA-FCX-1", "数据资源持有权", "管制业务", false);
        service.saveTableItems(id, List.of(item("PRPLDLOSSCONFIG", "人伤费用配置信息表", "不涉密", "A 自行生产数据", "是", "是")));
        ConsolidationResult r = service.judgeConsolidation(id);
        assertEquals("3.1", r.rule());
        assertEquals("依权益判定", r.operateRight());
    }

    @Test
    void exports_produce_official_summary_workbooks() throws Exception {
        String id = newApply("DA-EXP-1", "数据资源持有权、数据加工使用权", "管制业务", false);
        service.saveTableItems(id, List.of(item("MEETING_ROOM_INFO", "日程会议室表", "工作秘密", "A 自行生产数据", "否", "否")));

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.exportConfirmSummary()))) {
            XSSFSheet s = wb.getSheet("数据确权信息汇总表");
            assertEquals("密级（不涉密/核心商密/普通商密/工作秘密/敏感信息）", s.getRow(0).getCell(8).getStringCellValue());
            assertTrue(s.getLastRowNum() >= 1, "应有数据行");
        }
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(service.exportEquityConsolidation()))) {
            XSSFSheet s = wb.getSheet("数据权益内部管理汇总表");
            assertEquals("权益主体", s.getRow(0).getCell(0).getStringCellValue());
            assertEquals("管制/非管制", s.getRow(0).getCell(3).getStringCellValue());
            int last = s.getRow(0).getLastCellNum() - 1;
            assertEquals("共享判定原因", s.getRow(0).getCell(last).getStringCellValue());
            // 导出含全库申请,按系统名定位本用例数据行再校验列对齐
            org.apache.poi.ss.usermodel.Row mine = null;
            for (int r = 1; r <= s.getLastRowNum(); r++) {
                if (s.getRow(r) != null && "DA-EXP-1系统".equals(s.getRow(r).getCell(1).getStringCellValue())) {
                    mine = s.getRow(r);
                    break;
                }
            }
            assertTrue(mine != null, "应包含本用例申请的数据行");
            assertEquals("中国南方电网有限责任公司", mine.getCell(0).getStringCellValue());
            assertEquals("管制业务", mine.getCell(3).getStringCellValue(), "数据行应与表头列对齐");
            assertEquals(last + 1, (int) mine.getLastCellNum(), "数据行列数应与表头一致");
            assertTrue(mine.getCell(last).getStringCellValue().contains("确权授权工作指引"),
                    "末列应为标准共享判定原因");
        }
    }
}
