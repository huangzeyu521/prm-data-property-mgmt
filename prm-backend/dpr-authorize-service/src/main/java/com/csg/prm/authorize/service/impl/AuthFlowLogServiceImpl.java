package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthFlowLog;
import com.csg.prm.authorize.mapper.AuthFlowLogMapper;
import com.csg.prm.authorize.service.AuthFlowLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthFlowLogServiceImpl implements AuthFlowLogService {

    private final AuthFlowLogMapper mapper;

    public AuthFlowLogServiceImpl(AuthFlowLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void record(AuthApply apply, String fromStatus, String toStatus, String responder, String opinion) {
        AuthFlowLog entry = new AuthFlowLog();
        entry.setApplyId(apply.getApplyId());
        entry.setApplyNo(apply.getApplyNo());
        entry.setFromStatus(fromStatus);
        entry.setToStatus(toStatus);
        entry.setNodeName(toStatus);
        entry.setResponder(responder);
        entry.setOpinion(opinion);
        mapper.insert(entry);
    }

    @Override
    public List<AuthFlowLog> listByApply(String applyId) {
        return mapper.selectList(new LambdaQueryWrapper<AuthFlowLog>()
                .eq(AuthFlowLog::getApplyId, applyId)
                .orderByAsc(AuthFlowLog::getCreateTime));
    }
}
