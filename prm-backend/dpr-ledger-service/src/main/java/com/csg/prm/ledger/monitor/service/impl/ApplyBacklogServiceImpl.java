package com.csg.prm.ledger.monitor.service.impl;

import com.csg.prm.ledger.aggregate.AuthQueryGateway;
import com.csg.prm.ledger.aggregate.ConfirmQueryGateway;
import com.csg.prm.ledger.aggregate.DomainRecord;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
import com.csg.prm.ledger.monitor.service.ApplyBacklogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 申请/审核积压监测实现。复用台账侧聚合网关 {@link ConfirmQueryGateway}/{@link AuthQueryGateway}
 * 拉取处于审批链中的确权/授权申请,识别 createTime 早于阈值的"超时未审/审核积压"并生成预警。
 * 同资产+同条件已有未关闭预警则跳过(去重),供定时器周期调用。
 */
@Service
public class ApplyBacklogServiceImpl implements ApplyBacklogService {

    private static final Logger log = LoggerFactory.getLogger(ApplyBacklogServiceImpl.class);
    private static final String SOURCE = "申请审核监测";
    private static final int DEFAULT_DAYS = 7;

    private final ConfirmQueryGateway confirmGateway;
    private final AuthQueryGateway authGateway;
    private final AlertRecordService alertService;

    public ApplyBacklogServiceImpl(ConfirmQueryGateway confirmGateway, AuthQueryGateway authGateway,
                                   AlertRecordService alertService) {
        this.confirmGateway = confirmGateway;
        this.authGateway = authGateway;
        this.alertService = alertService;
    }

    @Override
    public int scanBacklog(int days) {
        int threshold = days <= 0 ? DEFAULT_DAYS : days;
        LocalDateTime deadline = LocalDateTime.now().minusDays(threshold);
        int hit = 0;
        hit += scan(confirmGateway.pending(), "确权", threshold, deadline);
        hit += scan(authGateway.pending(), "授权", threshold, deadline);
        return hit;
    }

    private int scan(List<DomainRecord> pending, String domain, int threshold, LocalDateTime deadline) {
        int hit = 0;
        for (DomainRecord r : pending) {
            LocalDateTime created = parse(r.getTime());
            if (created == null || !created.isBefore(deadline)) {
                continue;
            }
            String cond = domain + "申请审核积压>" + threshold + "天";
            if (alertService.existsOpen(r.getAssetId(), cond)) {
                continue;
            }
            String desc = domain + "申请 " + r.getNo() + " 在'" + r.getStatus() + "'环节停留超 " + threshold + " 天,疑似审核积压";
            alertService.raise(null, SOURCE, r.getAssetId(), AlertRecord.LEVEL_IMPORTANT, cond, desc);
            hit++;
        }
        return hit;
    }

    private LocalDateTime parse(String t) {
        if (t == null || t.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(t);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(t.length() >= 19 ? t.substring(0, 19) : t);
            } catch (DateTimeParseException ex) {
                log.warn("[申请审核积压] 申请时间解析失败: {}", t);
                return null;
            }
        }
    }
}
