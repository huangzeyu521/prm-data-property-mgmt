package com.csg.prm.confirm.service;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmSummary;

import java.util.List;

/**
 * 确权汇总表服务(表3/表4)。在合规审核节点自动生成。
 */
public interface ConfirmSummaryService {

    /** 由确权申请自动生成表3、表4 */
    void generate(ConfirmApply apply);

    /** 按申请查询汇总表 */
    List<ConfirmSummary> listByApply(String applyId);
}
