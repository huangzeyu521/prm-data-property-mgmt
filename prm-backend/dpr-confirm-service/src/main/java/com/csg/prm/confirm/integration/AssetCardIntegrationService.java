package com.csg.prm.confirm.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.integration.dto.AssetEquityVO;
import com.csg.prm.confirm.integration.dto.AssetPropertyVO;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 数据资产卡片集成服务:以平台资产ID(assetId)为键,把产权模块权威产出
 * (确权登记 ConfirmApply + 权益卡片 EquityCard)装配成卡片「产权信息 / 权益基本信息」两个子Tab的只读契约。
 * 产权模块为权威源,卡片只读消费;含 待确权/确权中/已确权/已驳回 边界态,卡片永不空白。
 */
@Service
public class AssetCardIntegrationService {

    public static final String STATE_NONE = "待确权";
    public static final String STATE_PROGRESS = "确权中";
    public static final String STATE_DONE = "已确权";
    public static final String STATE_REJECTED = "已驳回";

    private final ConfirmApplyMapper applyMapper;
    private final EquityCardMapper cardMapper;
    private final AssetCardFieldMapper adapter;

    public AssetCardIntegrationService(ConfirmApplyMapper applyMapper, EquityCardMapper cardMapper,
                                       AssetCardFieldMapper adapter) {
        this.applyMapper = applyMapper;
        this.cardMapper = cardMapper;
        this.adapter = adapter;
    }

    /** 「产权信息」子Tab:取该资产最新有效确权登记;无则返回"待确权"占位。 */
    public AssetPropertyVO property(String assetId) {
        if (!StringUtils.hasText(assetId)) {
            return AssetPropertyVO.placeholder(assetId, STATE_NONE, "缺少资产ID");
        }
        List<ConfirmApply> list = applyMapper.selectList(new LambdaQueryWrapper<ConfirmApply>()
                .eq(ConfirmApply::getAssetId, assetId)
                .orderByDesc(ConfirmApply::getCreateTime));
        if (list.isEmpty()) {
            return AssetPropertyVO.placeholder(assetId, STATE_NONE,
                    "该资产尚未发起确权,请在数据产权管理模块发起确权登记");
        }
        // 已确权(终审通过)优先,取最新一条
        ConfirmApply done = first(list, a -> ConfirmApply.STATUS_DONE.equals(a.getStatus()));
        if (done != null) {
            String msg = isChange(done) ? "本次为确权变更,展示最新确权结论" : null;
            return AssetPropertyVO.of(done, STATE_DONE, msg);
        }
        // 在途(草稿/审批中)→ 确权中;已驳回·已撤回为终态,须排除(否则撤回单会被误报"审核中(节点 null)")
        ConfirmApply inProgress = first(list, a ->
                !ConfirmApply.STATUS_REJECTED.equals(a.getStatus())
                        && !ConfirmApply.STATUS_WITHDRAWN.equals(a.getStatus()));
        if (inProgress != null) {
            return AssetPropertyVO.of(inProgress, STATE_PROGRESS,
                    "确权审核中(当前节点 " + inProgress.getCurrentNode() + ")");
        }
        // 仅有终态记录:按最新一条区分 已撤回 / 已驳回
        ConfirmApply latest = list.get(0);
        if (ConfirmApply.STATUS_WITHDRAWN.equals(latest.getStatus())) {
            return AssetPropertyVO.of(latest, STATE_NONE, "最近一次确权已撤回,可重新发起确权");
        }
        return AssetPropertyVO.of(latest, STATE_REJECTED,
                "确权被驳回:" + (StringUtils.hasText(latest.getRejectReason()) ? latest.getRejectReason() : "请重新发起"));
    }

    /** 「权益基本信息」子Tab:该资产名下未失效的权益条目(三权)。 */
    public List<AssetEquityVO> equity(String assetId) {
        if (!StringUtils.hasText(assetId)) {
            return List.of();
        }
        List<EquityCard> cards = cardMapper.selectList(new LambdaQueryWrapper<EquityCard>()
                .eq(EquityCard::getAssetId, assetId)
                .ne(EquityCard::getCardStatus, EquityCard.STATUS_INVALID)
                .orderByDesc(EquityCard::getCreateTime));
        return cards.stream().map(AssetEquityVO::of).toList();
    }

    /** 产权信息按平台字段名输出(经适配层),供平台卡片直接消费。 */
    public Map<String, Object> propertyForPlatform(String assetId) {
        return adapter.toPlatformProperty(property(assetId));
    }

    private static boolean isChange(ConfirmApply a) {
        return "确权变更".equals(a.getRegisterType()) || Boolean.TRUE.equals(a.getReConfirm());
    }

    private static ConfirmApply first(List<ConfirmApply> list, java.util.function.Predicate<ConfirmApply> p) {
        return list.stream().filter(p).findFirst().orElse(null);
    }
}
