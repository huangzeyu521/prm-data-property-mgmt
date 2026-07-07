package com.csg.prm.confirm.job;

import com.csg.prm.confirm.service.ConfirmRecheckTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 季度重确权(35号文 §二(三)2 权益变更:按季度定期对权益信息变动/到期的数据重新确权)。
 * 每季度首月1日 9:00 扫描有效期 90 天内(含已到期)的正常权益卡片,生成重确权工单入池
 * (通知中心/工单页可见,处置双出口:派生变更草稿 / 复核确认无变化)——检测→工单→处置→销号闭环。
 * 本 Job 即指引确权流程步骤10「公司总部数字化管理部门主管组织发起数据确权认定工作」的系统化身。
 */
@Component
public class ReConfirmReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReConfirmReminderJob.class);
    private static final int QUARTER_DAYS = 90;

    private final ConfirmRecheckTaskService recheckTaskService;

    public ReConfirmReminderJob(ConfirmRecheckTaskService recheckTaskService) {
        this.recheckTaskService = recheckTaskService;
    }

    @Scheduled(cron = "0 0 9 1 1,4,7,10 *")
    public void quarterlyReConfirmReminder() {
        int created = recheckTaskService.scanDueCards(QUARTER_DAYS);
        if (created == 0) {
            log.info("[季度重确权] 本季无新增待重确权工单(无到期卡片或已有在池工单)");
        } else {
            log.info("[季度重确权] 新建重确权工单 {} 张,已入工单池待处置(通知中心可见)", created);
        }
    }
}
