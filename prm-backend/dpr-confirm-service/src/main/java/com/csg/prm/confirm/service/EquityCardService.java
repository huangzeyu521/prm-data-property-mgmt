package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;

import java.util.List;

/**
 * 权益卡片服务。确权终审通过后自动生成;支持冻结/解冻/注销(冻结/失效后不可用于下游授权),全程变更留痕。
 */
public interface EquityCardService {

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

    PageResult<EquityCard> page(PageQuery query);
}
