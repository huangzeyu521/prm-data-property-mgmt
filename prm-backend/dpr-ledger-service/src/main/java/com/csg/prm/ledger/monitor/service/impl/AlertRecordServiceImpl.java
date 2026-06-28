package com.csg.prm.ledger.monitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.ledger.monitor.dto.AlertRecordQuery;
import com.csg.prm.ledger.monitor.dto.AlertStatsVO;
import com.csg.prm.ledger.monitor.entity.AlertRecord;
import com.csg.prm.ledger.monitor.mapper.AlertRecordMapper;
import com.csg.prm.ledger.monitor.service.AlertNotificationService;
import com.csg.prm.ledger.monitor.service.AlertRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AlertRecordServiceImpl implements AlertRecordService {

    private final AlertRecordMapper mapper;
    private final AlertNotificationService notificationService;

    public AlertRecordServiceImpl(AlertRecordMapper mapper, AlertNotificationService notificationService) {
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public String raise(String ruleId, String source, String assetId, String alertLevel,
                        String triggerCond, String abnormalDesc) {
        AlertRecord r = new AlertRecord();
        r.setRuleId(ruleId);
        r.setSource(source);
        r.setAssetId(assetId);
        r.setAlertLevel(StringUtils.hasText(alertLevel) ? alertLevel : AlertRecord.LEVEL_NORMAL);
        r.setTriggerCond(triggerCond);
        r.setAbnormalDesc(abnormalDesc);
        r.setAlertTime(LocalDateTime.now());
        r.setDisposeStatus(AlertRecord.STATUS_PENDING);
        mapper.insert(r);
        // 按命中规则的通知对象+通知方式,生成定向通知并推送(站内信落库+多渠道桩)
        notificationService.pushForAlert(r, ruleId);
        return r.getAlertId();
    }

    @Override
    public boolean existsOpen(String assetId, String triggerCond) {
        LambdaQueryWrapper<AlertRecord> w = new LambdaQueryWrapper<>();
        w.eq(AlertRecord::getAssetId, assetId)
                .eq(AlertRecord::getTriggerCond, triggerCond)
                .ne(AlertRecord::getDisposeStatus, AlertRecord.STATUS_CLOSED);
        return mapper.selectCount(w) > 0;
    }

    @Override
    @Transactional
    public void dispose(String alertId, String feedback) {
        AlertRecord r = require(alertId);
        if (AlertRecord.STATUS_CLOSED.equals(r.getDisposeStatus())) {
            throw new BusinessException("预警已关闭,不能再处置");
        }
        AlertRecord upd = new AlertRecord();
        upd.setAlertId(alertId);
        upd.setDisposeStatus(AlertRecord.STATUS_PROCESSING);
        upd.setDisposeFeedback(feedback);
        upd.setResponderId(UserContextHolder.get().getUserId());
        mapper.updateById(upd);
    }

    @Override
    @Transactional
    public void close(String alertId, String feedback) {
        AlertRecord r = require(alertId);
        AlertRecord upd = new AlertRecord();
        upd.setAlertId(alertId);
        upd.setDisposeStatus(AlertRecord.STATUS_CLOSED);
        upd.setDisposeFeedback(StringUtils.hasText(feedback) ? feedback : r.getDisposeFeedback());
        upd.setResponderId(UserContextHolder.get().getUserId());
        upd.setCloseTime(LocalDateTime.now());
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AlertRecord> page(AlertRecordQuery query) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getAlertLevel()), AlertRecord::getAlertLevel, query.getAlertLevel())
                .eq(StringUtils.hasText(query.getDisposeStatus()), AlertRecord::getDisposeStatus, query.getDisposeStatus())
                .eq(StringUtils.hasText(query.getAssetId()), AlertRecord::getAssetId, query.getAssetId())
                .orderByDesc(AlertRecord::getAlertTime);
        IPage<AlertRecord> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public AlertStatsVO stats() {
        List<AlertRecord> all = mapper.selectList(null);
        long total = all.size();
        long pending = all.stream().filter(a -> AlertRecord.STATUS_PENDING.equals(a.getDisposeStatus())).count();
        long processing = all.stream().filter(a -> AlertRecord.STATUS_PROCESSING.equals(a.getDisposeStatus())).count();
        long closed = all.stream().filter(a -> AlertRecord.STATUS_CLOSED.equals(a.getDisposeStatus())).count();
        Map<String, Long> levelDist = all.stream()
                .collect(Collectors.groupingBy(a -> StringUtils.hasText(a.getAlertLevel()) ? a.getAlertLevel() : "未分级",
                        Collectors.counting()));
        double closureRate = total == 0 ? 0d
                : BigDecimal.valueOf(closed * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();

        AlertStatsVO vo = new AlertStatsVO();
        vo.setTotal(total);
        vo.setPending(pending);
        vo.setProcessing(processing);
        vo.setClosed(closed);
        vo.setClosureRate(closureRate);
        vo.setLevelDistribution(levelDist);
        return vo;
    }

    private AlertRecord require(String alertId) {
        AlertRecord r = mapper.selectById(alertId);
        if (r == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "预警记录不存在");
        }
        return r;
    }
}
