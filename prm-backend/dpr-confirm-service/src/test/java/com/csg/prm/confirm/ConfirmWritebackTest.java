package com.csg.prm.confirm;

import com.csg.prm.common.writeback.LedgerWritebackGateway;
import com.csg.prm.common.writeback.RightsEvent;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.service.ConfirmApplyService;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0-① 确权域回写联动测试:走完确权制卡 -> 触发台账回写网关(确权事件)。
 * 用 @Primary 录制桩替换回写网关,验证流程贯通的事件已正确发出。
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(ConfirmWritebackTest.RecordingConfig.class)
class ConfirmWritebackTest {

    static class RecordingGateway implements LedgerWritebackGateway {
        final List<RightsEvent> events = new ArrayList<>();
        @Override
        public void apply(RightsEvent event) {
            events.add(event);
        }
    }

    @TestConfiguration
    static class RecordingConfig {
        @Bean
        @Primary
        RecordingGateway recordingLedgerWriteback() {
            return new RecordingGateway();
        }
    }

    @Autowired
    private ConfirmApplyService applyService;
    @Autowired
    private RecordingGateway recording;

    @Test
    void confirm_card_triggers_ledger_writeback_event() {
        ConfirmApply a = new ConfirmApply();
        a.setAssetId("WB-CFM-1");
        a.setAssetName("回写联动表");
        a.setRightType("数据持有权");
        a.setRightHolder("广东电网");
        String id = applyService.saveDraft(a);
        applyService.submit(id);          // -> 合规审核中(元数据质量达标)
        applyService.approve(id);         // 节点50 -> 主管
        applyService.approve(id);         // 节点60 -> 经理
        String cardId = applyService.approve(id); // 节点70 -> 制卡
        assertNotNull(cardId, "应制卡");

        RightsEvent ev = recording.events.stream()
                .filter(e -> "WB-CFM-1".equals(e.getAssetId())).findFirst().orElse(null);
        assertNotNull(ev, "确权制卡应触发台账回写事件");
        assertTrue(RightsEvent.TYPE_CONFIRMED.equals(ev.getEventType()));
        assertTrue("已确权".equals(ev.getConfirmStatus()));
    }
}
