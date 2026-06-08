package com.csg.prm.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.ledger.dto.PropertyChangeRecordQuery;
import com.csg.prm.ledger.entity.PropertyChangeRecord;
import com.csg.prm.ledger.mapper.PropertyChangeRecordMapper;
import com.csg.prm.ledger.service.PropertyChangeRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class PropertyChangeRecordServiceImpl implements PropertyChangeRecordService {

    private final PropertyChangeRecordMapper mapper;

    public PropertyChangeRecordServiceImpl(PropertyChangeRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String record(String assetId, String changeType, String fieldName,
                         String beforeValue, String afterValue, String reason,
                         String sourceFlow, String sourceTicket) {
        PropertyChangeRecord r = new PropertyChangeRecord();
        r.setAssetId(assetId);
        r.setChangeType(changeType);
        r.setFieldName(fieldName);
        r.setBeforeValue(beforeValue);
        r.setAfterValue(afterValue);
        r.setChangeReason(reason);
        r.setSourceFlow(sourceFlow);
        r.setSourceTicket(sourceTicket);
        r.setOperatorId(UserContextHolder.get().getUserId());
        r.setChangeTime(LocalDateTime.now());
        // TODO: 调用区块链存证服务取回执(SM3)写入 chainHash
        mapper.insert(r);
        return r.getChangeId();
    }

    @Override
    public PageResult<PropertyChangeRecord> page(PropertyChangeRecordQuery query) {
        LambdaQueryWrapper<PropertyChangeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(query.getAssetId()), PropertyChangeRecord::getAssetId, query.getAssetId())
                .eq(StringUtils.hasText(query.getChangeType()), PropertyChangeRecord::getChangeType, query.getChangeType())
                .eq(StringUtils.hasText(query.getOperatorId()), PropertyChangeRecord::getOperatorId, query.getOperatorId())
                .eq(StringUtils.hasText(query.getSourceFlow()), PropertyChangeRecord::getSourceFlow, query.getSourceFlow())
                .ge(StringUtils.hasText(query.getChangeTimeStart()), PropertyChangeRecord::getChangeTime, query.getChangeTimeStart())
                .le(StringUtils.hasText(query.getChangeTimeEnd()), PropertyChangeRecord::getChangeTime, query.getChangeTimeEnd())
                .orderByDesc(PropertyChangeRecord::getChangeTime);
        IPage<PropertyChangeRecord> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }
}
