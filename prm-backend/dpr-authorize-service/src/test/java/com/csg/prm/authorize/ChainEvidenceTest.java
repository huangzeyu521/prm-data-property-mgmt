package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.common.evidence.ChainEvidence;
import com.csg.prm.common.evidence.ChainEvidenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 区块链存证集成测试(⑧):授权生效登记关键节点自动上链 + SM3 防篡改验真。
 */
@SpringBootTest
@ActiveProfiles("test")
class ChainEvidenceTest {

    @Autowired
    private AuthCertService certService;
    @Autowired
    private ChainEvidenceService evidenceService;

    @Test
    void issuing_cert_should_anchor_evidence_on_chain() {
        AuthApply apply = new AuthApply();
        apply.setApplyId("AP-EVID-1");
        apply.setAssetId("DA-EVID-1");
        apply.setAssetName("存证测试表");
        apply.setGranteeOrg("广州供电局");
        apply.setRightType("使用权");
        apply.setScope("全字段");
        String certId = certService.generateFromApply(apply);
        AuthCert cert = certService.getById(certId);

        List<ChainEvidence> list = evidenceService.listByBiz(certId);
        assertEquals(1, list.size(), "生效登记应自动产生一条上链存证");
        ChainEvidence ev = list.get(0);
        assertEquals("授权生效", ev.getBizType());
        assertEquals(ChainEvidence.STATUS_ANCHORED, ev.getAnchorStatus());
        assertEquals(64, ev.getSm3Hash().length(), "SM3 摘要应为 64 位十六进制");
        assertTrue(ev.getChainTxHash().startsWith("0x"), "应有上链交易哈希");
        assertNotNull(ev.getBlockHeight());
        assertTrue(ev.getBlockHeight() > 0);

        // 防篡改验真:原始 payload 一致 -> true;被篡改 -> false
        String payload = String.join("|", cert.getCertNo(), cert.getApplyId(), cert.getAssetId(),
                cert.getGranteeOrg(), cert.getRightType());
        assertTrue(evidenceService.verify(ev.getEvidenceId(), payload), "原始数据应通过验真");
        assertFalse(evidenceService.verify(ev.getEvidenceId(), payload + "TAMPERED"), "篡改数据应验真失败");
    }
}
