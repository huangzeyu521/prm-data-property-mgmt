package com.csg.prm.common.workflow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 声明式流程定义注册表:把"审批链"从散落在 service 的 switch/数组中抽出为统一的有序状态序列,
 * 作为流程编排的单一事实来源。状态机引擎与 Camunda BPMN 适配器均以此为准(BPMN 顺序流与之一一对应)。
 *
 * <p>每个流程定义是一串有序状态:start() 返回首状态,advance() 沿序列推进,末状态为终态。</p>
 */
public final class FlowDefinitions {

    /** 确权审批链(对齐附录F:合规审核50 -> 主管复核60 -> 经理终审70 -> 制卡完成80) */
    public static final String DPR_CONFIRM = "DPR_CONFIRM";
    /** 授权·一事一议(专项)审批链 */
    public static final String DPR_AUTH_SPECIAL = "DPR_AUTH_SPECIAL";
    /** 授权·批量审批链 */
    public static final String DPR_AUTH_BATCH = "DPR_AUTH_BATCH";

    private static final Map<String, List<String>> FLOWS = new LinkedHashMap<>();

    static {
        // 确权审批链:提交后先"人工预审"(对 AI 校验结果人工复核),通过再进合规管控小组(生成表3/表4)。
        FLOWS.put(DPR_CONFIRM, List.of("人工预审中", "合规审核中", "主管复核中", "经理终审中", "已完成"));
        FLOWS.put(DPR_AUTH_SPECIAL, List.of("合规审核中", "业务审核中", "主管审核中", "经理审核中", "副总审批中", "已生效"));
        // 批量(附录F 步骤50-90):合规审核 -> 数字化部主管 -> 数字化部经理 -> 数字化部副总 -> 领导小组决策 -> 已生效。
        // 数字化部三节点(主管/经理/副总)与一事一议同名同粒度,差异仅在:一事一议第2节点为"业务审核中",批量末节点为"领导小组审批中"。
        FLOWS.put(DPR_AUTH_BATCH, List.of("合规审核中", "主管审核中", "经理审核中", "副总审批中", "领导小组审批中", "已生效"));
    }

    private FlowDefinitions() {
    }

    /** 取流程的有序状态序列;未知流程键抛异常(配置错误应尽早暴露) */
    public static List<String> states(String flowKey) {
        List<String> states = FLOWS.get(flowKey);
        if (states == null) {
            throw new IllegalArgumentException("未知流程定义:" + flowKey);
        }
        return states;
    }

    public static boolean exists(String flowKey) {
        return FLOWS.containsKey(flowKey);
    }
}
