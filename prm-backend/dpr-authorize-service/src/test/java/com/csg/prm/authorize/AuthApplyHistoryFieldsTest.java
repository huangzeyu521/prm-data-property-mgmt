package com.csg.prm.authorize;

import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.service.AuthApplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权申请历史查询页字段契约锁(防回归)。
 *
 * 「授权申请历史查询」页主表列(所属系统/数据表/权益类型)与进度详情抽屉(申请要素:模式名称/业务域/
 * 使用场景/生效卡片/利益分配/安全保障 等)全部直接渲染 pageAuthApply 返回的 AuthApply 实体字段。
 * 任一表5/表6/§3.4.4 字段在分页查询中丢失都会让历史页静默丢列/丢要素。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthApplyHistoryFieldsTest {

    @Autowired
    private AuthApplyService applyService;

    @Test
    @DisplayName("pageAuthApply 明细原样带回 表5/表6/§3.4.4 字段(系统派生/权益/模式/场景/业务域/利益分配/安全保障)")
    void page_carriesTable56AndAgreementFields() {
        String grantee = "历史页测试被授权方-" + System.nanoTime();
        AuthApply a = new AuthApply();
        a.setAuthMode(AuthApply.MODE_SPECIAL);
        a.setAssetId("SYS:营销管理系统");          // 历史页据此派生「所属系统」
        a.setAssetName("用户用电信息表");          // 数据表(库表名)
        a.setEquityCardId("EC-OK-1");
        a.setGranteeOrg(grantee);
        a.setRightType("使用权");          // 主表「权益类型」列
        a.setScenario("综合能源服务");
        a.setScope("全字段");
        a.setSchemaName("BILLING");                // 表5/表6 模式名称
        a.setBusinessDomain("营销域");
        a.setBenefitAllocation("按调用次数计费,收益 7:3 分成"); // §3.4.4
        a.setSecurityReq("加密传输 + 最小授权访问控制 + 操作留痕审计"); // §3.4.4
        applyService.saveDraft(a);

        AuthApplyQuery q = new AuthApplyQuery();
        q.setGranteeOrg(grantee);
        q.setPageSize(50);
        List<AuthApply> records = applyService.page(q).getRecords();

        AuthApply got = records.stream().filter(r -> grantee.equals(r.getGranteeOrg())).findFirst().orElse(null);
        assertNotNull(got, "分页应查回刚建的一事一议申请");
        assertTrue(got.getAssetId().startsWith("SYS:"), "assetId 须为 SYS:系统名(历史页派生所属系统)");
        assertEquals("营销管理系统", got.getAssetId().substring(4), "所属系统应可派生");
        assertEquals("使用权", got.getRightType(), "主表权益类型列字段");
        assertEquals("BILLING", got.getSchemaName(), "模式名称应往返保留");
        assertEquals("综合能源服务", got.getScenario(), "使用场景应往返保留");
        assertEquals("营销域", got.getBusinessDomain(), "业务域应往返保留");
        assertEquals("按调用次数计费,收益 7:3 分成", got.getBenefitAllocation(), "§3.4.4 利益分配应往返保留");
        assertEquals("加密传输 + 最小授权访问控制 + 操作留痕审计", got.getSecurityReq(), "§3.4.4 安全保障应往返保留");
    }
}
