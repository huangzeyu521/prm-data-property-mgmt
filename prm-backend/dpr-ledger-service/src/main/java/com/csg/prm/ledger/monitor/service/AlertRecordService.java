package com.csg.prm.ledger.monitor.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.monitor.dto.AlertRecordQuery;
import com.csg.prm.ledger.monitor.dto.AlertStatsVO;
import com.csg.prm.ledger.monitor.entity.AlertRecord;

/**
 * 风险预警服务:预警生成、处置闭环、统计。
 */
public interface AlertRecordService {

    /** 生成预警(由状态监控/合规检查触发),返回预警ID */
    String raise(String ruleId, String source, String assetId, String alertLevel,
                 String triggerCond, String abnormalDesc);

    /** 是否已存在同资产+同触发条件的未关闭预警(定时巡检去重,避免重复刷预警) */
    boolean existsOpen(String assetId, String triggerCond);

    /** 受理处置(待处理 -> 处理中) */
    void dispose(String alertId, String feedback);

    /** 处置闭环(-> 已关闭) */
    void close(String alertId, String feedback);

    PageResult<AlertRecord> page(AlertRecordQuery query);

    AlertStatsVO stats();
}
