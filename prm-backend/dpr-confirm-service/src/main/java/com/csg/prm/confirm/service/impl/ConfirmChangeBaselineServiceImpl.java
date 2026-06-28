package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.csg.prm.confirm.service.ConfirmChangeBaselineService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class ConfirmChangeBaselineServiceImpl implements ConfirmChangeBaselineService {

    private static final String SYS_PREFIX = "SYS:";

    private final ConfirmApplyMapper applyMapper;
    private final ConfirmTableItemMapper itemMapper;
    private final EquityCardMapper cardMapper;
    private final ConfirmSummaryMapper summaryMapper;
    private final DataCatalogService catalogService;

    public ConfirmChangeBaselineServiceImpl(ConfirmApplyMapper applyMapper, ConfirmTableItemMapper itemMapper,
                                            EquityCardMapper cardMapper, ConfirmSummaryMapper summaryMapper,
                                            DataCatalogService catalogService) {
        this.applyMapper = applyMapper;
        this.itemMapper = itemMapper;
        this.cardMapper = cardMapper;
        this.summaryMapper = summaryMapper;
        this.catalogService = catalogService;
    }

    @Override
    public ChangeBaselineFull baselineOf(String assetId, String sysName) {
        ConfirmApply prior = findPriorConfirmed(assetId, sysName);
        if (prior == null) {
            // 无上一版真实确权 → 退回目录合成桩(身份级),明细为空
            ChangeBaseline stub = StringUtils.hasText(sysName) ? catalogService.baselineOf(sysName) : null;
            return new ChangeBaselineFull(stub, false, null,
                    Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        // 上一版表3逐表行(真实底版)
        List<ConfirmTableItem> items = itemMapper.selectList(new LambdaQueryWrapper<ConfirmTableItem>()
                .eq(ConfirmTableItem::getApplyId, prior.getApplyId())
                .orderByAsc(ConfirmTableItem::getCreateTime));
        // 上一版权益卡片(表4/边界底版:正常态,按权益类型排序便于持有/使用/经营对齐)
        List<EquityCard> cards = cardMapper.selectList(new LambdaQueryWrapper<EquityCard>()
                .eq(EquityCard::getAssetId, prior.getAssetId())
                .eq(EquityCard::getCardStatus, EquityCard.STATUS_NORMAL)
                .orderByAsc(EquityCard::getRightType));
        // 上一版表3/表4认定意见(来源留痕)
        List<ConfirmSummary> sums = summaryMapper.selectList(new LambdaQueryWrapper<ConfirmSummary>()
                .eq(ConfirmSummary::getApplyId, prior.getApplyId())
                .orderByAsc(ConfirmSummary::getSummaryType));

        ChangeBaseline base = new ChangeBaseline(
                StringUtils.hasText(sysName) ? sysName : prior.getAssetName(),
                prior.getRightHolder(), prior.getSubjectLevel(), prior.getRespDept(),
                prior.getRightType(), prior.getRegulated(),
                prior.getSourceIdentification(), prior.getRelationIdentification(),
                authTimeOf(prior), versionOf(prior));

        return new ChangeBaselineFull(base, true, prior.getApplyId(),
                items,
                cards.stream().map(c -> new ChangeBaselineFull.CardView(
                        c.getCardNo(), c.getRightType(), c.getScope(),
                        c.getValidDate(), c.getCardStatus(), c.getConsolidatedUnit())).toList(),
                sums.stream().map(s -> new ChangeBaselineFull.SummaryView(
                        s.getSummaryType(), s.getContent(), s.getGeneratorId())).toList());
    }

    /** 反查"最近一笔已完成(制卡)"确权:① assetId 精确;② 系统级兜底 assetName==sysName。 */
    private ConfirmApply findPriorConfirmed(String assetId, String sysName) {
        String aid = StringUtils.hasText(assetId) ? assetId
                : (StringUtils.hasText(sysName) ? SYS_PREFIX + sysName : null);
        if (StringUtils.hasText(aid)) {
            ConfirmApply byId = latestDone(new LambdaQueryWrapper<ConfirmApply>()
                    .eq(ConfirmApply::getAssetId, aid));
            if (byId != null) {
                return byId;
            }
        }
        if (StringUtils.hasText(sysName)) {
            return latestDone(new LambdaQueryWrapper<ConfirmApply>()
                    .eq(ConfirmApply::getAssetName, sysName));
        }
        return null;
    }

    private ConfirmApply latestDone(LambdaQueryWrapper<ConfirmApply> w) {
        w.eq(ConfirmApply::getStatus, ConfirmApply.STATUS_DONE)
                .orderByDesc(ConfirmApply::getCreateTime)
                .last("LIMIT 1");
        return applyMapper.selectOne(w);
    }

    private String authTimeOf(ConfirmApply p) {
        if (p.getValidDate() != null) {
            return p.getValidDate().toLocalDate().toString();
        }
        return p.getCreateTime() == null ? "" : p.getCreateTime().toLocalDate().toString();
    }

    private int versionOf(ConfirmApply p) {
        return p.getChangeVersion() != null ? p.getChangeVersion() : 1;
    }
}
