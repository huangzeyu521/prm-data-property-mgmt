package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AgreementNegotiationVO;
import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 协议要素落定(附录D 协商项)生命周期测试:
 * 草案预填 → 填空校验(止日5年上限/覆盖明细时效/违约金/送达) → 正式稿锁定快照 → 签章门禁 →
 * 承诺函三条件收尾 → 续期/终止(动态跟踪)。
 */
@SpringBootTest
@ActiveProfiles("test")
class AgreementNegotiationTest {

    @Autowired private AuthAgreementService agreementService;
    @Autowired private AuthApplyMapper applyMapper;

    private AgreementNegotiationVO vo(String validUntil, String penalty, String delivery) {
        return new AgreementNegotiationVO(null, null, null, validUntil, null, null, null, null,
                null, penalty, null, delivery, null, null, null, null, null);
    }

    @Test
    void generate_prefills_negotiation_defaults() {
        String id = agreementService.generate("AP-NEG-0", null, "广州供电局");
        AgreementNegotiationVO n = agreementService.negotiation(id);
        assertEquals(AuthAgreement.DOC_DRAFT, n.docStatus(), "生成即草案");
        assertNotNull(n.validUntil(), "止日预填(今日+3年)");
        assertEquals("广东省行政区域内", n.geoScope(), "地理范围缺省");
        assertNotNull(n.securityEncrypt(), "表2三行预填");
        assertNotNull(n.securityAccess());
        assertNotNull(n.securityAudit());
        assertEquals(4, n.copiesCount(), "正本一式四份缺省");
        assertFalse(Boolean.TRUE.equals(n.confidentialityUploaded()), "承诺函未收口");
    }

    @Test
    void finalize_requires_all_negotiated_elements() {
        String id = agreementService.generate("AP-NEG-1", null, "广州供电局");
        // 预填后仍缺 违约金/送达信息 → 锁定被拦且报出缺项
        BusinessException ex = assertThrows(BusinessException.class, () -> agreementService.finalizeDoc(id));
        assertTrue(ex.getMessage().contains("违约金"), "应点名缺违约金:" + ex.getMessage());
        assertTrue(ex.getMessage().contains("送达"), "应点名缺送达信息:" + ex.getMessage());
        // 补齐 → 锁定成功,正文快照包含落定值
        agreementService.saveNegotiation(id, vo(null, "15", "手机:13800000000"));
        agreementService.finalizeDoc(id);
        AuthAgreement a = agreementService.getById(id);
        assertEquals(AuthAgreement.DOC_FINAL, a.getDocStatus());
        String html = new String(agreementService.appendixDDoc(id), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("人民币 15 万元"), "违约金落进正文");
        assertTrue(html.contains("手机:13800000000"), "送达信息落进正文");
        assertFalse(html.contains("_______________ 万元"), "正式稿不得残留违约金占位");
    }

    @Test
    void finalize_rejects_over_five_years_and_uncovered_item_term() {
        String id = agreementService.generate("AP-NEG-2", null, "广州供电局");
        // 止日超过5年上限(附录D 表1)
        agreementService.saveNegotiation(id, vo(LocalDate.now().plusYears(6).toString(), "5", "邮箱:a@csg.cn"));
        BusinessException over = assertThrows(BusinessException.class, () -> agreementService.finalizeDoc(id));
        assertTrue(over.getMessage().contains("5年"), over.getMessage());

        // 明细时效超出协议止日(「具体项目数据使用期限不得超过本协议有效期」)
        AuthApply apply = new AuthApply();
        apply.setApplyId("AP-NEG-3");
        apply.setAssetId("AST-NEG-3");
        apply.setAssetName("台区负荷数据");
        apply.setGranteeOrg("南网综能");
        apply.setRightType("使用权");
        apply.setValidDate(LocalDateTime.now().plusYears(4));
        applyMapper.insert(apply);
        String id2 = agreementService.generate("AP-NEG-3", null, "南网综能");
        // 预填止日=max(3年,明细4年)=4年,先验证预填即覆盖
        assertTrue(LocalDate.parse(agreementService.negotiation(id2).validUntil())
                .isAfter(LocalDate.now().plusYears(3).minusDays(1)), "预填止日应覆盖明细最长时效");
        // 人为压短到2年 → 锁定被拦
        agreementService.saveNegotiation(id2, vo(LocalDate.now().plusYears(2).toString(), "5", "邮箱:a@csg.cn"));
        BusinessException uncovered = assertThrows(BusinessException.class, () -> agreementService.finalizeDoc(id2));
        assertTrue(uncovered.getMessage().contains("不得超过协议有效期"), uncovered.getMessage());
    }

