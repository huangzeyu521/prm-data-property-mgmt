package com.csg.prm.confirm.service;

import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmFlowLog;

import java.util.List;

/**
 * 确权申请流转历史/进度通知服务。
 */
public interface ConfirmFlowLogService {

    /** 记录一次状态流转(节点/责任人/意见/时间)并生成进度通知(系统消息)。 */
    void record(ConfirmApply apply, String fromStatus, String toStatus, String responder, String opinion);

    /** 某申请的流转历史(按时间正序,供进度时间线)。 */
    List<ConfirmFlowLog> listByApply(String applyId);
}
