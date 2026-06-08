package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmFlowLog;
import com.csg.prm.confirm.mapper.ConfirmFlowLogMapper;
import com.csg.prm.confirm.service.ConfirmFlowLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfirmFlowLogServiceImpl implements ConfirmFlowLogService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmFlowLogServiceImpl.class);
    private static final String CHANNEL = "系统消息";

    private final ConfirmFlowLogMapper mapper;

    public ConfirmFlowLogServiceImpl(ConfirmFlowLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void record(ConfirmApply apply, String fromStatus, String toStatus, String responder, String opinion) {
        ConfirmFlowLog entry = new ConfirmFlowLog();
        entry.setApplyId(apply.getApplyId());
        entry.setApplyNo(apply.getApplyNo());
        entry.setFromStatus(fromStatus);
        entry.setToStatus(toStatus);
        entry.setNode(nodeOf(toStatus));
        entry.setNodeName(nameOf(toStatus));
        entry.setResponder(responder);
        entry.setOpinion(opinion);
        entry.setNotifyContent(notify(apply, toStatus, opinion));
        entry.setPushChannel(CHANNEL);
        mapper.insert(entry);
        // 站内系统消息即落库可查;生产经统一消息中心/eLink 推送给申请人
        log.info("[确权进度通知] 申请={} {}->{} 责任人={} -> {}",
                apply.getApplyNo(), fromStatus, toStatus, responder, entry.getNotifyContent());
    }

    @Override
    public List<ConfirmFlowLog> listByApply(String applyId) {
        return mapper.selectList(new LambdaQueryWrapper<ConfirmFlowLog>()
                .eq(ConfirmFlowLog::getApplyId, applyId)
                .orderByAsc(ConfirmFlowLog::getCreateTime));
    }

    private Integer nodeOf(String status) {
        if (ConfirmApply.STATUS_COMPLIANCE.equals(status)) {
            return ConfirmApply.NODE_COMPLIANCE;
        }
        if (ConfirmApply.STATUS_MANAGER.equals(status)) {
            return ConfirmApply.NODE_MANAGER;
        }
        if (ConfirmApply.STATUS_DIRECTOR.equals(status)) {
            return ConfirmApply.NODE_DIRECTOR;
        }
        if (ConfirmApply.STATUS_DONE.equals(status)) {
            return ConfirmApply.NODE_DONE;
        }
        return null;
    }

    private String nameOf(String status) {
        if (ConfirmApply.STATUS_COMPLIANCE.equals(status)) {
            return "合规审核";
        }
        if (ConfirmApply.STATUS_MANAGER.equals(status)) {
            return "主管复核";
        }
        if (ConfirmApply.STATUS_DIRECTOR.equals(status)) {
            return "经理终审";
        }
        if (ConfirmApply.STATUS_DONE.equals(status)) {
            return "制卡完成";
        }
        if (ConfirmApply.STATUS_REJECTED.equals(status)) {
            return "驳回";
        }
        return status;
    }

    private String notify(ConfirmApply a, String to, String opinion) {
        String no = a.getApplyNo() == null ? a.getApplyId() : a.getApplyNo();
        if (ConfirmApply.STATUS_COMPLIANCE.equals(to)) {
            return "您的确权申请 " + no + " 已提交,进入【合规审核】";
        }
        if (ConfirmApply.STATUS_MANAGER.equals(to)) {
            return "确权申请 " + no + " 已通过合规审核,进入【主管复核】";
        }
        if (ConfirmApply.STATUS_DIRECTOR.equals(to)) {
            return "确权申请 " + no + " 已通过主管复核,进入【经理终审】";
        }
        if (ConfirmApply.STATUS_DONE.equals(to)) {
            return "确权申请 " + no + " 已通过终审并完成制卡,权益卡片已生成";
        }
        if (ConfirmApply.STATUS_REJECTED.equals(to)) {
            return "确权申请 " + no + " 已被驳回:" + (opinion == null ? "" : opinion);
        }
        return "确权申请 " + no + " 状态更新为:" + to;
    }
}
