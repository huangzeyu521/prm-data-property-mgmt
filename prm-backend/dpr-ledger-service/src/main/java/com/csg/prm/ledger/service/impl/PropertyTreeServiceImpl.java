package com.csg.prm.ledger.service.impl;

import com.csg.prm.ledger.dto.PropertyTreeNode;
import com.csg.prm.ledger.entity.DataAssetInfo;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.DataAssetInfoMapper;
import com.csg.prm.ledger.mapper.PropertyArchiveMapper;
import com.csg.prm.ledger.service.PropertyTreeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PropertyTreeServiceImpl implements PropertyTreeService {

    private static final String UNKNOWN = "未分类";
    private static final String STATUS_UNCONFIRMED = "未确权";

    private final DataAssetInfoMapper assetMapper;
    private final PropertyArchiveMapper archiveMapper;

    public PropertyTreeServiceImpl(DataAssetInfoMapper assetMapper, PropertyArchiveMapper archiveMapper) {
        this.assetMapper = assetMapper;
        this.archiveMapper = archiveMapper;
    }

    @Override
    public List<PropertyTreeNode> buildTree() {
        List<DataAssetInfo> assets = assetMapper.selectList(null);
        Map<String, String> confirmStatusMap = loadConfirmStatus();

        // 三层分组容器,保持插入顺序
        Map<String, PropertyTreeNode> subsidiaryNodes = new LinkedHashMap<>();
        Map<String, PropertyTreeNode> systemNodes = new LinkedHashMap<>();
        Map<String, PropertyTreeNode> schemaNodes = new LinkedHashMap<>();

        if (assets != null) {
            for (DataAssetInfo asset : assets) {
                String sub = orUnknown(asset.getSubsidiaryName());
                String sys = orUnknown(asset.getSystemName());
                String sch = orUnknown(asset.getSchemaName());

                String subKey = sub;
                PropertyTreeNode subNode = subsidiaryNodes.computeIfAbsent(subKey,
                        k -> new PropertyTreeNode("SUB:" + k, sub, PropertyTreeNode.TYPE_SUBSIDIARY));

                String sysKey = subKey + "|" + sys;
                PropertyTreeNode sysNode = systemNodes.computeIfAbsent(sysKey, k -> {
                    PropertyTreeNode n = new PropertyTreeNode("SYS:" + k, sys, PropertyTreeNode.TYPE_SYSTEM);
                    subNode.getChildren().add(n);
                    return n;
                });

                String schKey = sysKey + "|" + sch;
                PropertyTreeNode schNode = schemaNodes.computeIfAbsent(schKey, k -> {
                    PropertyTreeNode n = new PropertyTreeNode("SCH:" + k, sch, PropertyTreeNode.TYPE_SCHEMA);
                    sysNode.getChildren().add(n);
                    return n;
                });

                PropertyTreeNode dataset = new PropertyTreeNode("DS:" + asset.getAssetId(),
                        asset.getAssetName(), PropertyTreeNode.TYPE_DATASET);
                dataset.setAssetId(asset.getAssetId());
                dataset.setConfirmStatus(confirmStatusMap.getOrDefault(asset.getAssetId(), STATUS_UNCONFIRMED));
                schNode.getChildren().add(dataset);
            }
        }
        return new ArrayList<>(subsidiaryNodes.values());
    }

    /** 资产ID -> 确权状态;同一资产多档时"已确权"优先 */
    private Map<String, String> loadConfirmStatus() {
        Map<String, String> map = new HashMap<>();
        List<PropertyArchive> archives = archiveMapper.selectList(null);
        if (archives != null) {
            for (PropertyArchive a : archives) {
                String status = a.getConfirmStatus();
                if (!StringUtils.hasText(status)) {
                    continue;
                }
                String prev = map.get(a.getAssetId());
                if (prev == null || "已确权".equals(status)) {
                    map.put(a.getAssetId(), status);
                }
            }
        }
        return map;
    }

    private String orUnknown(String v) {
        return StringUtils.hasText(v) ? v : UNKNOWN;
    }
}
