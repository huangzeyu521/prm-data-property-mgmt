package com.csg.prm.ledger.monitor.schedule;

import com.csg.prm.ledger.monitor.dto.ComplianceReportVO;
import com.csg.prm.ledger.monitor.service.ApplyBacklogService;
import com.csg.prm.ledger.monitor.service.ComplianceCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 权益动态监测定时巡检器。周期性自动执行:
 * ① 有效期合规巡检(本地档案,识别到期/过期);
 * ② 确权/授权申请审核积压巡检(经聚合网关,覆盖"申请、审核"环节)。
 * 自动采集状态变化与异常并生成预警,落地可研"实时监控/自动采集"要求(替代原手工触发)。
 */
@Component
public class MonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitorScheduler.class);

    private final ComplianceCheckService complianceCheckService;
    private final ApplyBacklogService applyBacklogService;

    public MonitorScheduler(ComplianceCheckService complianceCheckService, ApplyBacklogService applyBacklogService) {
        this.complianceCheckService = complianceCheckService;
        this.applyBacklogService = applyBacklogService;
    }

    /** 默认每 10 分钟一次(prm.monitor.scan-interval-ms 可配),启动后 20s 首次执行。 */
    @Scheduled(fixedDelayString = "${prm.monitor.scan-interval-ms:600000}",
            initialDelayString = "${prm.monitor.scan-initial-delay-ms:20000}")
    public void scan() {
        try {
            ComplianceReportVO report = complianceCheckService.runComplianceCheck();
            int backlog = applyBacklogService.scanBacklog(7);
            if (report.getHitCount() + backlog > 0) {
                log.info("[监测定时巡检] 合规命中={} 审核积压={} -> 已自动生成预警/检查报告 {}",
                        report.getHitCount(), backlog, report.getReportId());
            }
        } catch (RuntimeException e) {
            log.warn("[监测定时巡检] 执行异常(忽略,下个周期重试): {}", e.getMessage());
        }
    }
}
