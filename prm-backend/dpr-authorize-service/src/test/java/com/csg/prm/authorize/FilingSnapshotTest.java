package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthFiling;
import com.csg.prm.authorize.mapper.AuthFilingMapper;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthFilingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 对外经营权授权备案(附录G)建档快照契约锁(防回归)。
 *
 * 附录G 备案表须有 协议编号 / 授权期限。备案记录应自包含:关联协议建档时,
 * 由协议 + 申请单快照 agreementNo(协议编号)/validDate(授权期限)/被授权方/产权类型,
 * 列表零 join 即显附录G 列。本测试锁该快照正确。
 */
@SpringBootTest
@ActiveProfiles("test")
class FilingSnapshotTest {

    @Autowired private AuthApplyService applyService;
    @Autowired private AuthAgreementService agreementService;
    @Autowired private AuthFilingService filingService;
    @Autowired private AuthFilingMapper filingMapper;

    @Test
    @DisplayName("备案关联经营权协议建档 → 快照 协议编号/授权期限/被授权方/产权类型(附录G 自包含)")
    void filing_snapshots_agreementNo_and_validDate() {
        LocalDateTime validDate = LocalDateTime.now().plusYears(2).withNano(0);
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId("SYS:营销管理系统");
        a.setAssetName("用户用电信息表");
        a.setEquityCardId("EC-OK-1");
        a.setGranteeOrg("南网综合能源股份有限公司");
        a.setRightType("数据产品经营权");   // 经营权对外授权才需备案(附录G)
        a.setScenario("综合能源服务");
        a.setScope("全字段");
        a.setValidDate(validDate);
        String applyId = applyService.saveDraft(a);

        String agreementId = agreementService.generate(applyId, null, "南网综合能源股份有限公司");
        String agreementNo = agreementService.getById(agreementId).getAgreementNo();

        // 建档:仅给 agreementId,其余由快照带出(granteeOrg/rightType 留空验证回填)
        AuthFiling filing = new AuthFiling();
        filing.setFilingOrg("广东电网有限责任公司");
        filing.setAgreementId(agreementId);
        String filingId = filingService.create(filing);

        AuthFiling saved = filingMapper.selectById(filingId);
        assertNotNull(saved, "应建档成功");
        assertEquals(agreementNo, saved.getAgreementNo(), "协议编号应由关联协议快照(附录G 列)");
        assertNotNull(saved.getValidDate(), "授权期限应由申请单快照(附录G 列)");
        assertEquals(validDate, saved.getValidDate(), "授权期限应等于申请单授权时效");
        assertEquals("南网综合能源股份有限公司", saved.getGranteeOrg(), "被授权方应由协议快照回填");
        assertEquals("数据产品经营权", saved.getRightType(), "产权类型应由申请单快照回填");
    }
}
