package com.csg.prm.confirm;

import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.dto.ConfirmApplyQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0 草稿归属隔离 + mutation 本人门禁(确权域):
 * 草稿=私有未提交资产 —— 他人在查询页看不到、不可删除、不可提交;本人可见、可删。
 * 对照 withdraw 既有的 assertApplicant,补齐 delete/submit/page 的同等门禁。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmDraftIsolationTest {

    @Autowired private ConfirmApplyService service;

    private UserContext apply(String uid) {
        UserContext c = new UserContext();
        c.setUserId(uid);
        c.setRoles(Set.of("apply"));
        return c;
    }

    private boolean pageSees(String assetName, String applyId) {
        ConfirmApplyQuery q = new ConfirmApplyQuery();
        q.setAssetName(assetName);
        return service.page(q).getRecords().stream().anyMatch(r -> applyId.equals(r.getApplyId()));
    }

    @Test
    void draft_private_to_owner_and_delete_submit_owner_gated() {
        String asset = "确权隔离-" + System.nanoTime();
        final String applyId;

        // userA 建草稿(显式落创建人 userA)
        UserContextHolder.set(apply("userA"));
        try {
            ConfirmApply a = new ConfirmApply();
            a.setAssetId(asset);
            a.setAssetName(asset);
            a.setRightType("持有权");
            a.setRightHolder("广东电网有限责任公司");
            a.setSourceIdentification("A自行生产数据");
            a.setCreatorId("userA");
            applyId = service.saveDraft(a);
            assertEquals("userA", service.getById(applyId).getCreatorId(), "创建人应为 userA");
            assertTrue(pageSees(asset, applyId), "本人应能在查询页看到自己的草稿");
        } finally {
            UserContextHolder.clear();
        }

        // userB(同为 apply 角色)看不到、不能删/提交 A 的草稿
        UserContextHolder.set(apply("userB"));
        try {
            assertFalse(pageSees(asset, applyId), "他人不应在查询页看到我的草稿");
            assertThrows(BusinessException.class, () -> service.delete(applyId), "他人不可删除我的草稿");
            assertThrows(BusinessException.class, () -> service.submit(applyId), "他人不可提交我的草稿");
        } finally {
            UserContextHolder.clear();
        }

        // 草稿未被 B 破坏;本人可删
        UserContextHolder.set(apply("userA"));
        try {
            assertEquals(ConfirmApply.STATUS_DRAFT, service.getById(applyId).getStatus(),
                    "B 的删除/提交应无效,草稿仍在");
            service.delete(applyId); // 本人删除不抛
        } finally {
            UserContextHolder.clear();
        }
    }
}
