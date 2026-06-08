package com.csg.prm.ledger.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.ledger.monitor.dto.AlertNotificationQuery;
import com.csg.prm.ledger.monitor.entity.AlertNotification;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.entity.MonitorRule;
import com.csg.prm.ledger.monitor.mapper.AlertNotificationMapper;
import com.csg.prm.ledger.monitor.mapper.AlertRecordMapper;
import com.csg.prm.ledger.monitor.mapper.MonitorRuleMapper;
import com.csg.prm.ledger.monitor.service.AlertNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 预警通知/推送实现。站内信渠道=落库通知本身(铃铛即收件箱);
 * 邮件/短信/eLink 渠道目前为日志桩(生产对接统一消息中心)。
 */
@Service
public class AlertNotificationServiceImpl implements AlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AlertNotificationServiceImpl.class);
    private static final String DEFAULT_RECIPIENT = "合规管控小组";
    private static final String DEFAULT_CHANNEL = "站内信";

    private final AlertNotificationMapper mapper;
    private final MonitorRuleMapper ruleMapper;
    private final AlertRecordMapper alertMapper;

    public AlertNotificationServiceImpl(AlertNotificationMapper mapper, MonitorRuleMapper ruleMapper,
                                        AlertRecordMapper alertMapper) {
        this.mapper = mapper;
        this.ruleMapper = ruleMapper;
        this.alertMapper = alertMapper;
    }

    @Override
    @Transactional
    public void pushForAlert(AlertRecord alert, String ruleId) {
        if (alert == null) {
            return;
        }
        String recipient = DEFAULT_RECIPIENT;
        String channel = DEFAULT_CHANNEL;
        if (StringUtils.hasText(ruleId)) {
            MonitorRule rule = ruleMapper.selectById(ruleId);
            if (rule != null) {
                if (StringUtils.hasText(rule.getNotifyTarget())) {
                    recipient = rule.getNotifyTarget();
                }
                if (StringUtils.hasText(rule.getNotifyChannel())) {
                    channel = rule.getNotifyChannel();
                }
            }
        }
        send(alert, recipient, channel);
    }

    private void send(AlertRecord alert, String recipient, String channel) {
        AlertNotification n = new AlertNotification();
        n.setAlertId(alert.getAlertId());
        n.setAssetId(alert.getAssetId());
        n.setRecipient(recipient);
        n.setChannel(channel);
        n.setAlertLevel(alert.getAlertLevel());
        n.setTitle("[" + alert.getAlertLevel() + "]" + safe(alert.getTriggerCond()));
        n.setContent(safe(alert.getAbnormalDesc()));
        n.setReadStatus(AlertNotification.READ_UNREAD);
        n.setPushTime(LocalDateTime.now());
        mapper.insert(n);
        // 站内信=落库即送达(铃铛收件箱);其余渠道目前为桩,生产对接 eLink/邮件/短信网关
        for (String ch : channel.split(",")) {
            if (!DEFAULT_CHANNEL.equals(ch.trim())) {
                log.info("[预警推送-{}桩] 收件方={} 预警={} 资产={} -> 待生产对接统一消息中心",
                        ch.trim(), recipient, alert.getAlertId(), alert.getAssetId());
            }
        }
    }

    @Override
    @Transactional
    public int repush(String alertId) {
        AlertRecord alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "预警不存在:" + alertId);
        }
        pushForAlert(alert, alert.getRuleId());
        return 1;
    }

    @Override
    public PageResult<AlertNotification> page(AlertNotificationQuery query) {
        LambdaQueryWrapper<AlertNotification> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(query.getRecipient()), AlertNotification::getRecipient, query.getRecipient())
                .eq(StringUtils.hasText(query.getReadStatus()), AlertNotification::getReadStatus, query.getReadStatus())
                .orderByDesc(AlertNotification::getPushTime);
        IPage<AlertNotification> page = mapper.selectPage(query.toPage(), w);
        return PageResult.of(page);
    }

    @Override
    public long unreadCount(String recipient) {
        LambdaQueryWrapper<AlertNotification> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(recipient), AlertNotification::getRecipient, recipient)
                .eq(AlertNotification::getReadStatus, AlertNotification.READ_UNREAD);
        return mapper.selectCount(w);
    }

    @Override
    @Transactional
    public void markRead(String notifyId) {
        AlertNotification upd = new AlertNotification();
        upd.setNotifyId(notifyId);
        upd.setReadStatus(AlertNotification.READ_READ);
        upd.setReadTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public int markAllRead(String recipient) {
        AlertNotification upd = new AlertNotification();
        upd.setReadStatus(AlertNotification.READ_READ);
        upd.setReadTime(LocalDateTime.now());
        LambdaQueryWrapper<AlertNotification> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(recipient), AlertNotification::getRecipient, recipient)
                .eq(AlertNotification::getReadStatus, AlertNotification.READ_UNREAD);
        return mapper.update(upd, w);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
