package com.csg.prm.common.workflow;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 声明式状态机流程引擎(默认实现):依据 {@link FlowDefinitions} 的有序状态序列推进。
 * 纯内存、确定性、零外部依赖——离线与单测始终可用,且是去硬编码后的权威默认。
 */
@Component
public class StateMachineFlowEngine implements ProcessFlowEngine {

    @Override
    public String start(String flowKey, String businessKey) {
        return FlowDefinitions.states(flowKey).get(0);
    }

    @Override
    public FlowTransition advance(String flowKey, String businessKey, String currentState) {
        List<String> states = FlowDefinitions.states(flowKey);
        int idx = states.indexOf(currentState);
        if (idx < 0) {
            throw new IllegalStateException("状态[" + currentState + "]不在流程[" + flowKey + "]中,不可推进");
        }
        if (idx >= states.size() - 1) {
            throw new IllegalStateException("状态[" + currentState + "]已是流程[" + flowKey + "]终态,不可再推进");
        }
        int nextIdx = idx + 1;
        String next = states.get(nextIdx);
        boolean terminal = nextIdx == states.size() - 1;
        return new FlowTransition(next, terminal, nextIdx);
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
}
