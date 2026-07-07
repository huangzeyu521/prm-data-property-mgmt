package com.csg.prm.ledger.monitor;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.ledger.monitor.dto.AlertRecordQuery;
import com.csg.prm.ledger.monitor.dto.LinkageResult;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.entity.MonitorRule;
import com.csg.prm.ledger.monitor.gateway.AuthSuspendGateway;
import com.csg.prm.ledger.monitor.gateway.ReConfirmGateway;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
import com.csg.prm.ledger.monitor.service.MonitorRuleService;
import com.csg.prm.ledger.monitor.service.RightsLinkageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 监测规则联动熔断集成测试:违规上报 -> 生成紧急预警 + 按规则联动暂停授权。
 * 用 @Primary 录制桩替换授权熔断网关,验证联动是否被正确触发。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(RightsLinkageTest.RecordingConfig.class)
class RightsLinkageTest {

    /** 录制型授权熔断网关:捕获联动调用,固定返回 3 条被暂停证书 */
    static class RecordingGateway implements AuthSuspendGateway {
        final List<String> calls = new ArrayList<>();
        String lastAsset;
        String lastViolation;
        String lastAlertId;

        void reset() {
            calls.clear();
            lastAsset = null;
            lastViolation = null;
            lastAlertId = null;
        }

        @Override
        public int suspendByAsset(String assetId, String reason, String sourceAlertId, String violationType) {
            calls.add(assetId);
            lastAsset = assetId;
            lastViolation = violationType;
            lastAlertId = sourceAlertId;
            return 3;
        }
    }

    /** 录制型重确权派生网关:捕获派生调用,固定返回工单ID */
    static class RecordingReConfirmGateway implements ReConfirmGateway {
        final List<String> calls = new ArrayList<>();
        String lastAsset;
        String lastTriggerReason;

        void reset() {
            calls.clear();
            lastAsset = null;
            lastTriggerReason = null;
        }

        @Override
        public String trigger(String assetId, String assetName, String rightType, String reason, String sourceRef) {
            calls.add(assetId);
            lastAsset = assetId;
            lastTriggerReason = reason;
            return "RC-MOCK-1";
        }
    }

    @TestConfiguration
    static class RecordingConfig {
        @Bean
        @Primary
        RecordingGateway recordingGateway() {
            return new RecordingGateway();
        }

        @Bean
        @Primary
        RecordingReConfirmGateway recordingReConfirmGateway() {
            return new RecordingReConfirmGateway();
        }
    }

    @Autowired
    private RightsLinkageService linkageService;
    @Autowired
    private MonitorRuleService ruleService;
    @Autowired
    private AlertRecordService alertService;
    @Autowired
    private RecordingGateway gateway;
    @Autowired
    private RecordingReConfirmGateway reConfirmGateway;

    @BeforeEach
    void setUp() {
        gateway.reset();
        reConfirmGateway.reset();
    }

    private String circuitBreakRule(boolean cb) {
        MonitorRule rule = new MonitorRule();
        rule.setRuleName("越权调用熔断规则-" + cb);
        rule.setRuleCategory("调用异常");
        rule.setCircuitBreak(cb);
        String id = ruleService.create(rule);
        ruleService.enable(id);
        return id;
    }

    @Test
    void violation_with_circuit_break_rule_should_suspend_and_alert() {
        String ruleId = circuitBreakRule(true);
        LinkageResult r = linkageService.onViolation("DA-VIO-1", ruleId, "越权调用", "检测到跨域越权访问");

        assertTrue(r.isCircuitBroken(), "熔断规则应触发联动");
        assertEquals(3, r.getSuspendedCount(), "应回传被暂停证书数");
        assertEquals("DA-VIO-1", gateway.lastAsset);
        assertEquals("越权调用", gateway.lastViolation);
        assertEquals(r.getAlertId(), gateway.lastAlertId, "熔断应携带来源预警ID");

        // 同时生成紧急预警留痕
        assertNotNull(r.getAlertId());
        AlertRecordQuery q = new AlertRecordQuery();
        q.setAssetId("DA-VIO-1");
        PageResult<AlertRecord> alerts = alertService.page(q);
        assertTrue(alerts.getTotal() >= 1);
        assertEquals(AlertRecord.LEVEL_URGENT, alerts.getRecords().get(0).getAlertLevel());
    }

    @Test
    void violation_with_non_break_rule_should_only_alert() {
        String ruleId = circuitBreakRule(false);
        LinkageResult r = linkageService.onViolation("DA-VIO-2", ruleId, "违规使用", "外发未脱敏数据");

        assertFalse(r.isCircuitBroken(), "非熔断规则不应联动暂停");
        assertEquals(0, r.getSuspendedCount());
        assertTrue(gateway.calls.isEmpty(), "网关不应被调用");
        assertNotNull(r.getAlertId(), "仍应留痕预警");
    }

    @Test
    void violation_without_rule_defaults_to_circuit_break() {
        LinkageResult r = linkageService.onViolation("DA-VIO-3", null, "超范围", "超出确权边界使用");

        assertTrue(r.isCircuitBroken(), "无规则的显式违规上报默认熔断");
        assertEquals(1, gateway.calls.size());
        assertEquals("DA-VIO-3", gateway.lastAsset);
    }

    @Test
    void re_confirm_trigger_should_derive_work_order_and_alert() {
        LinkageResult r = linkageService.triggerReConfirm("DA-RC-1", "客户用电信息表",
                "持有权", "来源变更", "上游系统更换导致来源变更");

        // 派生重确权工单
        assertEquals("RC-MOCK-1", r.getReConfirmId(), "应回传派生的重确权工单ID");
        assertEquals(1, reConfirmGateway.calls.size());
        assertEquals("DA-RC-1", reConfirmGateway.lastAsset);
        assertTrue(reConfirmGateway.lastTriggerReason.contains("来源变更"));

        // 重确权场景不熔断,但生成重要级预警留痕
        assertFalse(r.isCircuitBroken());
        assertTrue(gateway.calls.isEmpty(), "重确权不应触发授权熔断");
        AlertRecordQuery q = new AlertRecordQuery();
        q.setAssetId("DA-RC-1");
        PageResult<AlertRecord> alerts = alertService.page(q);
        assertTrue(alerts.getTotal() >= 1);
        assertEquals(AlertRecord.LEVEL_IMPORTANT, alerts.getRecords().get(0).getAlertLevel());
    }
}
