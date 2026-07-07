package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.BatchComplianceResult;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.BatchAuthListService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批量授权清单只读合规校验(试跑):支撑前端"校验通过才放行提交"闭环。
 * 与 submit 门禁同源(第三方凭证红线 + submitBlockReason),但不改状态。
 */
@SpringBootTest
@ActiveProfiles("test")
class BatchComplianceCheckTest {

    @Autowired private BatchAuthListService service;
    @Autowired private AuthApplyService applyService;
    @Autowired private AuthApplyMapper applyMapper;

    private String newList() {
        BatchAuthList l = new BatchAuthList();
        l.setListYear("2026");
        return service.create(l);
    }

    private String addItem(String id, String asset, String name, String thirdParty) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_BATCH);
        a.setBatchListId(id);
        a.setAssetId(asset);
        a.setAssetName(name);
        a.setEquityCardId("EC-PRA-BATCH-" + asset);
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScope("全字段");
        if (thirdParty != null) a.setThirdPartySource(thirdParty);
        return applyService.saveDraft(a);
    }

    /** 全部合规明细 → 整单可提交。 */
    @Test
    void clean_list_all_pass() {
        String id = newList();
        addItem(id, "CC-1", "资产一", null);
        addItem(id, "CC-2", "资产二", null);
        BatchComplianceResult r = service.complianceCheck(id);
        assertTrue(r.allPass(), "全合规应可提交");
        assertEquals(2, r.total());
        assertEquals(0, r.blockedCount());
    }

    /** 涉第三方未提凭证 → 被拦;补凭证后重新校验通过(闭环)。 */
    @Test
    void third_party_no_license_blocked_then_fixed() {
        String id = newList();
        addItem(id, "CC-3", "资产三", null);
        String tp = addItem(id, "CC-4", "涉三方资产", "公开采集");

        BatchComplianceResult r1 = service.complianceCheck(id);
        assertFalse(r1.allPass(), "存在涉三方未提凭证,整单不可提交");
        assertTrue(r1.blocked().stream().anyMatch(b -> b.applyId().equals(tp) && b.reason().contains("第三方")),
                "被拦明细应含该涉三方项");

        // 修正:补第三方许可凭证 → 重新校验通过
        AuthApply fix = new AuthApply();
        fix.setApplyId(tp);
        fix.setThirdPartyLicense("第三方授权许可凭证X");
        applyMapper.updateById(fix);

        BatchComplianceResult r2 = service.complianceCheck(id);
        assertTrue(r2.allPass(), "补凭证后应可提交");
        assertEquals(0, r2.blockedCount());
    }
}
