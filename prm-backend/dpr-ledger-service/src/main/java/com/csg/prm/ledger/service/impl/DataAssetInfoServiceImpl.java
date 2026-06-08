package com.csg.prm.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.ledger.dto.DataAssetInfoQuery;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.mapper.DataAssetInfoMapper;
import com.csg.prm.ledger.service.DataAssetInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class DataAssetInfoServiceImpl implements DataAssetInfoService {

    private final DataAssetInfoMapper mapper;

    public DataAssetInfoServiceImpl(DataAssetInfoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String saveOrUpdateAsset(DataAssetInfo asset) {
        if (!StringUtils.hasText(asset.getAssetId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产ID不能为空");
        }
        if (!StringUtils.hasText(asset.getAssetName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产名称不能为空");
        }
        DataAssetInfo exist = mapper.selectById(asset.getAssetId());
        if (exist == null) {
            mapper.insert(asset);
        } else {
            mapper.updateById(asset);
        }
        return asset.getAssetId();
    }

    @Override
    @Transactional
    public int syncBatch(List<DataAssetInfo> assets) {
        if (CollectionUtils.isEmpty(assets)) {
            return 0;
        }
        int count = 0;
        for (DataAssetInfo asset : assets) {
            saveOrUpdateAsset(asset);
            count++;
        }
        return count;
    }

    @Override
    public DataAssetInfo getById(String assetId) {
        DataAssetInfo asset = mapper.selectById(assetId);
        if (asset == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "数据资产不存在");
        }
        return asset;
    }

    @Override
    public PageResult<DataAssetInfo> page(DataAssetInfoQuery query) {
        LambdaQueryWrapper<DataAssetInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getAssetName()), DataAssetInfo::getAssetName, query.getAssetName())
                .eq(StringUtils.hasText(query.getSubsidiaryName()), DataAssetInfo::getSubsidiaryName, query.getSubsidiaryName())
                .eq(StringUtils.hasText(query.getSystemName()), DataAssetInfo::getSystemName, query.getSystemName())
                .eq(StringUtils.hasText(query.getAssetType()), DataAssetInfo::getAssetType, query.getAssetType())
                .orderByDesc(DataAssetInfo::getCreateTime);
        IPage<DataAssetInfo> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public List<DataAssetInfo> listAll() {
        List<DataAssetInfo> list = mapper.selectList(null);
        return list == null ? Collections.emptyList() : list;
    }
}
