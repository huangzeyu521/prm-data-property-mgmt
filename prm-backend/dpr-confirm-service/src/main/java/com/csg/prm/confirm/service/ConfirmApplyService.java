package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.confirm.dto.ConfirmApplyQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.gateway.MetadataGateway;

/**
 * 数据确权申请与审批服务。
 * 状态机:草稿 -(提交)-> 合规审核中 -(通过)-> 主管复核中 -(通过)-> 已完成(自动生成权益卡片);
 *         审核中任一环节可驳回 -> 已驳回。
 */
public interface ConfirmApplyService {

    /** 元数据自动填充确权表单要素(含质量评分) */
    MetadataGateway.MetadataInfo autofill(String assetId);

    /**
     * 权益事实(确权信息带出):按资产取最新已完成确权,推导第三方来源/隐私商密事实,
     * 供授权侧只读带出,避免人工低报击穿应交材料与合规校验。
     */
    com.csg.prm.confirm.dto.RightsFactsVO rightsFacts(String assetId);

    /** 暂存草稿,返回申请ID */
    String saveDraft(ConfirmApply apply);

    /**
     * 派生重确权工单(草稿,标记 reConfirm)。
     * 供权益动态监测在识别"数据新增/来源变更/到期"时联动调用(附录F 3.3.2 季度重确权)。
     * @return 新建重确权申请ID
     */
    String createReConfirm(String assetId, String assetName, String rightType, String reason,
                           String sourceRef, String changeTrigger);

    /** 固化提交前的 AI 校验结果快照(JSON),供人工预审完整复核·可追溯。在提交前调用。 */
    void saveAiSnapshot(String applyId, String snapshotJson);

    /** 提交进入审批(草稿 -> 人工预审中) */
    void submit(String applyId);

    /**
     * 审批通过,推进到下一节点;终审通过自动生成权益卡片。
     * @return 终审通过时返回生成的权益卡片ID,否则返回 null
     */
    String approve(String applyId);

    /** 驳回 */
    void reject(String applyId, String reason);

    /**
     * 申请人主动撤回(审批中 -> 已撤回中间态)。仅审批链活动态可撤回,且仅申请人本人(非节点审批角色)。
     * 审批中尚未制卡,撤回无副作用;撤回后可经"重新编辑提交"复用原单内容再次发起。
     */
    void withdraw(String applyId, String reason);

    /** 删除确权申请:仅草稿状态可删除(已提交/审批中请走撤回或驳回)。 */
    void delete(String applyId);

    ConfirmApply getById(String applyId);

    PageResult<ConfirmApply> page(ConfirmApplyQuery query);

    /** 导出确权申请历史记录(CSV,含处理时效),按当前过滤条件。 */
    byte[] exportHistory(ConfirmApplyQuery query);

    /** 批量提交草稿申请至审核(逐条,失败不影响其余)。 */
    com.csg.prm.confirm.dto.BatchResult batchSubmit(java.util.List<String> applyIds);

    /** 批量审批通过(逐条走流程引擎)。 */
    com.csg.prm.confirm.dto.BatchResult batchApprove(java.util.List<String> applyIds);

    /** 批量驳回(逐条,统一驳回原因)。 */
    com.csg.prm.confirm.dto.BatchResult batchReject(java.util.List<String> applyIds, String reason);
}
