package com.csg.prm.ledger.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.dto.PropertyChangeRecordQuery;
import com.csg.prm.ledger.entity.PropertyChangeRecord;

/**
 * 产权变更记录服务。记录由业务流程自动触发,人工增删受控。
 */
public interface PropertyChangeRecordService {

    /** 记录一条变更(供确权/授权流程及档案变更调用) */
    String record(String assetId, String changeType, String fieldName,
                  String beforeValue, String afterValue, String reason,
                  String sourceFlow, String sourceTicket);

    PageResult<PropertyChangeRecord> page(PropertyChangeRecordQuery query);
}
