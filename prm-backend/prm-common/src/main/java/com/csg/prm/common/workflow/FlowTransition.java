package com.csg.prm.common.workflow;

/**
 * 流程推进结果:推进后的目标状态、是否终态、目标状态在流程定义中的步序(0基)。
 *
 * @param nextState 推进后的状态
 * @param terminal  是否到达流程终态(如确权"已完成"、授权"已生效")
 * @param stepIndex 目标状态在流程定义中的索引(供需要节点编号的域使用)
 */
public record FlowTransition(String nextState, boolean terminal, int stepIndex) {
}
