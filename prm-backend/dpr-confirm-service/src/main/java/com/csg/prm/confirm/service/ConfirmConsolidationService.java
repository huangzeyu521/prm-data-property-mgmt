package com.csg.prm.confirm.service;

import com.csg.prm.confirm.entity.ConfirmTableItem;

import java.util.List;

/**
 * 表级确权清单(M02)与权益归集判定(分子公司共享网公司)。
 * 规则依据《数据权益内部管理汇总表》说明页 5 条归集判定规则。
 */
public interface ConfirmConsolidationService {

    /** 权益归集判定结果:命中规则 + 网公司三权 + 标准共享判定原因 */
    record ConsolidationResult(String rule, String holdRight, String useRight, String operateRight,
                               boolean involvesThird, String regulated, String reason) {
    }

    /** 批量保存申请的表级数据清单(覆盖式) */
    int saveTableItems(String applyId, List<ConfirmTableItem> items);

    /** 查询申请的表级数据清单 */
    List<ConfirmTableItem> listTableItems(String applyId);

    /** 权益归集判定:输出网公司三权与共享判定原因 */
    ConsolidationResult judgeConsolidation(String applyId);

    /** 导出《数据确权信息汇总表》(32列官方格式) */
    byte[] exportConfirmSummary();

    /** 导出《数据权益内部管理汇总表(分子公司共享网公司)》(34列官方格式) */
    byte[] exportEquityConsolidation();
}
