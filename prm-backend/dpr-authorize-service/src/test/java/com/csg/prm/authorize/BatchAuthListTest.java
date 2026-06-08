package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.BatchAuthList;
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
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchAuthListTest {

    @Autowired
    private BatchAuthListService service;

    @Test
    void list_lifecycle_draft_submit_approve() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        l.setRemark("2026年度批量授权清单");
        String id = service.create(l);
        assertNotNull(id);
        assertEquals(BatchAuthList.STATUS_DRAFT, service.getById(id).getListStatus());
        assertNotNull(service.getById(id).getListNo(), "应自动生成清单编号");

        service.submit(id);
        assertEquals(BatchAuthList.STATUS_SUBMITTED, service.getById(id).getListStatus());

        service.approve(id);
        assertEquals(BatchAuthList.STATUS_APPROVED, service.getById(id).getListStatus());

        assertTrue(service.page(1, 10, "2026", "批准").getTotal() >= 1);
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
