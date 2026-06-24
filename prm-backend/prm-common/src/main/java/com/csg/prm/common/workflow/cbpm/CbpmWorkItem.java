package com.csg.prm.common.workflow.cbpm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * CBPM/BPS 工作项视图(待办/已办/审批中等查询返回的核心子集),对齐规范响应参数。
 * 仅取 PRM 消费所需字段;忽略未知字段以兼容引擎返回的完整 VO。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbpmWorkItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务数据 ID */
    private String id;
    /** 工作项 ID */
    private String taskId;
    /** 流程实例 ID */
    private String processInsId;
    /** 流程状态:1-运行,2-挂起,3-完成,4-终止 */
    private Integer flowState;
    /** 流程定义名称 */
    private String processDefName;
    /** jadp 流程编码 */
    private String processId;
    /** 所属活动定义 ID */
    private String nodeId;
    /** 所属活动定义名称 */
    private String nodeName;
    /** 业务表标题 */
    private String title;
    /** 业务表名称 */
    private String name;
    /** 上报人员 ID */
    private String reporterId;
    /** 参与者唯一编号 */
    private String transActor;
    /** 业务逻辑处理门面类 */
    private String workflowFacadeName;
    /** 租户 ID */
    private String tenantId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Integer getFlowState() {
        return flowState;
    }

    public void setFlowState(Integer flowState) {
        this.flowState = flowState;
    }

    public String getProcessDefName() {
        return processDefName;
    }

    public void setProcessDefName(String processDefName) {
        this.processDefName = processDefName;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getTransActor() {
        return transActor;
    }

    public void setTransActor(String transActor) {
        this.transActor = transActor;
    }

    public String getWorkflowFacadeName() {
        return workflowFacadeName;
    }

    public void setWorkflowFacadeName(String workflowFacadeName) {
        this.workflowFacadeName = workflowFacadeName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
