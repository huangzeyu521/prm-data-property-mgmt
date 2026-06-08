package com.csg.prm.ledger.service;

import com.csg.prm.ledger.dto.LedgerOverviewVO;

/**
 * 产权总体概览服务:聚合确权规模、覆盖率、产权类型与组织分布等核心指标。
 */
public interface LedgerOverviewService {

    LedgerOverviewVO overview();
}
