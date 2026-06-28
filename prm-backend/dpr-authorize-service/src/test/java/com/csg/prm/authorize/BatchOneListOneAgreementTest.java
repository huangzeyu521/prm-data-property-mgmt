package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AgreementElementsVO;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.BatchAuthListService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 「批量授权:一清单一协议」契约锁(防回归)。
 *
 * 工作指引:一份《数据批量授权清单》经领导小组批准后,授权方/被授权方签 **一份**《运营授权协议》,
 * 清单各项=协议附件《数据授权清单》——不是每个授权项各签一份协议。
 * 本测试锁:generateForBatch 一清单一协议(幂等防重)、协议挂 batchListId、elements 聚合清单各项。
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchOneListOneAgreementTest {

    @Autowired private AuthApplyService applyService;
    @Autowired private BatchAuthListService batchListService;
    @Autowired private AuthAgreementService agreementService;

    private void item(String batchListId, String assetName) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        a.setBatchListId(batchListId);
        a.setAssetId("SYS:营销管理系统");
        a.setAssetName(assetName);
        a.setEquityCardId("EC-OK-1");
        a.setGranteeOrg("南网综合能源股份有限公司");
        a.setRightType("数据加工使用权");
        a.setScenario("综合能源服务");
        a.setScope("全字段");
        applyService.saveDraft(a);
    }

    private String approvedListWithTwoItems() {
        BatchAuthList list = new BatchAuthList();
        list.setListYear("2026");
        list.setRemark("一清单一协议测试");
        String batchListId = batchListService.create(list);
        item(batchListId, "用户用电信息表");
        item(batchListId, "电费账单表");
        batchListService.submit(batchListId);   // 草案→申报稿(逐项合规+入链)
        batchListService.approve(batchListId);  // 申报稿→批准(领导小组)
        return batchListId;
    }

    @Test
    @DisplayName("一份批量清单 → 一份运营授权协议(挂 batchListId);重复生成幂等返回同一份")
    void oneList_oneAgreement_idempotent() {
        String batchListId = approvedListWithTwoItems();

        String agId1 = agreementService.generateForBatch(batchListId);
        assertNotNull(agId1, "应生成批量协议");
        assertEquals(batchListId, agreementService.getById(agId1).getBatchListId(), "协议应挂在批量清单上");

        String agId2 = agreementService.generateForBatch(batchListId);
        assertEquals(agId1, agId2, "一清单一协议:重复生成应幂等返回同一份,不得重复签约");
    }

    @Test
    @DisplayName("批量协议要素核对聚合清单各项(itemCount=2,附件=数据授权清单)")
    void batchAgreement_elements_aggregateListItems() {
        String batchListId = approvedListWithTwoItems();
        String agId = agreementService.generateForBatch(batchListId);

        AgreementElementsVO vo = agreementService.elements(agId);
        assertEquals("批量", vo.authMode(), "应标批量授权方式");
        assertEquals(batchListId, vo.batchListId(), "应回链批量清单");
        assertEquals(2, vo.itemCount(), "应聚合清单 2 张数据表");
        assertNotNull(vo.items(), "应返回协议附件《数据授权清单》明细");
        assertEquals(2, vo.items().size(), "明细应含 2 项");
    }

    @Test
    @DisplayName("未批准的批量清单不可签订运营授权协议")
    void unapprovedList_cannotGenerateAgreement() {
        BatchAuthList list = new BatchAuthList();
        list.setListYear("2026");
        String batchListId = batchListService.create(list); // 仅草案
        assertThrows(RuntimeException.class, () -> agreementService.generateForBatch(batchListId),
                "仅批准的清单可签协议");
    }
}
