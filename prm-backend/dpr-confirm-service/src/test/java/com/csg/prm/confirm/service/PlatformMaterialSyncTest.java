package com.csg.prm.confirm.service;

import com.csg.prm.confirm.dto.MaterialCheckReport;
import com.csg.prm.confirm.dto.MaterialSyncReport;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import com.csg.prm.confirm.entity.ConfirmTableItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 确权材料"先从平台元数据(AU_TABLE_META_DATA)同步已上传材料、再补全"闭环:
 * AST-001 平台已上传 来源/证明(SOURCE_NAME)、行政监管(CHECK_NAME,G)、个人隐私(PRIVACY_NAME,H)三类附件;
 * 同步后这些应交项自动登记为"平台同步"免上传且材料校验直接通过;表1/表2(系统表单)仍待用户补全;同步幂等。
 */
@SpringBootTest
@ActiveProfiles("test")
class PlatformMaterialSyncTest {

    @Autowired
    private com.csg.prm.confirm.service.ConfirmApplyService applyService;
    @Autowired
    private ConfirmMaterialService materialService;
    @Autowired
    private com.csg.prm.confirm.service.ConfirmConsolidationService consolidationService;
    @Autowired
    private com.csg.prm.confirm.integration.DataCatalogService dataCatalogService;

