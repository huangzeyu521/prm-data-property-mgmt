package com.csg.prm.common.workflow.cbpm;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * CBPM/BPS 操作请求体(对齐《DAP 集成接口说明文档 1.1》提交接口)。
 * 提交时按数组上送:{@code [ {...} ]};以 {@link #operateType} 区分动作。
 * 工厂方法封装各动作的必填项,业务侧只填关键参数即可,确保遵循规范。
 */
public class CbpmSubmitRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务表实体 ID(必填) */
    private String workId;
    /** 业务逻辑处理门面类(必填,如 wfFacade) */
    private String workflowFacadeName;
    /** 操作类型,见 {@link CbpmOperateType}(必填) */
    private Integer operateType;
    /** 工作项 ID(下发/回退/转发/撤回/跳转下发 必填) */
    private String taskId;
    /** 流程实例 ID(终止/挂起/恢复/正常结束 必填) */
    private String processInsId;
    /** 意见 */
    private String opinion;
    /** 流程定义名称 → jadp 流程编号 映射(必填) */
    private Map<String, String> jadpTodoIds;
    /** 流程定义名称(上报可选,缺省读系统配置) */
    private String processDefName;
    /** 流程实例名称(上报可选) */
    private String processInstName;
    /** jadp 流程编码(上报可选) */
    private String processId;
    /** 业务流程相关数据(流程变量,路由条件用) */
    private Map<String, Object> relativeData;
    /** 工作项对应的用户数据 */
    private Map<String, Object> userData;
    /** 下一环节处理人(转发/跳转下发 必填) */
    private List<WFParticipant> nextParticipantList;
    /** 指定下发/回退/跳转下发的目标节点信息(JSON 字符串) */
    private String tagNodeInfoString;
    /** 扩展参数 */
    private Map<String, Object> extParams;

    public CbpmSubmitRequest() {
    }

    // ======================== 工厂方法(封装各动作必填项,遵循规范) ========================

    /** 上报:创建并启动流程实例。 */
    public static CbpmSubmitRequest report(String workId, String facadeName,
                                           Map<String, String> jadpTodoIds,
                                           List<WFParticipant> next) {
        CbpmSubmitRequest r = base(workId, facadeName, CbpmOperateType.REPORT, jadpTodoIds);
        r.nextParticipantList = next;
        return r;
    }

    /** 下发(完成工作项 / 审批通过)。 */
    public static CbpmSubmitRequest dispatch(String workId, String facadeName, String taskId,
                                             String opinion, Map<String, String> jadpTodoIds,
                                             List<WFParticipant> next) {
        CbpmSubmitRequest r = base(workId, facadeName, CbpmOperateType.DISPATCH, jadpTodoIds);
        r.taskId = taskId;
        r.opinion = opinion;
        r.nextParticipantList = next;
        return r;
    }

    /** 回退。 */
    public static CbpmSubmitRequest back(String workId, String facadeName, String taskId,
                                         String opinion, Map<String, String> jadpTodoIds,
                                         List<WFParticipant> next) {
        CbpmSubmitRequest r = base(workId, facadeName, CbpmOperateType.BACK, jadpTodoIds);
        r.taskId = taskId;
        r.opinion = opinion;
        r.nextParticipantList = next;
        return r;
    }

    /** 转发(必带下一处理人)。 */
    public static CbpmSubmitRequest forward(String workId, String facadeName, String taskId,
                                            String opinion, Map<String, String> jadpTodoIds,
                                            List<WFParticipant> next) {
        CbpmSubmitRequest r = base(workId, facadeName, CbpmOperateType.FORWARD, jadpTodoIds);
        r.taskId = taskId;
        r.opinion = opinion;
        r.nextParticipantList = next;
        return r;
    }

    /** 终止流程实例。 */
    public static CbpmSubmitRequest terminate(String workId, String facadeName, String processInsId,
                                              String opinion, Map<String, String> jadpTodoIds) {
        CbpmSubmitRequest r = base(workId, facadeName, CbpmOperateType.TERMINATE, jadpTodoIds);
        r.processInsId = processInsId;
        r.opinion = opinion;
        return r;
    }

    /** 正常结束。 */
    public static CbpmSubmitRequest normalEnd(String workId, String facadeName, String processInsId,
                                              String opinion, Map<String, String> jadpTodoIds) {
        CbpmSubmitRequest r = base(workId, facadeName, CbpmOperateType.NORMAL_END, jadpTodoIds);
        r.processInsId = processInsId;
        r.opinion = opinion;
        return r;
    }

    private static CbpmSubmitRequest base(String workId, String facadeName, int operateType,
                                          Map<String, String> jadpTodoIds) {
        CbpmSubmitRequest r = new CbpmSubmitRequest();
        r.workId = workId;
        r.workflowFacadeName = facadeName;
        r.operateType = operateType;
        r.jadpTodoIds = jadpTodoIds;
        return r;
    }

    // ======================== getter / setter ========================

    public String getWorkId() {
        return workId;
    }

    public void setWorkId(String workId) {
        this.workId = workId;
    }

    public String getWorkflowFacadeName() {
        return workflowFacadeName;
    }

    public void setWorkflowFacadeName(String workflowFacadeName) {
        this.workflowFacadeName = workflowFacadeName;
    }

    public Integer getOperateType() {
        return operateType;
    }

    public void setOperateType(Integer operateType) {
        this.operateType = operateType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProcessInsId() {
        return processInsId;
    }

    public void setProcessInsId(String processInsId) {
        this.processInsId = processInsId;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public Map<String, String> getJadpTodoIds() {
        return jadpTodoIds;
    }

    public void setJadpTodoIds(Map<String, String> jadpTodoIds) {
        this.jadpTodoIds = jadpTodoIds;
    }

    public String getProcessDefName() {
        return processDefName;
    }

    public void setProcessDefName(String processDefName) {
        this.processDefName = processDefName;
    }

    public String getProcessInstName() {
        return processInstName;
    }

    public void setProcessInstName(String processInstName) {
        this.processInstName = processInstName;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public Map<String, Object> getRelativeData() {
        return relativeData;
    }

    public void setRelativeData(Map<String, Object> relativeData) {
        this.relativeData = relativeData;
    }

    public Map<String, Object> getUserData() {
        return userData;
    }

    public void setUserData(Map<String, Object> userData) {
        this.userData = userData;
    }

    public List<WFParticipant> getNextParticipantList() {
        return nextParticipantList;
    }

    public void setNextParticipantList(List<WFParticipant> nextParticipantList) {
        this.nextParticipantList = nextParticipantList;
    }

    public String getTagNodeInfoString() {
        return tagNodeInfoString;
    }

    public void setTagNodeInfoString(String tagNodeInfoString) {
        this.tagNodeInfoString = tagNodeInfoString;
    }

    public Map<String, Object> getExtParams() {
        return extParams;
    }

    public void setExtParams(Map<String, Object> extParams) {
        this.extParams = extParams;
    }
}
