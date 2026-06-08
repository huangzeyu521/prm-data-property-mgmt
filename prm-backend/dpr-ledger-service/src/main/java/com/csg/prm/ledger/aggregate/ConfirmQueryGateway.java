package com.csg.prm.ledger.aggregate;

import java.util.List;

/**
 * 确权域只读查询网关(台账服务侧)。默认本地空桩(离线/单测),
 * prm.aggregate.enabled=true 时由 HTTP 实现覆盖,跨服务读取确权申请。
 */
public interface ConfirmQueryGateway {

    /** 某资产的全部确权记录(按时间倒序) */
    List<DomainRecord> findByAsset(String assetId);

    /** 待办:处于审批中(合规审核/主管复核/经理终审)的确权申请 */
    List<DomainRecord> pending();
}
