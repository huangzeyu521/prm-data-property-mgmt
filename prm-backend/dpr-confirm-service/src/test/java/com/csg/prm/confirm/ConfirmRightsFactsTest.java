package com.csg.prm.confirm;

import com.csg.prm.confirm.dto.RightsFactsVO;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.service.ConfirmApplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权益事实(确权信息带出)集成测试:验证授权侧"第三方来源/隐私商密"从最新已完成确权确定性推导,
 * 堵人工低报击穿应交材料与合规校验(对齐工作指引 表5「确权信息带出」)。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmRightsFactsTest {

    @Autowired
    private ConfirmApplyService service;
    @Autowired
    private ConfirmApplyMapper mapper;

    @Test
    void rightsFacts_derivesThirdPartyAndPrivacy_fromDoneConfirm() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-RF-001");
        a.setAssetName("权益事实测试");
        a.setStatus(ConfirmApply.STATUS_DONE);
        a.setInvolvesThirdParty(true);
        a.setSourceSubject("XX科技有限公司");
        a.setRelationIdentification("H个人家庭隐私,I第三方商业机密");
        mapper.insert(a);

        RightsFactsVO vo = service.rightsFacts("DA-RF-001");
        assertTrue(vo.isConfirmed());
        assertEquals("XX科技有限公司", vo.getThirdPartySource(), "涉第三方应带出来源主体");
        assertEquals("个人隐私、商业秘密", vo.getSensitiveType(), "H+I 应推导个人隐私、商业秘密");
    }

    @Test
    void rightsFacts_selfProduced_noSensitive_notInvolved() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("DA-RF-SELF");
        a.setAssetName("自产无敏感");
        a.setStatus(ConfirmApply.STATUS_DONE);
        a.setInvolvesThirdParty(false);
        a.setSourceIdentification("A自行生产");
        a.setRelationIdentification("");
        mapper.insert(a);

        RightsFactsVO vo = service.rightsFacts("DA-RF-SELF");
        assertTrue(vo.isConfirmed());
        assertEquals("", vo.getThirdPartySource(), "自行生产不涉第三方");
        assertEquals("无", vo.getSensitiveType());
    }

    @Test
    void rightsFacts_noConfirm_returnsEmptyDefaults() {
        RightsFactsVO vo = service.rightsFacts("DA-RF-NONE");
        assertFalse(vo.isConfirmed());
        assertEquals("", vo.getThirdPartySource());
        assertEquals("无", vo.getSensitiveType());
    }
}
