package com.csg.prm.confirm.workflow;

import com.csg.prm.common.workflow.FlowDefinitions;
import com.csg.prm.common.workflow.FlowTransition;
import com.csg.prm.common.workflow.ProcessFlowEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Camunda 7 BPMN 流程编排适配器(信创栈)。仅在 -Pcamunda 构建且 prm.workflow.engine=camunda 时
 * @Primary 覆盖默认状态机引擎,用真实 BPMN 流程实例驱动审批推进。
 *
 * <p>约定:每个流程定义键(如 DPR_CONFIRM)对应一个同 id 的 BPMN process;每个审批状态对应一个
 * 同名(name=状态串)的用户任务;business key=单据ID。状态以业务实体为权威,引擎据 BPMN 校验并推进。
 * 目标状态串与步序仍以 {@link FlowDefinitions} 对齐,保证与默认引擎行为一致、可互换。</p>
 */
@Component
@Primary
@ConditionalOnProperty(name = "prm.workflow.engine", havingValue = "camunda")
public class CamundaFlowEngine implements ProcessFlowEngine {

    private static final Logger log = LoggerFactory.getLogger(CamundaFlowEngine.class);

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public CamundaFlowEngine(RuntimeService runtimeService, TaskService taskService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        log.info("[流程编排] 已启用 Camunda 7 BPMN 引擎");
    }

    @Override
    public String start(String flowKey, String businessKey) {
        runtimeService.startProcessInstanceByKey(flowKey, businessKey);
        Task t = currentTask(flowKey, businessKey);
        if (t == null) {
            // 无用户任务(理论上不会):退回声明式首状态
            return FlowDefinitions.states(flowKey).get(0);
        }
        return t.getName();
    }

    @Override
    public FlowTransition advance(String flowKey, String businessKey, String currentState) {
        Task t = currentTask(flowKey, businessKey);
        if (t == null) {
            throw new IllegalStateException("流程[" + flowKey + "]实例[" + businessKey + "]无待办任务,不可推进");
        }
        taskService.complete(t.getId());
        Task next = currentTask(flowKey, businessKey);
        List<String> states = FlowDefinitions.states(flowKey);
        if (next == null) {
            // 流程结束 -> 终态(定义中的末状态)
            String terminal = states.get(states.size() - 1);
            return new FlowTransition(terminal, true, states.size() - 1);
        }
        String nextState = next.getName();
        int idx = states.indexOf(nextState);
        return new FlowTransition(nextState, false, idx);
    }

    @Override
    public boolean canAdvance(String flowKey, String currentState) {
        if (!FlowDefinitions.exists(flowKey)) {
            return false;
        }
        List<String> states = FlowDefinitions.states(flowKey);
        int idx = states.indexOf(currentState);
        return idx >= 0 && idx < states.size() - 1;
    }

    private Task currentTask(String flowKey, String businessKey) {
        return taskService.createTaskQuery()
                .processDefinitionKey(flowKey)
                .processInstanceBusinessKey(businessKey)
                .active()
                .listPage(0, 1).stream().findFirst().orElse(null);
    }
}
