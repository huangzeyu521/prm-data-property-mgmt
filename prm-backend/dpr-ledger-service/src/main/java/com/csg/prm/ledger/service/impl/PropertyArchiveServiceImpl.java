package com.csg.prm.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.writeback.RightsEvent;
import com.csg.prm.ledger.dto.PropertyArchiveQuery;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.mapper.PropertyArchiveMapper;
import com.csg.prm.ledger.service.PropertyArchiveService;
import com.csg.prm.ledger.service.PropertyChangeRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Service
public class PropertyArchiveServiceImpl implements PropertyArchiveService {

    private static final String UNCONFIRMED = "未确权";
    private static final String CONFIRMED = "已确权";
    private static final String PARTIAL = "部分确权";

    private final PropertyArchiveMapper mapper;
    private final PropertyChangeRecordService changeRecordService;

    public PropertyArchiveServiceImpl(PropertyArchiveMapper mapper,
                                      PropertyChangeRecordService changeRecordService) {
        this.mapper = mapper;
        this.changeRecordService = changeRecordService;
    }

    // ===== 三权分置:产权类型多值 + 按权确权状态(顿号/竖线等分隔,去重保序) =====
    private static List<String> splitRights(String joined) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(joined)) return out;
        for (String p : joined.split("[、,，;；/|]")) {
            String t = p.trim();
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }

    /** 由产权类型多值串构建"各权未确权"明细 */
    private static String buildDetail(String rightTypeJoined) {
        List<String> rs = splitRights(rightTypeJoined);
        StringBuilder sb = new StringBuilder();
        for (String r : rs) {
            if (sb.length() > 0) sb.append(";");
            sb.append(r).append(":").append(UNCONFIRMED);
        }
        return sb.toString();
    }

    private static LinkedHashMap<String, String> parseDetail(String detail) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        if (!StringUtils.hasText(detail)) return m;
        for (String seg : detail.split(";")) {
            String s = seg.trim();
            if (s.isEmpty()) continue;
            int i = s.lastIndexOf(':');
            if (i < 0) m.put(s, UNCONFIRMED);
            else m.put(s.substring(0, i).trim(), s.substring(i + 1).trim());
        }
        return m;
    }

    private static String detailString(LinkedHashMap<String, String> m) {
        StringBuilder sb = new StringBuilder();
        for (var e : m.entrySet()) {
            if (sb.length() > 0) sb.append(";");
            sb.append(e.getKey()).append(":").append(e.getValue());
        }
        return sb.toString();
    }

    /** 把 rightTypeJoined 涉及的权利在明细中置"已确权"(明细缺则补) */
    private static String markConfirmed(String detail, String rightTypeJoined) {
        LinkedHashMap<String, String> m = parseDetail(detail);
        for (String r : splitRights(rightTypeJoined)) m.put(r, CONFIRMED);
        return detailString(m);
    }

    /** 聚合确权状态:空/全未确权=未确权;全已确权=已确权;混合=部分确权 */
    private static String aggregate(String detail) {
        LinkedHashMap<String, String> m = parseDetail(detail);
        if (m.isEmpty()) return UNCONFIRMED;
        int conf = 0;
        for (String v : m.values()) if (CONFIRMED.equals(v)) conf++;
        if (conf == 0) return UNCONFIRMED;
        return conf == m.size() ? CONFIRMED : PARTIAL;
    }

    @Override
    @Transactional
    public String create(PropertyArchive archive) {
        validate(archive);
        // 多权登记:初始化各权"未确权"明细,聚合确权状态由明细派生(登记仅申报,确权状态由确权流程驱动)
        if (!StringUtils.hasText(archive.getConfirmDetail()) && StringUtils.hasText(archive.getRightType())) {
            archive.setConfirmDetail(buildDetail(archive.getRightType()));
        }
        if (!StringUtils.hasText(archive.getConfirmStatus())) {
            archive.setConfirmStatus(aggregate(archive.getConfirmDetail()));
        }
        mapper.insert(archive);
        return archive.getArchiveId();
    }

    @Override
    @Transactional
    public void applyRightsEvent(RightsEvent e) {
        if (e == null || !StringUtils.hasText(e.getAssetId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "资产ID不能为空");
        }
        String src = RightsEvent.TYPE_AUTHORIZED.equals(e.getEventType()) ? "授权流程" : "确权流程";
        PropertyArchive cur = mapper.selectOne(new LambdaQueryWrapper<PropertyArchive>()
                .eq(PropertyArchive::getAssetId, e.getAssetId()).last("LIMIT 1"));
        boolean isConfirm = RightsEvent.TYPE_CONFIRMED.equals(e.getEventType());
        if (cur == null) {
            // 一资产一档:确权制卡时若无档案则建档
            PropertyArchive a = new PropertyArchive();
            a.setAssetId(e.getAssetId());
            a.setAssetName(StringUtils.hasText(e.getAssetName()) ? e.getAssetName() : e.getAssetId());
            a.setRightType(e.getRightType());
            a.setRightSubject(e.getRightHolder());
            // 确权事件建档:事件携带的权利(可能多权)标记为已确权,其余未涉及;聚合状态据明细派生
            if (isConfirm && StringUtils.hasText(e.getRightType())) {
                a.setConfirmDetail(markConfirmed(buildDetail(e.getRightType()), e.getRightType()));
                a.setConfirmStatus(aggregate(a.getConfirmDetail()));
            } else {
                a.setConfirmDetail(buildDetail(e.getRightType()));
                a.setConfirmStatus(StringUtils.hasText(e.getConfirmStatus()) ? e.getConfirmStatus() : UNCONFIRMED);
            }
            a.setAuthStatus(StringUtils.hasText(e.getAuthStatus()) ? e.getAuthStatus() : "未授权");
            a.setEquityCardId(e.getEquityCardId());
            mapper.insert(a);
            changeRecordService.record(e.getAssetId(), e.getEventType(), "建档", null,
                    a.getConfirmStatus() + "/" + a.getAuthStatus(), e.getReason(), src, e.getSourceTicket());
            return;
        }
        PropertyArchive upd = new PropertyArchive();
        upd.setArchiveId(cur.getArchiveId());
        String newConfirmStatus = null;
        // 确权回写:把事件涉及的权利在按权明细中置已确权,聚合状态重算(支持"3权登记、只确2权=部分确权")
        if (isConfirm && StringUtils.hasText(e.getRightType())) {
            String baseDetail = StringUtils.hasText(cur.getConfirmDetail())
                    ? cur.getConfirmDetail() : buildDetail(cur.getRightType());
            String newDetail = markConfirmed(baseDetail, e.getRightType());
            upd.setConfirmDetail(newDetail);
            newConfirmStatus = aggregate(newDetail);
            upd.setConfirmStatus(newConfirmStatus);
        } else if (StringUtils.hasText(e.getConfirmStatus())) {
            newConfirmStatus = e.getConfirmStatus();
            upd.setConfirmStatus(newConfirmStatus);
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
        if (newConfirmStatus != null) {
            changeRecordService.record(e.getAssetId(), e.getEventType(), "确权状态",
                    cur.getConfirmStatus(), newConfirmStatus, e.getReason(), src, e.getSourceTicket());
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
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "档案ID不能为空");
        }
        PropertyArchive exist = mapper.selectById(archive.getArchiveId());
        if (exist == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "产权档案不存在");
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
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "产权档案不存在");
        }
        // 业务约束:已关联权益卡片/授权的档案不可直接删除(对应界面"请先解除关联"提示)
        if (StringUtils.hasText(exist.getEquityCardId())) {
            throw new BusinessException("该档案已关联权益卡片或授权协议,请先解除关联");
        }
        mapper.deleteById(archiveId);
    }

    @Override
    public PropertyArchive getById(String archiveId) {
        PropertyArchive archive = mapper.selectById(archiveId);
        if (archive == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "产权档案不存在");
        }
        return archive;
    }

    @Override
    public PageResult<PropertyArchive> page(PropertyArchiveQuery query) {
        LambdaQueryWrapper<PropertyArchive> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getAssetName()), PropertyArchive::getAssetName, query.getAssetName())
                // 产权类型多值串(顿号拼接),按"含此权"过滤
                .like(StringUtils.hasText(query.getRightType()), PropertyArchive::getRightType, query.getRightType())
                .eq(StringUtils.hasText(query.getRespDept()), PropertyArchive::getRespDept, query.getRespDept())
                .eq(StringUtils.hasText(query.getConfirmStatus()), PropertyArchive::getConfirmStatus, query.getConfirmStatus())
                .orderByDesc(PropertyArchive::getCreateTime);
        IPage<PropertyArchive> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    private void validate(PropertyArchive archive) {
        if (Objects.isNull(archive)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(archive.getAssetId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "关联资产ID不能为空");
        }
        if (!StringUtils.hasText(archive.getAssetName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "资产名称不能为空");
        }
    }
}
