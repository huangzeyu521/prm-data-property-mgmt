package com.csg.prm.confirm;

import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.EquityCardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 流程优化:季度重确权清单(F指导书"按季度定期重新确权"/权益到期)。
 * listReConfirmDue 应列出正常状态、有效期 90 天内(含已到期)的权益卡片。
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfirmReConfirmDueTest {

    @Autowired private EquityCardService cardService;
    @Autowired private EquityCardMapper cardMapper;

    private void card(String no, LocalDateTime valid, String status) {
        EquityCard c = new EquityCard();
        c.setCardNo(no);
        c.setAssetId(no);
        c.setRightType("持有权");
        c.setCardStatus(status);
        c.setValidDate(valid);
        cardMapper.insert(c);
    }

    @Test
    void due_lists_expiring_and_overdue_normal_cards() {
        String t = String.valueOf(System.nanoTime());
        card("RC-DUE-" + t, LocalDateTime.now().plusDays(30), EquityCard.STATUS_NORMAL);     // 30天内到期 → due
        card("RC-OVERDUE-" + t, LocalDateTime.now().minusDays(5), EquityCard.STATUS_NORMAL); // 已到期 → due
        card("RC-FAR-" + t, LocalDateTime.now().plusDays(200), EquityCard.STATUS_NORMAL);    // 远期 → 不due
        card("RC-FROZEN-" + t, LocalDateTime.now().plusDays(10), EquityCard.STATUS_FROZEN);  // 冻结 → 不due

        Set<String> due = cardService.listReConfirmDue(90).stream()
                .map(EquityCard::getCardNo).collect(Collectors.toSet());
        assertTrue(due.contains("RC-DUE-" + t), "90天内到期应列入");
        assertTrue(due.contains("RC-OVERDUE-" + t), "已到期应列入");
        assertFalse(due.contains("RC-FAR-" + t), "远期(>90天)不应列入");
        assertFalse(due.contains("RC-FROZEN-" + t), "非正常卡片不计入");

        // 清单按有效期升序(最紧迫在前)
        List<EquityCard> ordered = cardService.listReConfirmDue(90);
        for (int i = 1; i < ordered.size(); i++) {
            assertTrue(!ordered.get(i - 1).getValidDate().isAfter(ordered.get(i).getValidDate()),
                    "应按有效期升序");
        }
    }
}
