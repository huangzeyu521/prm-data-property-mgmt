package com.csg.prm.confirm.service;

import com.csg.prm.confirm.dto.MaterialCheckReport;
import com.csg.prm.confirm.dto.MaterialSyncReport;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmMaterial;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private String draftAst001() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("AST-001");
        a.setAssetName("客户用电信息表");
        a.setRightType("数据加工使用权");
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

        // AST-001 平台覆盖 4 项:证明材料 + A 来源说明 + G 行政监管 + H 个人隐私
        assertEquals(4, rep.getSyncedCount(), "应从平台同步 4 项已上传材料,实际 " + rep.getSyncedCount());
        assertTrue(rep.getSynced().stream().anyMatch(s -> s.getMaterialName().contains("证明材料")), "应同步 证明材料");
        assertTrue(rep.getSynced().stream().anyMatch(s -> "A".equals(s.getCode())), "应同步 A 来源说明");
        assertTrue(rep.getSynced().stream().anyMatch(s -> "G".equals(s.getCode())), "应同步 G 行政监管");
        assertTrue(rep.getSynced().stream().anyMatch(s -> "H".equals(s.getCode())), "应同步 H 个人隐私");

        // 登记的材料带平台来源 + 平台附件名 + 校验通过(免上传)
        List<ConfirmMaterial> mats = materialService.listByApply(id);
        assertTrue(mats.stream().allMatch(m -> ConfirmMaterial.SOURCE_PLATFORM.equals(m.getSource())),
                "本次登记材料均应为 平台同步");
        assertTrue(mats.stream().allMatch(m -> m.getFileName() != null && !m.getFileName().isBlank()),
                "平台同步材料应带平台附件名");
        assertTrue(mats.stream().allMatch(m -> ConfirmMaterial.CHECK_PASS.equals(m.getCheckResult())),
                "平台同步材料校验应直接通过");

        // 表1/表2(系统自生成表单)平台无附件 → 仍待用户补全
        assertTrue(rep.getStillMissing().stream().anyMatch(n -> n.contains("表1")), "表1 应待补全");
        assertTrue(rep.getStillMissing().stream().anyMatch(n -> n.contains("表2")), "表2 应待补全");
        assertFalse(rep.getStillMissing().stream().anyMatch(n -> n.contains("个人/家庭隐私")), "H 已平台同步,不应待补全");
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
