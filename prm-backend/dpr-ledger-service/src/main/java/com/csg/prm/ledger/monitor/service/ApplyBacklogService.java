package com.csg.prm.ledger.monitor.service;

/**
 * 申请/审核阶段监测:扫描确权/授权申请在审批环节停留过久(审核积压)的情况并生成预警。
 * 跨服务只读经聚合网关(prm.aggregate.enabled=true 时生效),覆盖可研"申请、审核"全过程监控。
 */
public interface ApplyBacklogService {

    /**
     * 扫描确权/授权申请审核积压(在审批状态停留超 days 天),生成预警(去重),返回命中数。
     */
    int scanBacklog(int days);
}
