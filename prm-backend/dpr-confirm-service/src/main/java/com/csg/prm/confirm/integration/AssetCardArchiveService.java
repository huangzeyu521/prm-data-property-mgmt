package com.csg.prm.confirm.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.integration.dto.AssetArchiveRowVO;
import com.csg.prm.confirm.integration.dto.AssetPropertyVO;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

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

    /** 档案分页查询。keyword 命中资产名/ID;state 按确权状态筛选。 */
    public PageResult<AssetArchiveRowVO> page(PageQuery query, String keyword, String state) {
        // 数据源:平台权限清单优先,未接入回退 PRM 已确权资产(兜底)
        List<String> assetIds = platform.platformAvailable() ? platform.listVisibleAssetIds() : prmConfirmedAssetIds();

        List<AssetArchiveRowVO> rows = assetIds.stream().distinct()
                .map(this::toRow)
                .filter(r -> matchKeyword(r, keyword))
                .filter(r -> !StringUtils.hasText(state) || state.equals(r.state()))
                .toList();

        long current = query.getCurrent() <= 0 ? 1 : query.getCurrent();
        long size = query.getSize() <= 0 ? 10 : query.getSize();
        int from = (int) Math.min((current - 1) * size, rows.size());
        int to = (int) Math.min(from + size, rows.size());
        return new PageResult<>(rows.size(), current, size, rows.subList(from, to));
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
