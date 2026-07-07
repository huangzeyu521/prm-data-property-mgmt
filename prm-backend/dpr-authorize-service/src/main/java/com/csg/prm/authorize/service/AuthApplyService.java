package com.csg.prm.authorize.service;

import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.common.api.PageResult;

import java.util.List;

/**
 * 数据授权申请与审批服务。
 * 强制"先确后授":提交时校验引用的权益卡片有效。
 * 专项(一事一议)终审=批准(待双签),协议双签+承诺函归档后置已生效;批量终审即已生效。
 */
public interface AuthApplyService {

    /**
     * P0(权益动态监测):协议续期后同步台账到期日。续期改的是 AuthAgreement.validUntil,不是
     * AuthApply.validDate 本身——若不重新回写,台账 validDate 会停在首次生效时的旧日期,续期后
     * 立即又变回"扫描的是过期数据",P0-① 的修复对已续期的协议就形同虚设。
     */
    void syncLedgerValidDate(String applyId, java.time.LocalDateTime validDate);

    String saveDraft(AuthApply apply);

    /** 删除授权申请(仅草稿态可删,如批量清单里加错/重复的明细项);逻辑删除。 */
    void deleteApply(String applyId);

    /** 按批量清单ID查其下授权项(表6 明细行) */
    List<AuthApply> byBatch(String batchListId);

    /** 新建一事一议申请单:返回申请单号 formNo,供多张数据表逐张 saveDraft 共用(同号=一份表5多表)。 */
    String createForm();

    /** 按申请单号查其下数据表行(一事一议多表;向导回填/历史详情)。 */
    List<AuthApply> byForm(String formNo);

    /** 提交整份一事一议申请单:formNo 下全部草稿行逐行 submit(同一事务,任一行硬门禁失败整单回滚)。 */
    void submitForm(String formNo);

    /** 申请单逐表自检(只读试跑):逐行硬门禁(先确后授/第三方凭证/信息授权协议/范围⊆边界)+ 协议要素提示。复用 BatchComplianceResult。 */
    com.csg.prm.authorize.dto.BatchComplianceResult checkFormCompliance(String formNo);

    /** 提交审批(校验先确后授;草稿 -> 审核中) */
    void submit(String applyId);

    /** 只读试跑:返回提交将被拦截的合规原因(先确后授/经营权目录/范围⊆边界/期限),通过返回 null。不改状态。 */
    String submitBlockReason(String applyId);

    /**
     * 审批通过(带审核意见)沿链推进。专项终审=批准(待双签,生效副作用移至协议归档);
     * 批量终审=已生效并返回生效记录ID;其余节点返回 null。
     */
    String approve(String applyId, String opinion);

    /**
     * 协议双签+《保密承诺函》归档后收口:一事一议申请 批准->已生效,执行生效副作用
     * (生效记录/台账/卡片回写)。非批准态幂等跳过返回 null;卡片冻结抛熔断异常。
     */
    String markEffectiveAfterAgreement(String applyId);

    void reject(String applyId, String reason);

    /** 申请人主动撤回(审批中 -> 已撤回);仅本人、仅审批链活动态可撤;撤回后可修改重提。 */
    void withdraw(String applyId, String reason);

    /** 整份一事一议申请单撤回:formNo 下全部处于审批中的明细逐行撤回(草稿/已生效等非活动态跳过)。 */
    void withdrawForm(String formNo);

    /** 明细退回草稿(供批量清单撤回:申报稿→草案时,把在审明细退回草稿以便再编辑/再提交)。草稿态幂等、终态跳过。 */
    void returnToDraft(String applyId);

    /** 批量审批通过(逐条,失败不影响其余)。 */
    com.csg.prm.authorize.dto.BatchResult batchApprove(List<String> applyIds);

    /** 批量驳回(逐条,统一原因)。 */
    com.csg.prm.authorize.dto.BatchResult batchReject(List<String> applyIds, String reason);

    AuthApply getById(String applyId);

    PageResult<AuthApply> page(AuthApplyQuery query);
}
