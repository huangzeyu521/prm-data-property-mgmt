package com.csg.prm.confirm;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmSummary;
import com.csg.prm.confirm.entity.ConfirmTableItem;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.integration.DataCatalogService;
import com.csg.prm.confirm.integration.dto.ChangeBaseline;
import com.csg.prm.confirm.integration.dto.ChangeBaselineFull;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.ConfirmSummaryMapper;
import com.csg.prm.confirm.mapper.ConfirmTableItemMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.impl.ConfirmChangeBaselineServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 确权变更基线反查单测:验证"反查上一版真实确权结论(表3行 + 权益卡片 + 认定意见)"与无上一版时退回合成桩。
 */
class ConfirmChangeBaselineServiceTest {

    private final ConfirmApplyMapper applyMapper = mock(ConfirmApplyMapper.class);
    private final ConfirmTableItemMapper itemMapper = mock(ConfirmTableItemMapper.class);
    private final EquityCardMapper cardMapper = mock(EquityCardMapper.class);
    private final ConfirmSummaryMapper summaryMapper = mock(ConfirmSummaryMapper.class);
    private final DataCatalogService catalogService = mock(DataCatalogService.class);

    private final ConfirmChangeBaselineServiceImpl svc = new ConfirmChangeBaselineServiceImpl(
            applyMapper, itemMapper, cardMapper, summaryMapper, catalogService);

    @Test
    @DisplayName("反查到上一版已完成确权 → fromRealConfirm,带出表3行+权益卡片+认定意见,base 取上一版真实值")
    void baseline_fromRealConfirm_loadsTableItemsAndCards() {
        ConfirmApply prior = new ConfirmApply();
        prior.setApplyId("CA-001");
        prior.setAssetId("AST-001");
        prior.setAssetName("客户用电信息表");
        prior.setStatus(ConfirmApply.STATUS_DONE);
        prior.setRightHolder("广东电网");
        prior.setRightType("持有权");
        prior.setRegulated("非管制");
        when(applyMapper.selectOne(any())).thenReturn(prior);

        ConfirmTableItem t1 = new ConfirmTableItem();
        t1.setTableCode("MKT_BILL_CONS");
        t1.setTableName("用户用电信息表");
        t1.setGFlag("否");
        t1.setHFlag("是");
        when(itemMapper.selectList(any())).thenReturn(List.of(t1));

        EquityCard card = new EquityCard();
        card.setCardNo("EC-PRA-0001");
        card.setRightType("持有权");
        card.setScope("全字段");
        card.setCardStatus(EquityCard.STATUS_NORMAL);
        when(cardMapper.selectList(any())).thenReturn(List.of(card));

        ConfirmSummary sm = new ConfirmSummary();
        sm.setSummaryType("表3 数据确权信息汇总表");
        sm.setContent("持有权归广东电网");
        when(summaryMapper.selectList(any())).thenReturn(List.of(sm));

        ChangeBaselineFull full = svc.baselineOf("AST-001", null);

        assertTrue(full.fromRealConfirm(), "反查到上一版应为真实确权");
        assertEquals("CA-001", full.priorApplyId());
        assertEquals(1, full.tableItems().size(), "应带出上一版表3逐表行");
        assertEquals("是", full.tableItems().get(0).getHFlag(), "表3行原值应保留(非布尔误判)");
        assertEquals(1, full.cards().size(), "应带出上一版权益卡片");
        assertEquals("全字段", full.cards().get(0).scope(), "卡片确权边界 scope 作底版");
        assertEquals(1, full.summaries().size(), "应带出表3认定意见");
        assertEquals("广东电网", full.base().rightHolder(), "base 取上一版真实权属主体");
        assertEquals("持有权", full.base().rightType());
    }

    @Test
    @DisplayName("无上一版确权 → 退回身份级合成桩,fromRealConfirm=false 且明细为空")
    void baseline_noPrior_fallsBackToStub() {
        when(applyMapper.selectOne(any())).thenReturn(null);
        ChangeBaseline stub = new ChangeBaseline("营销系统", "中国南方电网有限责任公司", "分省公司",
                "数字化部", "持有权", "非管制", "A", "H", "2025-04-20", 1);
        when(catalogService.baselineOf("营销系统")).thenReturn(stub);

        ChangeBaselineFull full = svc.baselineOf(null, "营销系统");

        assertFalse(full.fromRealConfirm(), "无上一版应退回合成桩");
        assertNull(full.priorApplyId());
        assertTrue(full.tableItems().isEmpty(), "合成桩无表3行");
        assertTrue(full.cards().isEmpty(), "合成桩无权益卡片");
        assertEquals("营销系统", full.base().sysName(), "base 退回目录合成桩");
    }
}
