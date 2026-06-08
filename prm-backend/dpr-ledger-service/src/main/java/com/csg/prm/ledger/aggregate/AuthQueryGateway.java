package com.csg.prm.ledger.aggregate;

import java.util.List;

/**
 * 授权域只读查询网关(台账服务侧)。默认本地空桩(离线/单测),
 * prm.aggregate.enabled=true 时由 HTTP 实现覆盖,跨服务读取授权申请。
 */
public interface AuthQueryGateway {

    /** 某资产的全部授权记录(按时间倒序) */
    List<DomainRecord> findByAsset(String assetId);

    /** 待办:处于审批链中(未生效、未驳回、非草稿)的授权申请 */
    List<DomainRecord> pending();
}
