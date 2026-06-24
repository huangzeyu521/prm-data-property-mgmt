package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 表6《数据批量授权清单》生命周期:草案 -> 申报稿 -> 批准(领导小组办公室)。
 * 提交申报稿时逐项进批量审批链并做逐项合规门禁(第三方凭证红线)。
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchAuthListTest {

    @Autowired
    private BatchAuthListService service;
    @Autowired
    private AuthApplyService applyService;

    private String addItem(String batchListId, String assetId, String name) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        a.setBatchListId(batchListId);
        a.setAssetId(assetId);
        a.setAssetName(name);
        a.setEquityCardId("EC-PRA-BATCH-" + assetId);
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        a.setScope("全字段");
        return applyService.saveDraft(a);
    }

    @Test
    void list_lifecycle_draft_submit_approve() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        l.setRemark("2026年度批量授权清单");
        String id = service.create(l);
        assertNotNull(id);
        assertEquals(BatchAuthList.STATUS_DRAFT, service.getById(id).getListStatus());
        assertNotNull(service.getById(id).getListNo(), "应自动生成清单编号");

        // 提交申报稿前需有明细(对齐:明细随提交入批量审批链)
        addItem(id, "DA-BAT-1", "批量资产一");
        addItem(id, "DA-BAT-2", "批量资产二");

        service.submit(id);
        assertEquals(BatchAuthList.STATUS_SUBMITTED, service.getById(id).getListStatus());
        // 明细应已从草稿进入合规审核中(批量链首节点)
        assertTrue(applyService.byBatch(id).stream()
                .allMatch(a -> AuthApply.STATUS_COMPLIANCE.equals(a.getStatus())), "明细应随提交进入合规审核中");

        service.approve(id);
        assertEquals(BatchAuthList.STATUS_APPROVED, service.getById(id).getListStatus());

        assertTrue(service.page(1, 10, "2026", "批准").getTotal() >= 1);
    }

    @Test
    void empty_list_cannot_submit() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        String id = service.create(l);
        BizException ex = assertThrows(BizException.class, () -> service.submit(id));
        assertTrue(ex.getMessage().contains("清单为空"), "空清单不可提交");
    }

    @Test
    void submit_blocks_when_item_missing_third_party_license() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        String id = service.create(l);
        // 一条明细涉第三方来源但未提许可凭证 -> 红线拦截,整单不可提交
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        a.setBatchListId(id);
        a.setAssetId("DA-BAT-TP");
        a.setAssetName("涉三方资产");
        a.setEquityCardId("EC-PRA-BATCH-TP");
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        a.setScope("全字段");
        a.setThirdPartySource("公开采集");
        applyService.saveDraft(a);
        BizException ex = assertThrows(BizException.class, () -> service.submit(id));
        assertTrue(ex.getMessage().contains("第三方"), "涉第三方未提凭证应拦截:" + ex.getMessage());
    }

    @Test
    void deleteApply_removesDraftItem_fromBatchList() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        String id = service.create(l);
        String a1 = addItem(id, "DA-DEL-1", "待删资产");
        String a2 = addItem(id, "DA-DEL-2", "保留资产");
        assertEquals(2, applyService.byBatch(id).size());

        applyService.deleteApply(a1);
        java.util.List<AuthApply> remain = applyService.byBatch(id);
        assertEquals(1, remain.size(), "删后只剩一条");
        assertEquals(a2, remain.get(0).getApplyId());
    }

    @Test
    void deleteApply_rejectsNonDraft() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        String id = service.create(l);
        String a1 = addItem(id, "DA-DEL-3", "已提交资产");
        service.submit(id); // 明细进合规审核中(非草稿)
        BizException ex = assertThrows(BizException.class, () -> applyService.deleteApply(a1));
        assertTrue(ex.getMessage().contains("草稿"), "非草稿不可删:" + ex.getMessage());
    }

    @Test
    void cannot_approve_draft_directly() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        String id = service.create(l);
        // 草案不可直接批准(须先提交为申报稿)
        assertThrows(BizException.class, () -> service.approve(id));
    }
}
