package com.csg.prm.confirm.service;

import com.csg.prm.confirm.integration.dto.ChangeBaselineFull;

/**
 * 确权变更基线服务:反查上一版"真实确权结论"(表3逐表行 + 权益卡片 + 认定意见)作变更底版。
 * 替代早期"据目录已确权库表合成桩"的弱基线,使确权变更在权威结论之上做差异编辑。
 */
public interface ConfirmChangeBaselineService {

    /**
     * @param assetId 资产ID(资产级 AST-xxx 或系统级 SYS:系统名);可空,空则据 sysName 推导
     * @param sysName 系统名称(系统级变更);可空
     * @return 完整基线;无上一版确权时 fromRealConfirm=false,base 退回目录合成桩
     */
    ChangeBaselineFull baselineOf(String assetId, String sysName);
}