    @Test
    void final_doc_locks_snapshot_and_rejects_edit() {
        String id = agreementService.generate("AP-NEG-4", null, "广州供电局");
        agreementService.saveNegotiation(id, vo(null, "8", "邮箱:b@csg.cn"));
        agreementService.finalizeDoc(id);
        // 正式稿要素不可再改(须先退回草案)
        assertThrows(BusinessException.class,
                () -> agreementService.saveNegotiation(id, vo(null, "99", "邮箱:b@csg.cn")));
        // 未签章可退回草案,快照作废可重新落定
        agreementService.revertToDraft(id);
        assertEquals(AuthAgreement.DOC_DRAFT, agreementService.getById(id).getDocStatus());
        agreementService.saveNegotiation(id, vo(null, "20", "邮箱:b@csg.cn"));
        agreementService.finalizeDoc(id);
        String html = new String(agreementService.appendixDDoc(id), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("人民币 20 万元"), "重新落定后快照应为新值");
        // 签章后不可退回草案
        agreementService.uploadSeal(id, "授权方", "grantor.pdf", new byte[128]);
        assertThrows(BusinessException.class, () -> agreementService.revertToDraft(id));
    }

    @Test
    void renew_and_terminate_lifecycle() {
        // 走完整收尾:正式稿+双签+承诺函 → 已归档
        String id = agreementService.generate("AP-NEG-5", null, "深圳供电局");
        agreementService.saveNegotiation(id, vo(null, "10", "邮箱:c@csg.cn"));
        agreementService.finalizeDoc(id);
        // 未归档不可续期
        assertThrows(BusinessException.class,
                () -> agreementService.renew(id, LocalDate.now().plusYears(4).toString()));
        agreementService.uploadSeal(id, "授权方", "g1.pdf", new byte[128]);
        agreementService.uploadSeal(id, "被授权方", "g2.pdf", new byte[128]);
        agreementService.uploadConfidentiality(id, "承诺函.pdf", new byte[128]);
        AuthAgreement archived = agreementService.getById(id);
        assertEquals(AuthAgreement.ARCHIVE_YES, archived.getArchiveStatus());
        // 续期:新止日须晚于原止日且≤今日+5年
        assertThrows(BusinessException.class,
                () -> agreementService.renew(id, LocalDate.now().plusYears(1).toString()), "早于原止日应拦");
        assertThrows(BusinessException.class,
                () -> agreementService.renew(id, LocalDate.now().plusYears(6).toString()), "超5年应拦");
        String renewed = LocalDate.now().plusYears(4).toString();
        agreementService.renew(id, renewed);
        assertEquals(renewed, agreementService.getById(id).getValidUntil().toLocalDate().toString());
        // 终止:记录原因,后续操作全部拒绝
        agreementService.terminate(id, "乙方违反数据安全管理要求(第七章)");
        AuthAgreement t = agreementService.getById(id);
        assertTrue(Boolean.TRUE.equals(t.getTerminated()));
        assertThrows(BusinessException.class,
                () -> agreementService.renew(id, LocalDate.now().plusYears(5).toString()), "已终止不可续期");
        assertThrows(BusinessException.class,
                () -> agreementService.uploadSeal(id, "授权方", "x.pdf", new byte[128]), "已终止不可签章");
    }
}
