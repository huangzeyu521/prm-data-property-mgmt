package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.confirm.dto.RecheckHealthVO;
import com.csg.prm.confirm.dto.RecheckTaskQuery;
import com.csg.prm.confirm.entity.ConfirmRecheckTask;

/**
 * 变更联动工单服务(季度重确权闭环:检测→工单→处置→销号)。
 * 三源入池:季度到期扫描 / 监测联动 / 变更生效联动;双出口:派生变更草稿 / 复核确认无变化。
 */
public interface ConfirmRecheckTaskService {

    /** 建工单(同资产×同类型存在未销号工单时幂等返回既有工单,不重复入池)。@return taskId */
    String createTask(ConfirmRecheckTask task);

    PageResult<ConfirmRecheckTask> page(RecheckTaskQuery query);

    ConfirmRecheckTask getById(String taskId);

    /** 季度到期扫描:有效期 days 天内(含已到期)的正常权益卡片 → 重确权工单。@return 新建工单数 */
    int scanDueCards(int days);

    /** 处置出口①:派生确权变更草稿(工单号写入 sourceRef 形成"检测→变更"证据链)。@return 新建变更申请ID */
    String deriveChange(String taskId);

    /** 处置出口②:复核确认无变化 —— 记录复核人/时间/结论并销号(重新确权的合法产出之一,必须留痕)。 */
    void confirmNoChange(String taskId, String note);

    /** 手工销号(授权处置完成等):记录处置人与说明。 */
    void complete(String taskId, String note);

    /** 派生变更单终审生效 → 关联工单自动销号。 */
    void completeByApply(String applyId);

    /**
     * P1.3 路径收敛:外部派生的变更草稿(监测联动等)登记入池 —— 有在池重确权工单则回链(状态→变更申请中),
     * 无则补建"变更申请中"工单,保证所有重确权路径在统一工单池可见可追溯。
     */
    void linkDerivedApply(String assetId, String assetName, String applyId,
                          String triggerType, String reason, String refNo);

    /** P2.2 变更健康度(闭环+版本链量化)。 */
    RecheckHealthVO health();
}