    private String draftAst001() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("AST-001");
        a.setAssetName("客户用电信息表");
        a.setRightType("使用权");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化部");
        a.setSourceIdentification("A");          // A 自行生产 → 平台 SOURCE_NAME
        a.setRelationIdentification("G,H");       // G 行政监管 + H 个人隐私 → 平台 CHECK_NAME / PRIVACY_NAME
        a.setInvolvesThirdParty(Boolean.TRUE);    // 涉三方 → 表2 入应交清单
        a.setThirdPartyInfo("来源主体:用电客户");
        return applyService.saveDraft(a);
    }

    @Test
    void sync_pulls_platform_uploaded_materials_and_marks_them() {
        String id = draftAst001();
        MaterialSyncReport rep = materialService.syncFromPlatform(id);

        // P3:系统级同步 = 证明材料 + A 来源说明 + 自动生成 表1/表2 = 4(G/H 已逐表化,不再系统级同步)
        assertEquals(4, rep.getSyncedCount(), "P3:系统级同步 证明+A + 表1/表2(G/H逐表化),实际 " + rep.getSyncedCount());
        assertTrue(rep.getSynced().stream().anyMatch(s -> s.getMaterialName().contains("证明材料")), "应同步 证明材料");
        assertTrue(rep.getSynced().stream().anyMatch(s -> "A".equals(s.getCode())), "应同步 A 来源说明");
        assertFalse(rep.getSynced().stream().anyMatch(s -> "G".equals(s.getCode()) || "H".equals(s.getCode())),
                "P3:G/H 已逐表化(ConfirmTableItem),系统级不再同步");
        assertTrue(rep.getSynced().stream().anyMatch(s -> "系统生成".equals(s.getCode())), "应自动生成 表1/表2");

        // 登记的材料:平台覆盖项=平台同步,表1/表2=系统生成;均带文件名 + 校验通过(免上传)
        List<ConfirmMaterial> mats = materialService.listByApply(id);
        assertTrue(mats.stream().allMatch(m -> ConfirmMaterial.SOURCE_PLATFORM.equals(m.getSource()) || "系统生成".equals(m.getSource())),
                "本次登记材料应为 平台同步 或 系统生成");
        assertTrue(mats.stream().allMatch(m -> m.getFileName() != null && !m.getFileName().isBlank()),
                "材料应带文件名");
        assertTrue(mats.stream().allMatch(m -> ConfirmMaterial.CHECK_PASS.equals(m.getCheckResult())),
                "平台同步/系统生成材料校验应直接通过");

        // 表1/表2 由系统据申报内容自动生成,不再待用户补全;全部应交项已齐
        assertFalse(rep.getStillMissing().stream().anyMatch(n -> n.contains("表1")), "表1 应系统生成,不待补全");
        assertFalse(rep.getStillMissing().stream().anyMatch(n -> n.contains("表2")), "表2 应系统生成,不待补全");
        assertTrue(rep.getStillMissing().isEmpty(), "AST-001 应交项应已全齐(平台覆盖+系统生成)");
    }

    @Test
    void synced_materials_are_downloadable_for_online_preview() {
        String id = draftAst001();
        materialService.syncFromPlatform(id);
        List<ConfirmMaterial> mats = materialService.listByApply(id);
        // 平台同步材料应已落地平台原件字节,可经 download 在线预览(.docx 由前端 docx-preview 渲染)
        assertTrue(mats.stream().anyMatch(m -> m.getFileName() != null && m.getFileName().endsWith(".docx")),
                "平台同步材料应带 .docx 平台附件名");
        for (ConfirmMaterial m : mats) {
            byte[] bytes = materialService.download(m.getMaterialId());
            assertNotNull(bytes, "平台同步材料应可下载原件字节(供预览):" + m.getMaterialName());
            assertTrue(bytes.length > 0, "平台同步材料原件不应为空:" + m.getMaterialName());
        }
    }

    @Test
    void source_materials_map_to_their_own_letter_attachment() {
        // 系统级申请(SYS:客户服务系统):库表来源 A/B/C/F + 关联 H。
        // 回归:修复前 A–F 会被拍平成"首表(A)附件",B/C/F 张冠李戴 A 的说明。
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("SYS:客户服务系统");
        a.setAssetName("客户服务系统");
        a.setRightType("持有权");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化部");
        a.setSourceIdentification("A,B,C,F");
        a.setRelationIdentification("H");
        a.setInvolvesThirdParty(Boolean.TRUE);
        String id = applyService.saveDraft(a);

        MaterialSyncReport rep = materialService.syncFromPlatform(id);
        java.util.Map<String, String> attByCode = new java.util.HashMap<>();
        for (MaterialSyncReport.SyncedItem s : rep.getSynced()) {
            if (s.getCode() != null && s.getCode().length() == 1) {
                attByCode.put(s.getCode(), s.getAttachment());
            }
        }
        // P3:系统级同步只含 A(自行生产·系统级);B/C/F/H 逐表化,不在系统级
        assertEquals("数据来源与系统建设投入说明.pdf", attByCode.get("A"), "A 系统级说明应同步");
        assertFalse(attByCode.containsKey("B") || attByCode.containsKey("C")
                        || attByCode.containsKey("F") || attByCode.containsKey("H"),
                "P3:B/C/F/H 已逐表化(ConfirmTableItem),系统级不再同步");

        // 逐字母各表专属来源凭证的单一真源现在是 ConfirmTableItem(平台预填自 cardsBySystem):B/C/F 各不相同
        java.util.List<com.csg.prm.confirm.integration.dto.PlatformTableMeta> cards =
                dataCatalogService.cardsBySystem("客户服务系统", null, null);
        java.util.Map<Character, String> bySrc = new java.util.HashMap<>();
        for (com.csg.prm.confirm.integration.dto.PlatformTableMeta m : cards) {
            char sc = (m.sourceType() == null || m.sourceType().isEmpty()) ? ' ' : m.sourceType().charAt(0);
            if ("BCF".indexOf(sc) >= 0) {
                bySrc.put(sc, m.sourceAttachment());
            }
        }
        assertEquals("公共采集情况说明.pdf", bySrc.get('B'), "B 表来源凭证专属");
        assertEquals("公共数据授权说明.pdf", bySrc.get('C'), "C 表来源凭证专属");
        assertEquals("其他来源情况说明.pdf", bySrc.get('F'), "F 表来源凭证专属");
        assertEquals(3, new java.util.HashSet<>(java.util.List.of(bySrc.get('B'), bySrc.get('C'), bySrc.get('F'))).size(),
                "B/C/F 来源凭证各不相同");
    }

    @Test
    void p3_bj_credentials_are_per_table_not_system_level() {
        // 客户服务系统:A/C 来源 + H 关联。P3:B–F(非A)/G–J 逐表(ConfirmTableItem),系统级不再建 B–J ConfirmMaterial;
        //   runCheck 对 B–J 逐表查附件——C 来源凭证齐→不报缺;某表 H 关联资料缺→精确报"表·H个人隐私资料"。
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("SYS:客户服务系统");
        a.setAssetName("客户服务系统");
        a.setRightType("持有权");
        a.setRightHolder("广东电网有限责任公司");
        a.setRespDept("数字化部");
        a.setSourceIdentification("A,C");
        a.setRelationIdentification("H");
        a.setInvolvesThirdParty(Boolean.TRUE);
        String id = applyService.saveDraft(a);

        // 逐表:C 来源表(来源凭证齐)+ A源但涉H的表(H 关联资料缺)
        ConfirmTableItem c = perTbl("T_C", "C 公共授权数据", "否");
        c.setSourceAttachment("公共数据授权说明.pdf");
        ConfirmTableItem h = perTbl("T_H", "A 自行生产数据", "是"); // A 源无来源凭证槽;H=是 但 privacyAttachment 空
        consolidationService.saveTableItems(id, java.util.List.of(c, h));

        // 同步:系统级不再含 C/H(逐表化)
        MaterialSyncReport rep = materialService.syncFromPlatform(id);
        assertFalse(rep.getSynced().stream().anyMatch(s -> "C".equals(s.getCode()) || "H".equals(s.getCode())),
                "P3:C/H 应逐表化,系统级同步不含,实际 " + rep.getSynced().stream().map(MaterialSyncReport.SyncedItem::getCode).toList());

        // 校验逐表化:H 表关联资料缺 → 精确报缺;C 来源凭证齐 → 不报缺
        MaterialCheckReport chk = materialService.runCheck(id);
        assertTrue(chk.getMissing().stream().anyMatch(x -> x.contains("H个人隐私资料")),
                "H 关联资料缺应逐表报缺,实际 missing=" + chk.getMissing());
        assertFalse(chk.getMissing().stream().anyMatch(x -> x.contains("来源凭证(C)")),
                "C 来源凭证齐不应报缺,实际 missing=" + chk.getMissing());
    }

    private ConfirmTableItem perTbl(String code, String source, String hFlag) {
        ConfirmTableItem it = new ConfirmTableItem();
        it.setTableCode(code);
        it.setTableName(code);
        it.setSchemaName("S");
        it.setSourceType(source);
        it.setGFlag("否");
        it.setHFlag(hFlag);
        it.setIFlag("否");
        it.setJFlag("否");
        return it;
    }

    @Test
    void sync_is_idempotent() {
        String id = draftAst001();
        materialService.syncFromPlatform(id);
        MaterialSyncReport again = materialService.syncFromPlatform(id);
        assertEquals(0, again.getSyncedCount(), "重复同步不应重复登记");
    }

    @Test
    void synced_materials_pass_material_check() {
        String id = draftAst001();
        materialService.syncFromPlatform(id);
        MaterialCheckReport check = materialService.runCheck(id);
        // 平台已覆盖的 4 项应判通过(有原件附件名)
        assertTrue(check.getPassCount() >= 4, "平台同步项应计入通过,实际通过 " + check.getPassCount());
        // 仅表1/表2 系统表单缺失,不会因 A/G/H 缺材料而拦截
        assertFalse(check.getMissing().stream().anyMatch(n -> n.contains("行政监管")), "G 已同步不应缺失");
    }
}
