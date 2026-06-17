package com.csg.prm.confirm.job;

import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.service.EquityCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 季度重确权提醒(F指导书"权益变更"原则:按季度定期对权益信息变动/到期的数据重新确权)。
 * 每季度首月1日 9:00 扫描有效期 90 天内(含已到期)的正常权益卡片,生成待重确权清单提醒。
 */
@Component
public class ReConfirmReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReConfirmReminderJob.class);
    private static final int QUARTER_DAYS = 90;

    private final EquityCardService cardService;

    public ReConfirmReminderJob(EquityCardService cardService) {
        this.cardService = cardService;
    }

    @Scheduled(cron = "0 0 9 1 1,4,7,10 *")
    public void quarterlyReConfirmReminder() {
        List<EquityCard> due = cardService.listReConfirmDue(QUARTER_DAYS);
        if (due.isEmpty()) {
            log.info("[季度重确权提醒] 本季无到期/待重确权权益卡片");
            return;
        }
        log.info("[季度重确权提醒] 待重确权权益卡片 {} 张(有效期 {} 天内/已到期),请组织重新确权",
                due.size(), QUARTER_DAYS);
        // TODO: 对接通知中心/待办(右上角铃铛聚合),向责任部门定向推送待重确权清单。
    }
}
