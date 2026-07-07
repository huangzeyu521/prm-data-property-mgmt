package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批量授权清单草稿生命周期(清单级):草案删除(级联明细) + 申报稿撤回(退回草案 + 明细回草稿,可再提交)。
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchDraftLifecycleTest {

    @Autowired private BatchAuthListService service;
    @Autowired private AuthApplyService applyService;

    private String addItem(String batchListId, String assetId, String name) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        a.setBatchListId(batchListId);
        a.setAssetId(assetId);
        a.setAssetName(name);
        a.setEquityCardId("EC-PRA-BATCH-" + assetId);
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScope("全字段");
        return applyService.saveDraft(a);
    }

    private String newDraftList() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        l.setRemark("草稿生命周期测试");
        return service.create(l);
    }

    @Test
    void delete_draft_list_cascades_draft_items() {
        String id = newDraftList();
        addItem(id, "DA-BDL-1", "资产一");
        addItem(id, "DA-BDL-2", "资产二");
        assertEquals(2, applyService.byBatch(id).size());

        service.delete(id);
        // 清单逻辑删除 -> getById 抛不存在;明细级联删除 -> byBatch 空
        assertThrows(BusinessException.class, () -> service.getById(id), "草案删除后清单应不存在");
        assertTrue(applyService.byBatch(id).isEmpty(), "草案删除应级联删除其下草稿明细");
    }

    @Test
    void delete_rejects_submitted_list() {
        String id = newDraftList();
        addItem(id, "DA-BDL-3", "已提交资产");
        service.submit(id); // 草案 -> 申报稿
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(id));
        assertTrue(ex.getMessage().contains("草案"), "申报稿不可直接删除:" + ex.getMessage());
    }

    @Test
    void withdraw_submitted_returns_list_and_items_to_draft_and_is_resubmittable() {
        String id = newDraftList();
        addItem(id, "DA-BDW-1", "资产一");
        addItem(id, "DA-BDW-2", "资产二");
        service.submit(id);
        assertEquals(BatchAuthList.STATUS_SUBMITTED, service.getById(id).getListStatus());
        assertTrue(applyService.byBatch(id).stream()
                .allMatch(a -> AuthApply.STATUS_COMPLIANCE.equals(a.getStatus())), "提交后明细在合规审核中");

        // 撤回:申报稿 -> 草案,明细退回草稿
        service.withdraw(id);
        assertEquals(BatchAuthList.STATUS_DRAFT, service.getById(id).getListStatus(), "撤回后清单回草案");
        assertTrue(applyService.byBatch(id).stream()
                .allMatch(a -> AuthApply.STATUS_DRAFT.equals(a.getStatus())), "撤回后明细退回草稿");

        // 可再次提交(在途→草案→再申报,闭环)
        service.submit(id);
        assertEquals(BatchAuthList.STATUS_SUBMITTED, service.getById(id).getListStatus(), "退回草案后可再提交");
    }

    @Test
    void withdraw_rejects_draft_list() {
        String id = newDraftList();
        addItem(id, "DA-BDW-3", "资产");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.withdraw(id));
        assertTrue(ex.getMessage().contains("申报稿"), "草案不可撤回(应直接编辑/删除):" + ex.getMessage());
    }
}
