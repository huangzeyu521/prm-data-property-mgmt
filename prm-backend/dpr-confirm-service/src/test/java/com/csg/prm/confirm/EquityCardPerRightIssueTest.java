package com.csg.prm.confirm;

import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.org.Jurisdiction;
import com.csg.prm.common.org.OrgService;
import com.csg.prm.common.writeback.LedgerWritebackGateway;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.mapper.ConfirmTableItemMapper;
import com.csg.prm.confirm.mapper.EquityCardLogMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.EquityCertService;
import com.csg.prm.confirm.service.impl.EquityCardServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 逐权制卡单测:多权拼接的权属类型必须拆成"一卡一权",每张卡 rightType 为单一权益,
 * 以便授权侧(findUsableCard/findCurrentValid/EquityCardGateway)按单一 rightType 精确命中。
 */
class EquityCardPerRightIssueTest {

    private final EquityCardMapper mapper = mock(EquityCardMapper.class);
    private final EquityCardLogMapper logMapper = mock(EquityCardLogMapper.class);
    private final ChainEvidenceService chainEvidenceService = mock(ChainEvidenceService.class);
    private final LedgerWritebackGateway ledgerWriteback = mock(LedgerWritebackGateway.class);
    private final EquityCertService certService = mock(EquityCertService.class);
    private final ConfirmTableItemMapper tableItemMapper = mock(ConfirmTableItemMapper.class);
    private final OrgService orgService = mock(OrgService.class);

    private final EquityCardServiceImpl svc = new EquityCardServiceImpl(
            mapper, logMapper, chainEvidenceService, ledgerWriteback, certService, tableItemMapper, orgService);

    private ConfirmApply apply(String rightType) {
        when(orgService.resolve(any())).thenReturn(Jurisdiction.EMPTY);
        when(tableItemMapper.selectCount(any())).thenReturn(0L);
        ConfirmApply a = new ConfirmApply();
        a.setApplyId("CA-X");
        a.setAssetId("AST-X");
        a.setAssetName("客户用电信息表");
        a.setRightHolder("广东电网");
        a.setRegisterType("初始确权");
        a.setRightType(rightType);
        return a;
    }

    @Test
    @DisplayName("多权拼接 → 逐权一卡,每卡 rightType 为单一权益(无顿号),可被授权侧精确命中")
    void multiRight_splitsIntoPerRightCards() {
        ArgumentCaptor<EquityCard> cap = ArgumentCaptor.forClass(EquityCard.class);

        svc.generateFromApply(apply("持有权、使用权、经营权"));

        verify(mapper, times(3)).insert(cap.capture());
        List<String> rights = cap.getAllValues().stream().map(EquityCard::getRightType).toList();
        assertEquals(List.of("持有权", "使用权", "经营权"), rights, "应逐权拆成三张卡");
        assertTrue(rights.stream().noneMatch(r -> r.contains("、")), "单卡 rightType 不应含顿号(可被精确等值命中)");
    }

    @Test
    @DisplayName("单权 → 仅一卡(行为与改造前一致)")
    void singleRight_issuesOneCard() {
        ArgumentCaptor<EquityCard> cap = ArgumentCaptor.forClass(EquityCard.class);

        svc.generateFromApply(apply("持有权"));

        verify(mapper, times(1)).insert(cap.capture());
        assertEquals("持有权", cap.getValue().getRightType());
    }

    @Test
    @DisplayName("逗号/中文逗号混用拆分,去重保序")
    void mixedSeparators_dedupAndSplit() {
        ArgumentCaptor<EquityCard> cap = ArgumentCaptor.forClass(EquityCard.class);

        svc.generateFromApply(apply("使用权,使用权，经营权"));

        verify(mapper, times(2)).insert(cap.capture());
        assertEquals(List.of("使用权", "经营权"),
                cap.getAllValues().stream().map(EquityCard::getRightType).toList(), "重复权益去重、混合分隔符拆分");
    }

    @Test
    @DisplayName("空权属类型 → 不制卡")
    void blankRight_noCard() {
        svc.generateFromApply(apply("   "));
        verify(mapper, never()).insert(any());
        assertFalse(false);
    }
}
