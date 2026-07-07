package com.csg.prm.confirm.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.integration.dto.AssetArchiveRowVO;
import com.csg.prm.confirm.integration.dto.AssetPropertyVO;
import com.csg.prm.confirm.integration.dto.PlatformCardRef;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据集产权档案管理:只读查询当前账户可见的数据资产卡片的确权/授权概要。
 * 列表来源遵循"卡片清单以平台为准"——平台已接入则用平台权限清单;未接入则回退 PRM 已确权资产。
 * 不提供新增/批量新增(卡片是平台主体,产权模块不创建卡片)。
 */
@Service
public class AssetCardArchiveService {

    private final ConfirmApplyMapper applyMapper;
    private final AssetCardIntegrationService integration;
    private final PlatformCardClient platform;

    public AssetCardArchiveService(ConfirmApplyMapper applyMapper,
                                   AssetCardIntegrationService integration, PlatformCardClient platform) {
        this.applyMapper = applyMapper;
        this.integration = integration;
        this.platform = platform;
    }

    /** 档案分页查询(全量可见)。keyword 命中资产名/ID;state 按确权状态筛选。 */
    public PageResult<AssetArchiveRowVO> page(PageRequest query, String keyword, String state) {
        return page(query, keyword, state, false);
    }

    /**
     * 档案分页查询。keyword 命中资产名/ID;state 按确权状态筛选;
     * mine=true 时只保留当前登录用户"我负责的"资产(其发起过确权登记 creatorId=me),供申报人作战面板收敛视图。
     */
    public PageResult<AssetArchiveRowVO> page(PageRequest query, String keyword, String state, boolean mine) {
        // 数据源:平台权限清单优先,未接入回退 PRM 已确权资产(兜底)
        List<String> assetIds = platform.platformAvailable() ? platform.listVisibleAssetIds() : prmConfirmedAssetIds();

        Set<String> myAssets = mine ? myAssetIds() : null;

        List<AssetArchiveRowVO> rows = assetIds.stream().distinct()
                .filter(id -> myAssets == null || myAssets.contains(id))
                .map(this::toRow)
                .filter(r -> matchKeyword(r, keyword))
                .filter(r -> !StringUtils.hasText(state) || state.equals(r.state()))
                .toList();

        long current = query.getCurrent() <= 0 ? 1 : query.getCurrent();
        long size = query.getSize() <= 0 ? 10 : query.getSize();
        int from = (int) Math.min((current - 1) * size, rows.size());
        int to = (int) Math.min(from + size, rows.size());
        return new PageResult<>((long) rows.size(), (int) current, (int) size, rows.subList(from, to));
    }

    /**
     * 按关键词搜索可关联的数据资产卡片(卡片名称/编码/系统·表)。
     * 平台已接入则搜平台卡片;未接入回退 PRM 已登记资产(仅 assetId+assetName 可得)。
     */
    public List<PlatformCardRef> searchCards(String keyword, int limit) {
        int lim = limit <= 0 ? 10 : Math.min(limit, 50);
        if (platform.platformAvailable()) {
            return platform.searchCards(keyword, lim);
        }
        Map<String, String> seen = new LinkedHashMap<>();
        for (ConfirmApply a : applyMapper.selectList(new LambdaQueryWrapper<ConfirmApply>()
                .isNotNull(ConfirmApply::getAssetId).orderByDesc(ConfirmApply::getCreateTime))) {
            if (!StringUtils.hasText(a.getAssetId())) {
                continue;
            }
            if (StringUtils.hasText(keyword)
                    && !safe(a.getAssetId()).contains(keyword) && !safe(a.getAssetName()).contains(keyword)) {
                continue;
            }
            seen.putIfAbsent(a.getAssetId(), a.getAssetName());
            if (seen.size() >= lim) {
                break;
            }
        }
        return seen.entrySet().stream()
                .map(e -> new PlatformCardRef(e.getKey(), e.getValue(), null, null, null, null, null))
                .toList();
    }

    /**
     * assetId 引用完整性:平台已接入时校验对应卡片真实存在(杜绝幽灵资产);
     * 平台未接入则不阻断(无权威源可校验,放行草稿/dev)。
     */
    public boolean assetCardResolvable(String assetId) {
        if (!StringUtils.hasText(assetId)) {
            return false;
        }
        if (!platform.platformAvailable()) {
            return true;
        }
        return platform.cardExists(assetId);
    }

    /** PRM 兜底:已发起确权的资产ID(去重,最近确权在前)。平台清单接入后此分支不再走。 */
    private List<String> prmConfirmedAssetIds() {
        return applyMapper.selectList(new LambdaQueryWrapper<ConfirmApply>()
                        .isNotNull(ConfirmApply::getAssetId)
                        .orderByDesc(ConfirmApply::getCreateTime))
                .stream().map(ConfirmApply::getAssetId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /**
     * 当前登录用户"我负责的"资产集合:其发起过确权登记(creatorId=me)的 assetId。
     * 无用户上下文(内部/未启用认证/单测)→ 空集:mine 请求下不回退全量,避免把他人资产暴露给收敛视图。
     * 归属口径与 ConfirmApplyServiceImpl 草稿隔离一致(creatorId 匹配)。
     */
    private Set<String> myAssetIds() {
        UserContext ctx = UserContextHolder.get();
        String me = ctx == null ? null : ctx.getUserId();
        if (!StringUtils.hasText(me)) {
            return Set.of();
        }
        return applyMapper.selectList(new LambdaQueryWrapper<ConfirmApply>()
                        .eq(ConfirmApply::getCreatorId, me)
                        .isNotNull(ConfirmApply::getAssetId))
                .stream().map(ConfirmApply::getAssetId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private AssetArchiveRowVO toRow(String assetId) {
        AssetPropertyVO p = integration.property(assetId);
        int equityCount = integration.equity(assetId).size();
        return new AssetArchiveRowVO(assetId, p.assetName(), p.state(), p.rightType(), p.rightHolder(),
                p.registerType(), p.respDept(), p.validDate(), p.confirmTime(), equityCount);
    }

    private static boolean matchKeyword(AssetArchiveRowVO r, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return safe(r.assetName()).contains(keyword) || safe(r.assetId()).contains(keyword);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
