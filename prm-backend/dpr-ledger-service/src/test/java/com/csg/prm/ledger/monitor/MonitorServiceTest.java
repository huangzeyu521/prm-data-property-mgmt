package com.csg.prm.ledger.monitor;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.service.PropertyArchiveService;
import com.csg.prm.ledger.monitor.dto.AlertRecordQuery;
import com.csg.prm.ledger.monitor.dto.AlertStatsVO;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.entity.MonitorRule;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
import com.csg.prm.ledger.monitor.service.ComplianceCheckService;
import com.csg.prm.ledger.monitor.service.MonitorRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权益动态监测集成测试:监测规则生命周期、预警处置闭环、到期合规巡检联动预警。
 */
@SpringBootTest
@ActiveProfiles("test")
class MonitorServiceTest {

    @Autowired
    private MonitorRuleService ruleService;
    @Autowired
    private AlertRecordService alertService;
    @Autowired
    private ComplianceCheckService complianceService;
    @Autowired
    private PropertyArchiveService archiveService;

    @Test
    void rule_lifecycle_create_enable_disable() {
        MonitorRule rule = new MonitorRule();
        rule.setRuleName("授权到期提醒规则");
        rule.setRuleCategory("到期提醒");
        rule.setThreshold("30");
        String id = ruleService.create(rule);

        assertEquals(MonitorRule.STATUS_DRAFT, ruleService.getById(id).getEffectStatus());
        assertEquals("v1", ruleService.getById(id).getRuleVersion());

        ruleService.enable(id);
        assertEquals(MonitorRule.STATUS_ACTIVE, ruleService.getById(id).getEffectStatus());

        ruleService.disable(id);
        assertEquals(MonitorRule.STATUS_DISABLED, ruleService.getById(id).getEffectStatus());
    }

    @Test
    void alert_dispose_close_loop() {
        String alertId = alertService.raise(null, "状态监控", "AL-001",
                AlertRecord.LEVEL_IMPORTANT, "权限越界", "检测到越权访问");

        AlertRecordQuery q = new AlertRecordQuery();
        q.setAssetId("AL-001");
        assertEquals(AlertRecord.STATUS_PENDING, alertService.page(q).getRecords().get(0).getDisposeStatus());

        alertService.dispose(alertId, "已介入核实");
        assertEquals(AlertRecord.STATUS_PROCESSING, alertService.page(q).getRecords().get(0).getDisposeStatus());

        alertService.close(alertId, "已整改完成");
        AlertRecord closed = alertService.page(q).getRecords().get(0);
        assertEquals(AlertRecord.STATUS_CLOSED, closed.getDisposeStatus());
        assertNotNull(closed.getCloseTime());

        AlertStatsVO stats = alertService.stats();
        assertTrue(stats.getTotal() >= 1);
        assertTrue(stats.getClosed() >= 1);
        assertTrue(stats.getClosureRate() > 0);
    }

    @Test
    void compliance_check_should_flag_expiring_and_raise_alert() {
        // 一条已过期、一条即将到期的已确权档案
        PropertyArchive expired = new PropertyArchive();
        expired.setAssetId("CHK-EXPIRED");
        expired.setAssetName("已过期权益表");
        expired.setConfirmStatus("已确权");
        expired.setValidDate(LocalDateTime.now().minusDays(1));
        archiveService.create(expired);

        PropertyArchive soon = new PropertyArchive();
        soon.setAssetId("CHK-SOON");
        soon.setAssetName("即将到期权益表");
        soon.setConfirmStatus("已确权");
        soon.setValidDate(LocalDateTime.now().plusDays(10));
        archiveService.create(soon);

        int issues = complianceService.runExpiryCheck(30);
        assertTrue(issues >= 2, "应识别出至少2条到期风险");

        // 巡检应联动生成预警(紧急/重要)
        AlertRecordQuery aq = new AlertRecordQuery();
        aq.setAssetId("CHK-EXPIRED");
        PageResult<AlertRecord> alerts = alertService.page(aq);
        assertTrue(alerts.getTotal() >= 1);
        assertEquals(AlertRecord.LEVEL_URGENT, alerts.getRecords().get(0).getAlertLevel(), "已过期应为紧急级");
    }
}
