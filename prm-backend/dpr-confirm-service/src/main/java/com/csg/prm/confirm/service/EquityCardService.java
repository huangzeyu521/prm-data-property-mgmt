package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;

import java.util.List;

/**
 * 权益卡片服务。确权终审通过后自动生成;支持冻结/解冻/注销(冻结/失效后不可用于下游授权),全程变更留痕。
 */
public interface EquityCardService {

    /** 按卡片编号查询(供授权服务跨服务"先确后授"真实校验) */
    com.csg.prm.confirm.entity.EquityCard findByNo(String cardNo);

    /** 由确权申请生成权益卡片,返回卡片ID */
    String generateFromApply(ConfirmApply apply);

    EquityCard getById(String cardId);

    void freeze(String cardId);

    /** 解冻:冻结 -> 正常 */
    void unfreeze(String cardId);

    /** 注销:-> 失效(不可逆) */
    void revoke(String cardId, String reason);

    /** 卡片变更历史 */
    List<EquityCardLog> listLogs(String cardId);

    /**
     * 当前有效权益卡片(资产+权利维度的最新"正常"卡,版本最高者)。
     * 供确权变更"基于现状预填"与下游展示;无则返回 null。
     */
    EquityCard findCurrentValid(String assetId, String rightType);

    PageResult<EquityCard> page(com.csg.prm.confirm.dto.EquityCardQuery query);

    /** 权益卡片概览统计(总/正常/冻结/失效/即将到期),按过滤聚合忽略 status。 */
    com.csg.prm.confirm.dto.EquityCardStats stats(com.csg.prm.confirm.dto.EquityCardQuery query);

    /**
     * 待重确权清单(F指导书"按季度定期重新确权"/权益到期):
     * 列出正常状态、有效期在 daysAhead 天内(含已到期)的权益卡片,供季度重确权提醒。
     */
    List<EquityCard> listReConfirmDue(int daysAhead);
}
