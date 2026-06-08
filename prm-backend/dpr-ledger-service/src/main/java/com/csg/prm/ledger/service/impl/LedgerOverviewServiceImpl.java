package com.csg.prm.ledger.service.impl;

import com.csg.prm.ledger.dto.LedgerOverviewVO;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.DataAssetInfoMapper;
import com.csg.prm.ledger.mapper.PropertyArchiveMapper;
import com.csg.prm.ledger.service.LedgerOverviewService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LedgerOverviewServiceImpl implements LedgerOverviewService {

    private static final String STATUS_CONFIRMED = "已确权";
    private static final String UNKNOWN = "未分类";

    private final DataAssetInfoMapper assetMapper;
    private final PropertyArchiveMapper archiveMapper;

    public LedgerOverviewServiceImpl(DataAssetInfoMapper assetMapper, PropertyArchiveMapper archiveMapper) {
        this.assetMapper = assetMapper;
        this.archiveMapper = archiveMapper;
    }

    @Override
    public LedgerOverviewVO overview() {
        List<DataAssetInfo> assets = assetMapper.selectList(null);
        List<PropertyArchive> archives = archiveMapper.selectList(null);

        // 已知资产 = 资产目录 ∪ 产权档案。流程驱动建档的资产(确权制卡回写)即使尚未同步到资产目录,
        // 也应计入台账概览,保证"走完确权流程,概览/看板实时一致"。
        Set<String> knownAssetIds = new HashSet<>();
        Set<String> confirmedAssetIds = new HashSet<>();
        Map<String, Long> rightTypeDist = new LinkedHashMap<>();
        if (archives != null) {
            for (PropertyArchive a : archives) {
                if (StringUtils.hasText(a.getAssetId())) {
                    knownAssetIds.add(a.getAssetId());
                }
                if (STATUS_CONFIRMED.equals(a.getConfirmStatus())) {
                    confirmedAssetIds.add(a.getAssetId());
                }
                String rt = StringUtils.hasText(a.getRightType()) ? a.getRightType() : UNKNOWN;
                rightTypeDist.merge(rt, 1L, Long::sum);
            }
        }

        Map<String, Long> subsidiaryDist = new LinkedHashMap<>();
        if (assets != null) {
            for (DataAssetInfo asset : assets) {
                if (StringUtils.hasText(asset.getAssetId())) {
                    knownAssetIds.add(asset.getAssetId());
                }
                String sub = StringUtils.hasText(asset.getSubsidiaryName()) ? asset.getSubsidiaryName() : UNKNOWN;
                subsidiaryDist.merge(sub, 1L, Long::sum);
            }
        }

        long total = knownAssetIds.size();
        // confirmedAssetIds 均来自档案,必然 ⊆ knownAssetIds
        long confirmed = confirmedAssetIds.size();

        double rate = total == 0 ? 0d
                : BigDecimal.valueOf(confirmed * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();

        LedgerOverviewVO vo = new LedgerOverviewVO();
        vo.setTotalAssets(total);
        vo.setConfirmedAssets(confirmed);
        vo.setUnconfirmedAssets(total - confirmed);
        vo.setConfirmRate(rate);
        vo.setRightTypeDistribution(rightTypeDist);
        vo.setSubsidiaryDistribution(subsidiaryDist);
        return vo;
    }
}
