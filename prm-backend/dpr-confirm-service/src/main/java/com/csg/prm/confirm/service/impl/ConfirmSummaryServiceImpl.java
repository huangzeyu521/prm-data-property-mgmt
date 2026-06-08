package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmSummary;
import com.csg.prm.confirm.mapper.ConfirmSummaryMapper;
import com.csg.prm.confirm.service.ConfirmSummaryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConfirmSummaryServiceImpl implements ConfirmSummaryService {

    private final ConfirmSummaryMapper mapper;

    public ConfirmSummaryServiceImpl(ConfirmSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void generate(ConfirmApply a) {
        String who = UserContextHolder.get().getUserId();
        LocalDateTime now = LocalDateTime.now();
        // 表3:数据确权信息汇总表
        ConfirmSummary t3 = new ConfirmSummary();
        t3.setApplyId(a.getApplyId());
        t3.setSummaryType(ConfirmSummary.TYPE_T3);
        t3.setContent("资产:" + a.getAssetName() + ";权属类型:" + a.getRightType()
                + ";权利主体:" + a.getRightHolder()
                + ";涉及第三方:" + (Boolean.TRUE.equals(a.getInvolvesThirdParty()) ? "是" : "否"));
        t3.setGeneratorId(who);
        t3.setGenerateTime(now);
        mapper.insert(t3);
        // 表4:数据权益内部管理汇总表
        ConfirmSummary t4 = new ConfirmSummary();
        t4.setApplyId(a.getApplyId());
        t4.setSummaryType(ConfirmSummary.TYPE_T4);
        t4.setContent("责任部门:" + a.getRespDept() + ";有效期:" + a.getValidDate()
                + ";认定意见:" + (a.getRecognitionOpinion() == null ? "合规通过" : a.getRecognitionOpinion()));
        t4.setGeneratorId(who);
        t4.setGenerateTime(now);
        mapper.insert(t4);
    }

    @Override
    public List<ConfirmSummary> listByApply(String applyId) {
        LambdaQueryWrapper<ConfirmSummary> w = new LambdaQueryWrapper<>();
        w.eq(ConfirmSummary::getApplyId, applyId).orderByAsc(ConfirmSummary::getSummaryType);
        return mapper.selectList(w);
    }
}
