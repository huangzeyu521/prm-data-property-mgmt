package com.csg.prm.ledger.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.dto.PropertyArchiveQuery;
import com.csg.prm.ledger.entity.PropertyArchive;

/**
 * 产权档案("一数一档")业务服务。
 */
public interface PropertyArchiveService {

    /** 新增产权档案,返回档案ID */
    String create(PropertyArchive archive);

    /**
     * 应用产权事件(确权制卡/授权生效回写):按资产ID 建档或更新确权/授权状态、权益卡片,并留变更记录。
     * 使台账由真实确权/授权流程实时驱动(P0-① 流程贯通)。
     */
    void applyRightsEvent(com.csg.prm.common.writeback.RightsEvent event);

    /** 修改产权档案 */
    void update(PropertyArchive archive);

    /** 删除产权档案(逻辑删除),含关联约束校验 */
    void delete(String archiveId);

    /** 按ID查询档案详情 */
    PropertyArchive getById(String archiveId);

    /** 多维分页查询 */
    PageResult<PropertyArchive> page(PropertyArchiveQuery query);
}
