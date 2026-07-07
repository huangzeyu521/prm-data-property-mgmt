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

import java.time.LocalDateTime;

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
                "WB-ASSET-1", "回写测试表", "持有权", "广东电网", "EC-WB-1", "QQ-WB-1", null));
        PropertyArchive a = byAsset("WB-ASSET-1");
        assertNotNull(a, "确权制卡应在台账建档");
        assertEquals("已确权", a.getConfirmStatus());
        assertEquals("EC-WB-1", a.getEquityCardId());
        assertTrue(changes("WB-ASSET-1") >= 1, "应留确权变更记录");
    }

    @Test
    void authorized_event_updates_existing_archive() {
        archiveService.applyRightsEvent(RightsEvent.confirmed(
                "WB-ASSET-2", "回写测试表2", "使用权", "广东电网", "EC-WB-2", "QQ-WB-2", null));
        archiveService.applyRightsEvent(RightsEvent.authorized(
                "WB-ASSET-2", "使用权", "广州供电局", "SQ-WB-2", null));
        PropertyArchive a = byAsset("WB-ASSET-2");
        assertEquals("已确权", a.getConfirmStatus(), "确权状态保持");
        assertEquals("已授权", a.getAuthStatus(), "授权生效应回写授权状态");
        assertTrue(changes("WB-ASSET-2") >= 2, "确权 + 授权各留一条变更");
    }

    @Test
    void authorized_only_creates_archive_with_auth_status() {
        archiveService.applyRightsEvent(RightsEvent.authorized(
                "WB-ASSET-3", "使用权", "深圳供电局", "SQ-WB-3", null));
        PropertyArchive a = byAsset("WB-ASSET-3");
        assertNotNull(a);
        assertEquals("已授权", a.getAuthStatus());
    }

    @Test
    void partial_confirm_yields_partial_then_full_status() {
        // 三权登记一个数据集(各权未确权)
        PropertyArchive reg = new PropertyArchive();
        reg.setAssetId("WB-ASSET-4");
        reg.setAssetName("三权登记表");
        reg.setRightType("持有权、使用权、经营权");
        archiveService.create(reg);
        PropertyArchive a0 = byAsset("WB-ASSET-4");
        assertEquals("未确权", a0.getConfirmStatus(), "三权登记初始应为未确权");
        assertTrue(a0.getConfirmDetail().contains("经营权:未确权"), "应有按权明细");

        // 确权只确了 2/3 权 -> 部分确权
        archiveService.applyRightsEvent(RightsEvent.confirmed(
                "WB-ASSET-4", "三权登记表", "持有权、使用权", "广东电网", "EC-WB-4", "QQ-WB-4a", null));
        PropertyArchive a1 = byAsset("WB-ASSET-4");
        assertEquals("部分确权", a1.getConfirmStatus(), "确2权应聚合为部分确权");
        assertTrue(a1.getConfirmDetail().contains("持有权:已确权"), "持有权应已确权");
        assertTrue(a1.getConfirmDetail().contains("经营权:未确权"), "经营权应仍未确权");

        // 补确第3权 -> 已确权
        archiveService.applyRightsEvent(RightsEvent.confirmed(
                "WB-ASSET-4", "三权登记表", "经营权", "广东电网", "EC-WB-4", "QQ-WB-4b", null));
        PropertyArchive a2 = byAsset("WB-ASSET-4");
        assertEquals("已确权", a2.getConfirmStatus(), "三权全确应聚合为已确权");
    }

    // P0-① 新增:验证 validDate 真正随事件流入台账,不再是永远为空的字段
    // (此前 ComplianceCheckServiceImpl 每10分钟的到期扫描,扫的就是这个从未被写入的字段)。
    @Test
    void confirmed_event_carries_valid_date_into_archive() {
        LocalDateTime due = LocalDateTime.now().plusDays(10);
        archiveService.applyRightsEvent(RightsEvent.confirmed(
                "WB-ASSET-5", "到期测试表", "使用权", "广东电网", "EC-WB-5", "QQ-WB-5", due));
        PropertyArchive a = byAsset("WB-ASSET-5");
        assertNotNull(a.getValidDate(), "确权事件携带到期日时,台账应写入 validDate");
        assertEquals(due.toLocalDate(), a.getValidDate().toLocalDate());
    }

    @Test
    void authorized_event_updates_valid_date_on_renewal() {
        LocalDateTime firstDue = LocalDateTime.now().plusYears(2);
        archiveService.applyRightsEvent(RightsEvent.authorized(
                "WB-ASSET-6", "使用权", "南网综合能源股份有限公司", "SQ-WB-6", firstDue));
        assertEquals(firstDue.toLocalDate(), byAsset("WB-ASSET-6").getValidDate().toLocalDate());

        // 续期场景:同资产再次回写更晚的到期日,台账应刷新为最新值(而非停在首次生效时的旧值)
        LocalDateTime renewed = firstDue.plusYears(1);
        archiveService.applyRightsEvent(RightsEvent.authorized(
                "WB-ASSET-6", "使用权", "南网综合能源股份有限公司", "SQ-WB-6-RENEW", renewed));
        assertEquals(renewed.toLocalDate(), byAsset("WB-ASSET-6").getValidDate().toLocalDate(), "续期后台账到期日应刷新");
    }

    @Test
    void null_valid_date_does_not_clobber_existing_archive_value() {
        LocalDateTime due = LocalDateTime.now().plusYears(2);
        archiveService.applyRightsEvent(RightsEvent.confirmed(
                "WB-ASSET-7", "空值不覆盖测试表", "使用权", "广东电网", "EC-WB-7", "QQ-WB-7", due));
        // 后续一次不带 validDate 的事件(如仅状态变更)不应把已有的到期日冲掉
        archiveService.applyRightsEvent(RightsEvent.authorized(
                "WB-ASSET-7", "使用权", "广州供电局", "SQ-WB-7", null));
        assertEquals(due.toLocalDate(), byAsset("WB-ASSET-7").getValidDate().toLocalDate(), "空 validDate 不应覆盖既有值");
    }
}
