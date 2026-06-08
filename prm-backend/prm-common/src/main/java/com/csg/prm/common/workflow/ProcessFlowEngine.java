package com.csg.prm.common.workflow;

/**
 * 流程编排引擎端口。确权/授权服务通过此端口推进审批,不再内联硬编码的 switch/链数组。
 * 默认实现为声明式状态机({@link StateMachineFlowEngine},离线确定性);
 * 信创栈下可由 Camunda 7 BPMN 适配器 @Primary 覆盖(prm.workflow.engine=camunda)。
 *
 * <p>状态以业务实体自身的 status 字段为权威来源(businessKey=单据ID),
 * 引擎据当前状态与流程定义计算下一步,使两种实现可互换。</p>
 */
public interface ProcessFlowEngine {

    /** 启动流程,返回首个审批状态(如"合规审核中") */
    String start(String flowKey, String businessKey);

    /** 推进一步:依据当前状态返回下一状态;当前状态不可推进时抛 IllegalStateException */
    FlowTransition advance(String flowKey, String businessKey, String currentState);

    /** 当前状态是否可继续推进(非终态且在流程链中) */
    boolean canAdvance(String flowKey, String currentState);
}
