package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批量授权清单明细(表6)字段契约锁(防回归)。
 *
 * 「批量授权清单」页明细抽屉直接渲染 byBatch(batchListId) 返回的 AuthApply 列表的表6字段:
 * 所属系统(assetId=SYS:系统名)/模式名称 schemaName/生效卡片 equityCardId/业务域 businessDomain/
 * 涉第三方 thirdPartySource/涉隐私商密 sensitiveType。任一字段在持久化往返中丢失都会让清单页静默丢列。
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchListItemTable6Test {

    @Autowired
    private AuthApplyService applyService;

    private AuthApply batchItem(String batchListId, String assetName, String tableSysId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        a.setBatchListId(batchListId);
        a.setAssetId("SYS:营销管理系统");          // 库表级:assetId=SYS:系统名,清单页据此派生「所属系统」
        a.setAssetName(assetName);                  // 数据表=库表名
        a.setGranteeOrg("南网综合能源股份有限公司"); // 被授权方(saveDraft 必填)
        a.setEquityCardId("EC-PRA-VALID01");        // 先确后授生效卡片
        a.setRightType("使用权");
        a.setScenario("综合能源服务");
        a.setScope("全字段");
        a.setSchemaName("BILLING");                 // 表6 模式名称
        a.setBusinessDomain("营销域");              // 表6 所属业务域
        a.setThirdPartySource("");                  // 不涉第三方
        a.setSensitiveType("个人隐私");             // 涉隐私
        return a;
    }

    @Test
    @DisplayName("byBatch 明细项原样带回表6字段(系统/模式/生效卡片/业务域/第三方/隐私),供清单页逐列渲染")
    void byBatch_carriesTable6FieldsForListPage() {
        String batchId = "BL-T6-" + System.nanoTime();
        applyService.saveDraft(batchItem(batchId, "用户用电信息表", "MKT_BILL_CONS"));
        applyService.saveDraft(batchItem(batchId, "电费账单表", "MKT_BILL_INVOICE"));

        List<AuthApply> items = applyService.byBatch(batchId);
        assertEquals(2, items.size(), "同一 batchListId 的两条明细应都能查回");

        for (AuthApply it : items) {
            assertNotNull(it.getAssetId(), "assetId 不应丢失");
            assertTrue(it.getAssetId().startsWith("SYS:"), "assetId 须为 SYS:系统名 形态(清单页派生所属系统)");
            assertEquals("营销管理系统", it.getAssetId().substring(4), "所属系统应可从 assetId 派生");
            assertEquals("BILLING", it.getSchemaName(), "表6 模式名称应往返保留");
            assertEquals("EC-PRA-VALID01", it.getEquityCardId(), "先确后授生效卡片应往返保留");
            assertEquals("营销域", it.getBusinessDomain(), "表6 业务域应往返保留");
            assertEquals("个人隐私", it.getSensitiveType(), "涉隐私应往返保留");
        }
    }
}
