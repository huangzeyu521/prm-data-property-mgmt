package com.csg.prm.common.workflow.cbpm;

import java.util.List;

/**
 * 南网云化流程组件(CBPM/BPS)对接网关。
 * <p>把确权/授权等业务流转(上报/下发/回退/转发/终止…)统一委托给南网平台流程引擎,
 * 替代/对接本地 {@link com.csg.prm.common.workflow.StateMachineFlowEngine} 的平台化路径。
 * <p>默认 {@link LocalCbpmWorkflowGateway} 本地桩(离线/测试);
 * 配置 {@code prm.cbpm.enabled=true} 时启用 {@link HttpCbpmWorkflowGateway}(@Primary)真调引擎。
 */
public interface CbpmWorkflowGateway {

    /** 单条操作(上报/下发/回退/转发/终止/挂起/恢复…),走 {@code /api/cbpm/bps/submit}。 */
    CbpmResult submit(CbpmSubmitRequest request);

    /** 批量操作,走 {@code /api/cbpm/bps/batchDataSubmit}。 */
    CbpmResult batchSubmit(List<CbpmSubmitRequest> requests);

    /** 分页查询待办,走 {@code /api/wf/queryTodoVOListByPage}。 */
    List<CbpmWorkItem> queryTodo(CbpmTodoQuery query);

    /** 查询待办数量,走 {@code /api/wf/queryTodoVOCount}。 */
    long countTodo(CbpmTodoQuery query);

    /** 分页查询已办,走 {@code /api/wf/queryDoneVOListByPage}。 */
    List<CbpmWorkItem> queryDone(CbpmTodoQuery query);
}
