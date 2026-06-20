package com.csg.prm.common.workflow;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 声明式状态机流程引擎单测:验证确权/授权审批链的逐级推进、终态、节点步序与非法推进。
 */
class StateMachineFlowEngineTest {

    private final ProcessFlowEngine engine = new StateMachineFlowEngine();

    @Test
    void confirm_chain_advances_through_appendix_F_nodes() {
        String key = FlowDefinitions.DPR_CONFIRM;
        assertEquals("人工预审中", engine.start(key, "b1"), "提交后首节点为人工预审");

        FlowTransition t0 = engine.advance(key, "b1", "人工预审中");
        assertEquals("合规审核中", t0.nextState());
        assertFalse(t0.terminal());

        FlowTransition t1 = engine.advance(key, "b1", "合规审核中");
        assertEquals("主管复核中", t1.nextState());
        assertFalse(t1.terminal());

        FlowTransition t2 = engine.advance(key, "b1", "主管复核中");
        assertEquals("经理终审中", t2.nextState());

        FlowTransition t3 = engine.advance(key, "b1", "经理终审中");
        assertEquals("已完成", t3.nextState());
        assertTrue(t3.terminal(), "经理终审通过 -> 制卡完成为终态");
    }

    @Test
    void auth_special_chain_has_five_approvals_then_effective() {
        String key = FlowDefinitions.DPR_AUTH_SPECIAL;
        assertEquals("合规审核中", engine.start(key, "a1"));
        assertEquals("业务审核中", engine.advance(key, "a1", "合规审核中").nextState());
        assertEquals("主管审核中", engine.advance(key, "a1", "业务审核中").nextState());
        assertEquals("经理审核中", engine.advance(key, "a1", "主管审核中").nextState());
        assertEquals("副总审批中", engine.advance(key, "a1", "经理审核中").nextState());
        FlowTransition last = engine.advance(key, "a1", "副总审批中");
        assertEquals("已生效", last.nextState());
        assertTrue(last.terminal());
    }

    @Test
    void auth_batch_chain_aligns_dept_nodes_and_ends_at_leadership() {
        // 节点对齐:批量数字化部三节点(主管/经理/副总)与一事一议同名同粒度,末节点为领导小组决策
        String key = FlowDefinitions.DPR_AUTH_BATCH;
        assertEquals("主管审核中", engine.advance(key, "a2", "合规审核中").nextState());
        assertEquals("经理审核中", engine.advance(key, "a2", "主管审核中").nextState());
        assertEquals("副总审批中", engine.advance(key, "a2", "经理审核中").nextState());
        assertEquals("领导小组审批中", engine.advance(key, "a2", "副总审批中").nextState());
        assertTrue(engine.advance(key, "a2", "领导小组审批中").terminal());
    }

    @Test
    void step_index_matches_position_in_chain() {
        // 业务审核中是专项链索引1 -> 推进自合规(0)返回步序1
        assertEquals(1, engine.advance(FlowDefinitions.DPR_AUTH_SPECIAL, "a1", "合规审核中").stepIndex());
        assertEquals(5, engine.advance(FlowDefinitions.DPR_AUTH_SPECIAL, "a1", "副总审批中").stepIndex());
    }

    @Test
    void cannot_advance_from_terminal_or_unknown_state() {
        assertFalse(engine.canAdvance(FlowDefinitions.DPR_CONFIRM, "已完成"), "终态不可推进");
        assertFalse(engine.canAdvance(FlowDefinitions.DPR_CONFIRM, "草稿"), "链外状态不可推进");
        assertTrue(engine.canAdvance(FlowDefinitions.DPR_CONFIRM, "合规审核中"));
        assertThrows(IllegalStateException.class,
                () -> engine.advance(FlowDefinitions.DPR_CONFIRM, "b1", "已完成"));
        assertThrows(IllegalStateException.class,
                () -> engine.advance(FlowDefinitions.DPR_CONFIRM, "b1", "草稿"));
    }
}
