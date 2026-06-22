package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据授权全流程集成测试:草稿 -> 提交(先确后授校验) -> 审批通过 -> 自动生成授权证书;
 * 以及冻结熔断、必填(必须引用权益卡片)、驳回等路径。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthFlowTest {

    @Autowired
    private AuthApplyService applyService;
    @Autowired
    private AuthCertService certService;

    private AuthApply draft(String assetId, String name, String cardId) {
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId(assetId);
        a.setAssetName(name);
        a.setEquityCardId(cardId);
        a.setGranteeOrg("广州供电局");
        a.setRightType("数据加工使用权");
        a.setScenario("电力金融征信");
        a.setScope("全字段");
        return a;
    }

    @Test
    void special_flow_five_level_should_generate_cert() {
        String id = applyService.saveDraft(draft("DA-AUTH-001", "客户用电信息表", "EC-PRA-VALID01"));
        applyService.submit(id);
        // 专项五级:合规->业务->主管->经理->副总->已生效
        assertEquals(AuthApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 合规->业务
        assertEquals(AuthApply.STATUS_BUSINESS, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 业务->主管
        assertEquals(AuthApply.STATUS_MANAGER, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 主管->经理
        assertEquals(AuthApply.STATUS_DIRECTOR, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 经理->副总
        assertEquals(AuthApply.STATUS_VP, applyService.getById(id).getStatus());
        String certId = applyService.approve(id, null); // 副总->已生效(发证)
        assertNotNull(certId, "终审通过应自动生成授权证书");
        assertEquals(AuthApply.STATUS_EFFECTIVE, applyService.getById(id).getStatus());

        AuthCert cert = certService.getById(certId);
        assertNotNull(cert.getCertNo());
        assertEquals("DA-AUTH-001", cert.getAssetId());
        assertEquals(AuthCert.STATUS_EFFECTIVE, cert.getCertStatus());
    }

    @Test
    void batch_flow_should_generate_cert_with_aligned_nodes() {
        AuthApply a = draft("DA-AUTH-005", "批量授权表", "EC-PRA-VALID05");
        a.setAuthMode(AuthApply.MODE_BATCH);
        String id = applyService.saveDraft(a);
        applyService.submit(id);
        // 批量(节点对齐后):合规->主管->经理->副总->领导小组->已生效
        // 数字化部三节点(主管/经理/副总)与一事一议同名同粒度,末节点为领导小组决策。
        assertEquals(AuthApply.STATUS_COMPLIANCE, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 合规->主管
        assertEquals(AuthApply.STATUS_MANAGER, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 主管->经理
        assertEquals(AuthApply.STATUS_DIRECTOR, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 经理->副总
        assertEquals(AuthApply.STATUS_VP, applyService.getById(id).getStatus());
        assertNull(applyService.approve(id, null)); // 副总->领导小组
        assertEquals(AuthApply.STATUS_LEADERSHIP, applyService.getById(id).getStatus());
        String certId = applyService.approve(id, null); // 领导小组->已生效
        assertNotNull(certId, "领导小组批准应自动生成授权证书");
        assertEquals(AuthApply.STATUS_EFFECTIVE, applyService.getById(id).getStatus());
    }

    @Test
    void submit_should_block_when_scope_exceeds_confirm_boundary() {
        // 确权边界=约定字段(本地桩 NARROW),授权填"全字段"=超界,应被授权⊆确权边界规则拦截
        AuthApply a = draft("DA-AUTH-006", "约定字段资产", "EC-NARROW-1");
        a.setScope("全字段");
        String id = applyService.saveDraft(a);
        BizException ex = assertThrows(BizException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("确权边界"), "授权范围超确权边界应被拦截:" + ex.getMessage());
    }

    @Test
    void submit_should_block_when_card_frozen_or_unconfirmed() {
        // 引用冻结卡片(FROZEN 前缀)模拟"未确权/冻结"
        String id = applyService.saveDraft(draft("DA-AUTH-002", "冻结资产表", "FROZEN-CARD-1"));
        BizException ex = assertThrows(BizException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("先确后授"), "应被先确后授规则拦截");
    }

    @Test
    void draft_should_require_equity_card() {
        AuthApply bad = draft("DA-AUTH-003", "缺卡片表", null);
        assertThrows(BizException.class, () -> applyService.saveDraft(bad));
    }

    @Test
    void operation_right_must_be_in_open_catalog() {
        // 经营权 + 资产不在对外开放目录(NONOPEN 前缀)-> 提交被拦截
        AuthApply bad = draft("NONOPEN-OP-1", "未开放经营资产", "EC-OK-OP");
        bad.setRightType("数据产品经营权");
        String id = applyService.saveDraft(bad);
        BizException ex = assertThrows(BizException.class, () -> applyService.submit(id));
        assertTrue(ex.getMessage().contains("对外开放目录"), "经营权应受对外开放目录约束");

        // 经营权 + 资产在对外开放目录 -> 正常进入合规审核
        AuthApply ok = draft("DA-OP-OK", "已开放经营资产", "EC-OK-OP2");
        ok.setRightType("数据产品经营权");
        String id2 = applyService.saveDraft(ok);
        applyService.submit(id2);
        assertEquals(AuthApply.STATUS_COMPLIANCE, applyService.getById(id2).getStatus());
    }

    @Test
    void batch_chain_must_not_contain_legacy_orphan_state() {
        // 回归:批量审批链把"数字化部认定"拆分为主管/经理/副总三节点后,旧编排态
        // "数字化部认定中"成为孤儿态——不在任一审批链,却被种子(AA-005)与前端当作可审批,
        // 导致该单永远卡死(approve/reject 均抛"当前状态不可审批/驳回")。已把 AA-005 迁移为主管审核中。
        // 守护:批量链不得再含旧孤儿态,且必须含迁移目标"主管审核中"。
        java.util.List<String> batch =
                com.csg.prm.common.workflow.FlowDefinitions.states(
                        com.csg.prm.common.workflow.FlowDefinitions.DPR_AUTH_BATCH);
        assertTrue(batch.contains(AuthApply.STATUS_MANAGER), "批量链应含主管审核中");
        assertTrue(!batch.contains("数字化部认定中"), "批量链不得再含孤儿态'数字化部认定中'");
        // 迁移目标在链中即代表"可审批可驳回"(canAdvance 为真),不再卡死。
        assertTrue(batch.indexOf(AuthApply.STATUS_MANAGER) >= 0
                && batch.indexOf(AuthApply.STATUS_MANAGER) < batch.size() - 1,
                "主管审核中应为可推进的中间节点");
    }

    @Test
    void reject_should_set_status() {
        String id = applyService.saveDraft(draft("DA-AUTH-004", "待驳回表", "EC-PRA-VALID02"));
        applyService.submit(id);
        applyService.reject(id, "授权范围超出确权边界");
        AuthApply a = applyService.getById(id);
        assertEquals(AuthApply.STATUS_REJECTED, a.getStatus());
        assertEquals("授权范围超出确权边界", a.getRejectReason());
    }
}
