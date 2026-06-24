package com.csg.prm.common.workflow.cbpm;

import java.io.Serializable;
import java.util.List;

/**
 * CBPM/BPS 待办/已办等查询条件,对齐规范 {@code /api/wf/queryTodoVOList...} 等接口入参。
 */
public class CbpmTodoQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参与者唯一编号(必填) */
    private String transActor;
    /** 所属活动定义 ID */
    private String nodeId;
    /** 流程定义名称 */
    private String processId;
    /** 业务表排序字段名 */
    private List<String> sortNames;
    /** 排序类型(asc/desc) */
    private List<String> sortTypes;
    /** 页码(分页接口用) */
    private Integer pageNo;
    /** 每页记录数(分页接口用) */
    private Integer pageSize;

    public CbpmTodoQuery() {
    }

    public CbpmTodoQuery(String transActor) {
        this.transActor = transActor;
    }

    public String getTransActor() {
        return transActor;
    }

    public void setTransActor(String transActor) {
        this.transActor = transActor;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public List<String> getSortNames() {
        return sortNames;
    }

    public void setSortNames(List<String> sortNames) {
        this.sortNames = sortNames;
    }

    public List<String> getSortTypes() {
        return sortTypes;
    }

    public void setSortTypes(List<String> sortTypes) {
        this.sortTypes = sortTypes;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
