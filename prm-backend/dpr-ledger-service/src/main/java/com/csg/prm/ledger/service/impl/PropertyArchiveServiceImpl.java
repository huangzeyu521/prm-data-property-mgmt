package com.csg.prm.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.writeback.RightsEvent;
import com.csg.prm.ledger.dto.PropertyArchiveQuery;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.PropertyArchiveMapper;
import com.csg.prm.ledger.service.PropertyArchiveService;
import com.csg.prm.ledger.service.PropertyChangeRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Service
public class PropertyArchiveServiceImpl implements PropertyArchiveService {

    private final PropertyArchiveMapper mapper;
    private final PropertyChangeRecordService changeRecordService;

    public PropertyArchiveServiceImpl(PropertyArchiveMapper mapper,
                                      PropertyChangeRecordService changeRecordService) {
        this.mapper = mapper;
        this.changeRecordService = changeRecordService;
    }

    @Override
    @Transactional
    public String create(PropertyArchive archive) {
        validate(archive);
        if (!StringUtils.hasText(archive.getConfirmStatus())) {
            archive.setConfirmStatus("未确权");
        }
        mapper.insert(archive);
        return archive.getArchiveId();
    }

    @Override
    @Transactional
    public void applyRightsEvent(RightsEvent e) {
        if (e == null || !StringUtils.hasText(e.getAssetId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产ID不能为空");
        }
        String src = RightsEvent.TYPE_AUTHORIZED.equals(e.getEventType()) ? "授权流程" : "确权流程";
        PropertyArchive cur = mapper.selectOne(new LambdaQueryWrapper<PropertyArchive>()
                .eq(PropertyArchive::getAssetId, e.getAssetId()).last("LIMIT 1"));
        if (cur == null) {
            // 一资产一档:确权制卡时若无档案则建档
            PropertyArchive a = new PropertyArchive();
            a.setAssetId(e.getAssetId());
            a.setAssetName(StringUtils.hasText(e.getAssetName()) ? e.getAssetName() : e.getAssetId());
            a.setRightType(e.getRightType());
            a.setRightSubject(e.getRightHolder());
            a.setConfirmStatus(StringUtils.hasText(e.getConfirmStatus()) ? e.getConfirmStatus() : "未确权");
            a.setAuthStatus(StringUtils.hasText(e.getAuthStatus()) ? e.getAuthStatus() : "未授权");
            a.setEquityCardId(e.getEquityCardId());
            mapper.insert(a);
            changeRecordService.record(e.getAssetId(), e.getEventType(), "建档", null,
                    a.getConfirmStatus() + "/" + a.getAuthStatus(), e.getReason(), src, e.getSourceTicket());
            return;
        }
        PropertyArchive upd = new PropertyArchive();
        upd.setArchiveId(cur.getArchiveId());
        if (StringUtils.hasText(e.getConfirmStatus())) {
            upd.setConfirmStatus(e.getConfirmStatus());
        }
        if (StringUtils.hasText(e.getAuthStatus())) {
            upd.setAuthStatus(e.getAuthStatus());
        }
        if (StringUtils.hasText(e.getEquityCardId())) {
            upd.setEquityCardId(e.getEquityCardId());
        }
        if (StringUtils.hasText(e.getRightType()) && !StringUtils.hasText(cur.getRightType())) {
            upd.setRightType(e.getRightType());
        }
        mapper.updateById(upd);
        if (StringUtils.hasText(e.getConfirmStatus())) {
            changeRecordService.record(e.getAssetId(), e.getEventType(), "确权状态",
                    cur.getConfirmStatus(), e.getConfirmStatus(), e.getReason(), src, e.getSourceTicket());
        }
        if (StringUtils.hasText(e.getAuthStatus())) {
            changeRecordService.record(e.getAssetId(), e.getEventType(), "授权状态",
                    cur.getAuthStatus(), e.getAuthStatus(), e.getReason(), src, e.getSourceTicket());
        }
    }

    @Override
    @Transactional
    public void update(PropertyArchive archive) {
        if (!StringUtils.hasText(archive.getArchiveId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "档案ID不能为空");
        }
        PropertyArchive exist = mapper.selectById(archive.getArchiveId());
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "产权档案不存在");
        }
        validate(archive);
        mapper.updateById(archive);
        // 关键产权字段变更自动留痕(防篡改证据链)
        recordIfChanged(exist.getAssetId(), "确权状态", exist.getConfirmStatus(), archive.getConfirmStatus());
        recordIfChanged(exist.getAssetId(), "产权类型", exist.getRightType(), archive.getRightType());
        recordIfChanged(exist.getAssetId(), "授权状态", exist.getAuthStatus(), archive.getAuthStatus());
    }

    private void recordIfChanged(String assetId, String fieldName, String before, String after) {
        if (!Objects.equals(before, after) && (StringUtils.hasText(before) || StringUtils.hasText(after))) {
            changeRecordService.record(assetId, "档案变更", fieldName, before, after,
                    "产权档案修改", "产权信息台账", null);
        }
    }

    @Override
    @Transactional
    public void delete(String archiveId) {
        PropertyArchive exist = mapper.selectById(archiveId);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "产权档案不存在");
        }
        // 业务约束:已关联权益卡片/授权的档案不可直接删除(对应界面"请先解除关联"提示)
        if (StringUtils.hasText(exist.getEquityCardId())) {
            throw new BizException("该档案已关联权益卡片或授权协议,请先解除关联");
        }
        mapper.deleteById(archiveId);
    }

    @Override
    public PropertyArchive getById(String archiveId) {
        PropertyArchive archive = mapper.selectById(archiveId);
        if (archive == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "产权档案不存在");
        }
        return archive;
    }

    @Override
    public PageResult<PropertyArchive> page(PropertyArchiveQuery query) {
        LambdaQueryWrapper<PropertyArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getAssetName()), PropertyArchive::getAssetName, query.getAssetName())
                .eq(StringUtils.hasText(query.getRightType()), PropertyArchive::getRightType, query.getRightType())
                .eq(StringUtils.hasText(query.getRespDept()), PropertyArchive::getRespDept, query.getRespDept())
                .eq(StringUtils.hasText(query.getConfirmStatus()), PropertyArchive::getConfirmStatus, query.getConfirmStatus())
                .orderByDesc(PropertyArchive::getCreateTime);
        IPage<PropertyArchive> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    private void validate(PropertyArchive archive) {
        if (Objects.isNull(archive)) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(archive.getAssetId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "关联资产ID不能为空");
        }
        if (!StringUtils.hasText(archive.getAssetName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产名称不能为空");
        }
    }
}
