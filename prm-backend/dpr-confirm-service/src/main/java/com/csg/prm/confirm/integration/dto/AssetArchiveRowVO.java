package com.csg.prm.confirm.integration.dto;

import java.time.LocalDateTime;

/**
 * 数据集产权档案管理列表行:一张数据资产卡片的确权/授权概要(只读)。
 * 点开行→复用 by-assetId 的产权信息/权益基本信息明细契约。
 */
public record AssetArchiveRowVO(
        String assetId,
        String assetName,
        String state,            // 待确权 / 确权中 / 已确权 / 已驳回
        String rightType,        // 权属类型
        String rightHolder,      // 权利主体
        String registerType,     // 初始确权 / 确权变更
        String respDept,         // 数据责任部门
        LocalDateTime validDate, // 有效期
        LocalDateTime confirmTime,
        int equityCount          // 权益条目数
) {
}
