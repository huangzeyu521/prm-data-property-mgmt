package com.csg.prm.confirm.aitool.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.aitool.entity.AitDecision;

/**
 * 智能确权辅助工具-确权决策支持服务(M3 / SW-007~008)。
 * 关键因子加权分析 + 确权结果预测 + 权益分割方案 + 决策建议生成 + 证据链。
 */
public interface AitDecisionService {

    /** 对一笔确权申请做智能研判,生成决策建议 */
    AitDecision analyze(String applyId);

    AitDecision getByApply(String applyId);

    PageResult<AitDecision> page(PageRequest query, String prediction);
}
