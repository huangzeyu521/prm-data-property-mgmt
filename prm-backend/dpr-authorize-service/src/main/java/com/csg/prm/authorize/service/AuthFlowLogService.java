package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthFlowLog;

import java.util.List;

/**
 * 授权申请审批处理记录服务。
 */
public interface AuthFlowLogService {

    /** 记录一次状态流转(责任人/审核意见/时间)。 */
    void record(AuthApply apply, String fromStatus, String toStatus, String responder, String opinion);

    /** 某申请的处理记录(按时间正序,供审批轨迹时间线)。 */
    List<AuthFlowLog> listByApply(String applyId);
}
