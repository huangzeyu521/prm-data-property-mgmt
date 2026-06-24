package com.csg.prm.common.workflow.cbpm;

/**
 * 南网云化流程组件(CBPM/BPS)操作类型 operateType,严格对齐《云化流程组件 DAP 集成接口说明文档 1.1》。
 * 所有操作统一走 {@code POST /api/cbpm/bps/submit},以本类常量区分动作。
 */
public final class CbpmOperateType {

    private CbpmOperateType() {
    }

    /** 上报(创建并启动流程实例) */
    public static final int REPORT = 1;
    /** 下发(完成工作项 / 审批通过);指定下发亦为 2,另带 tagNodeInfoString */
    public static final int DISPATCH = 2;
    /** 回退;指定回退亦为 3,另带 tagNodeInfoString */
    public static final int BACK = 3;
    /** 转发(须带 nextParticipantList) */
    public static final int FORWARD = 4;
    /** 终止流程实例 */
    public static final int TERMINATE = 5;
    /** 挂起流程实例 */
    public static final int SUSPEND = 6;
    /** 撤回 */
    public static final int WITHDRAW = 7;
    /** 回退给申请人 */
    public static final int BACK_TO_APPLICANT = 9;
    /** 正常结束 */
    public static final int NORMAL_END = 12;
    /** 跳转下发(须带 nextParticipantList + tagNodeInfoString) */
    public static final int JUMP_DISPATCH = 13;
    /** 恢复(挂起后恢复) */
    public static final int RESUME = 14;
}
