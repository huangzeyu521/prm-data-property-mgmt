package com.csg.prm.ledger.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.writeback.RightsEvent;
import com.csg.prm.ledger.entity.PropertyArchive;
import com.csg.prm.ledger.entity.PropertyChangeRecord;
import com.csg.prm.ledger.mapper.PropertyArchiveMapper;
import com.csg.prm.ledger.mapper.PropertyChangeRecordMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-① 产权事件回写测试:确权制卡/授权生效 -> 台账建档/更新状态 + 变更留痕(流程贯通,台账实时一致)。
 */
@SpringBootTest
@ActiveProfiles("test")
class PropertyArchiveWritebackTest {

    @Autowired
    private PropertyArchiveService archiveService;
    @Autowired
    private PropertyArchiveMapper archiveMapper;
    @Autowired
    private PropertyChangeRecordMapper changeMapper;

    private PropertyArchive byAsset(String assetId) {
        return archiveMapper.selectOne(new LambdaQueryWrapper<PropertyArchive>()
                .eq(PropertyArchive::getAssetId, assetId).last("LIMIT 1"));
    }

    private long changes(String assetId) {
        return changeMapper.selectCount(new LambdaQueryWrapper<PropertyChangeRecord>()
                .eq(PropertyChangeRecord::getAssetId, assetId));
    }

    @Test
    void confirmed_event_creates_archive_and_change_record() {
        archiveService.applyRightsEvent(RightsEvent.confirmed(
                "WB-ASSET-1", "回写测试表", "数据持有权", "广东电网", "EC-WB-1", "QQ-WB-1"));
        PropertyArchive a = byAsset("WB-ASSET-1");
        assertNotNull(a, "确权制卡应在台账建档");
        assertEquals("已确权", a.getConfirmStatus());
        assertEquals("EC-WB-1", a.getEquityCardId());
        assertTrue(changes("WB-ASSET-1") >= 1, "应留确权变更记录");
    }

    @Test
    void authorized_event_updates_existing_archive() {
        archiveService.applyRightsEvent(RightsEvent.confirmed(
                "WB-ASSET-2", "回写测试表2", "数据加工使用权", "广东电网", "EC-WB-2", "QQ-WB-2"));
        archiveService.applyRightsEvent(RightsEvent.authorized(
                "WB-ASSET-2", "数据加工使用权", "广州供电局", "SQ-WB-2"));
        PropertyArchive a = byAsset("WB-ASSET-2");
        assertEquals("已确权", a.getConfirmStatus(), "确权状态保持");
        assertEquals("已授权", a.getAuthStatus(), "授权生效应回写授权状态");
        assertTrue(changes("WB-ASSET-2") >= 2, "确权 + 授权各留一条变更");
    }

    @Test
    void authorized_only_creates_archive_with_auth_status() {
        archiveService.applyRightsEvent(RightsEvent.authorized(
                "WB-ASSET-3", "数据加工使用权", "深圳供电局", "SQ-WB-3"));
        PropertyArchive a = byAsset("WB-ASSET-3");
        assertNotNull(a);
        assertEquals("已授权", a.getAuthStatus());
    }
}
