package com.csg.prm.common.writeback;

/**
 * 台账回写网关(确权/授权域 -> 数据产权台账)。
 * 关键节点产生的 {@link RightsEvent} 经此回写产权档案(确权/授权状态、权益卡片)并留变更记录。
 * 默认 {@link LocalLedgerWritebackGateway} 空操作(离线/测试);
 * 配置 prm.ledger.writeback-enabled=true 时启用 {@link HttpLedgerWritebackGateway}(@Primary)真打台账服务。
 */
public interface LedgerWritebackGateway {

    /** 回写一条产权事件到台账;失败不应阻断主流程(确权/授权已成功) */
    void apply(RightsEvent event);
}
