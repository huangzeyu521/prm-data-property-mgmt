package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0 草稿归属隔离(授权域):
 * 一事一议草稿=私有(他人看不到、不可删/提交);批量明细草稿=清单级共享(他人可见、可走清单级删除)。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthDraftIsolationTest {

    @Autowired private AuthApplyService service;

    private UserContext apply(String uid) {
        UserContext c = new UserContext();
        c.setUserId(uid);
        c.setRoles(Set.of("apply"));
        return c;
    }

    private AuthApply base(String asset, String mode) {
        AuthApply a = new AuthApply();
        a.setAssetId(asset);
        a.setAssetName(asset);
        a.setGranteeOrg("南网科研院");
        a.setRightType("使用权");
        a.setEquityCardId("EC-ISO-" + asset);
        a.setScope("全字段");
        a.setAuthMode(mode);
        a.setCreatorId("userA");
        return a;
    }

    private boolean pageSees(String assetName, String applyId) {
        AuthApplyQuery q = new AuthApplyQuery();
        q.setAssetName(assetName);
        return service.page(q).getRecords().stream().anyMatch(r -> applyId.equals(r.getApplyId()));
    }

    @Test
    void special_draft_is_private_to_owner() {
        String asset = "授权隔离特-" + System.nanoTime();
        final String applyId;
        UserContextHolder.set(apply("userA"));
        try {
            applyId = service.saveDraft(base(asset, AuthApply.MODE_SPECIAL));
            assertTrue(pageSees(asset, applyId), "本人应能看到自己的一事一议草稿");
        } finally {
            UserContextHolder.clear();
        }

        UserContextHolder.set(apply("userB"));
        try {
            assertFalse(pageSees(asset, applyId), "他人不应看到我的一事一议草稿");
            assertThrows(BusinessException.class, () -> service.deleteApply(applyId), "他人不可删除我的一事一议草稿");
            assertThrows(BusinessException.class, () -> service.submit(applyId), "他人不可提交我的一事一议草稿");
        } finally {
            UserContextHolder.clear();
        }
    }

    @Test
    void batch_detail_draft_is_shared_not_isolated() {
        String asset = "授权隔离批-" + System.nanoTime();
        final String applyId;
        UserContextHolder.set(apply("userA"));
        try {
            AuthApply a = base(asset, AuthApply.MODE_BATCH);
            a.setBatchListId("BL-ISO-" + asset);
            applyId = service.saveDraft(a);
        } finally {
            UserContextHolder.clear();
        }

        // 批量明细=清单级共享:他人(同 apply 角色的部门成员)可见、可走清单级删除(不作个人归属校验)
        UserContextHolder.set(apply("userB"));
        try {
            assertTrue(pageSees(asset, applyId), "批量明细草稿为清单级共享,部门成员应可见");
            service.deleteApply(applyId); // 批量明细不作个人门禁,不抛
        } finally {
            UserContextHolder.clear();
        }
    }
}
