package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.BatchComplianceResult;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 一事一议「单场景·多表」申请单(同 formNo 多张数据表,对齐 35号文 表5 多行)。
 * 覆盖:createForm 申请单号 / byForm 分组 / checkFormCompliance 逐表自检 / submitForm 整单提交。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthFormMultiTableTest {

    @Autowired
    private AuthApplyService service;

    /** 一行 = 一张数据表(共享 被授权方/场景/权益/协议要素);卡片用非 FROZEN/NARROW → 桩可用+全字段边界。 */
    private AuthApply row(String formNo, String assetId, String cardId, String assetName) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setFormNo(formNo);
        a.setAssetId(assetId);
        a.setAssetName(assetName);
        a.setEquityCardId(cardId);
        a.setGranteeOrg("广州供电局");
        a.setRightType("使用权");
        a.setScenario("电力金融征信");
        a.setPurposeNote("用于本单位内部经营分析,数据不出域");
        a.setScope("全字段");
        a.setBenefitAllocation("按调用次数计费");
        a.setSecurityReq("加密传输 + 最小授权访问控制");
        return a;
    }

    @Test
    @DisplayName("createForm 返回 SQF- 前缀申请单号")
    void createForm_returns_prefixed_no() {
        String f = service.createForm();
        assertTrue(f != null && f.startsWith("SQF-"), "申请单号应以 SQF- 开头: " + f);
    }

    @Test
    @DisplayName("多表归一申请单:byForm 分组 + 逐表自检全过 + 整单提交全部离草稿")
    void multi_table_form_groups_checks_and_submits() {
        String f = service.createForm();
        service.saveDraft(row(f, "DA-FORM-1", "EC-OK-1", "用户用电信息表"));
        service.saveDraft(row(f, "DA-FORM-2", "EC-OK-2", "负荷曲线表"));

        // 分组:同 formNo 两张表归为一份申请单
        assertEquals(2, service.byForm(f).size(), "申请单应含 2 张数据表");
        // 目的摘要(表5)round-trip 入库
        assertEquals("用于本单位内部经营分析,数据不出域", service.byForm(f).get(0).getPurposeNote(), "目的摘要应持久化");

        // 逐表自检:两表均过硬门禁(先确后授/范围⊆边界;不涉三方/隐私)
        BatchComplianceResult r = service.checkFormCompliance(f);
        assertTrue(r.allPass(), "两表均合规应整单通过: " + r);
        assertEquals(2, r.items().size());
        assertTrue(r.items().get(0).dims().size() >= 4, "应含至少 4 个硬门禁维度 + 协议要素提示");

        // 整单提交:两行均离开草稿,进入审批链首节点(合规审核中)
        service.submitForm(f);
        assertTrue(service.byForm(f).stream().noneMatch(a -> AuthApply.STATUS_DRAFT.equals(a.getStatus())),
                "submitForm 后不应再有草稿行");
    }

    @Test
    @DisplayName("逐表自检:某表涉第三方未提许可凭证 → 该表被拦,整单不可提交")
    void third_party_without_license_blocks_form() {
        String f = service.createForm();
        service.saveDraft(row(f, "DA-FORM-3", "EC-OK-3", "正常表"));
        AuthApply bad = row(f, "DA-FORM-4", "EC-OK-4", "涉三方表");
        bad.setThirdPartySource("外部采购"); // 涉第三方但未填许可凭证
        service.saveDraft(bad);

        BatchComplianceResult r = service.checkFormCompliance(f);
        assertFalse(r.allPass(), "存在被拦表,整单不应通过");
        assertEquals(1, r.blocked().size(), "应恰有 1 张表被拦");
        assertEquals("涉三方表", r.blocked().get(0).assetName());
    }
}
