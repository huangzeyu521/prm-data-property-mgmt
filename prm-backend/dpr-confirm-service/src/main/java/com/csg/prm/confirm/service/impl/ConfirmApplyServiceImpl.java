package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.workflow.FlowDefinitions;
import com.csg.prm.common.workflow.FlowTransition;
import com.csg.prm.common.workflow.ProcessFlowEngine;
import com.csg.prm.confirm.dto.ConfirmApplyQuery;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.gateway.MetadataGateway;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmFlowLogService;
import com.csg.prm.confirm.service.ConfirmSummaryService;
import com.csg.prm.confirm.service.EquityCardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ConfirmApplyServiceImpl implements ConfirmApplyService {

    /** 元数据质量门禁阈值:<80 自动驳回确权(需求§5.5 接口③) */
    private static final int QUALITY_THRESHOLD = 80;

    private final ConfirmApplyMapper mapper;
    private final EquityCardService equityCardService;
    private final ConfirmSummaryService summaryService;
    private final MetadataGateway metadataGateway;
    private final ProcessFlowEngine flowEngine;
    private final ConfirmFlowLogService flowLogService;

    public ConfirmApplyServiceImpl(ConfirmApplyMapper mapper, EquityCardService equityCardService,
                                   ConfirmSummaryService summaryService, MetadataGateway metadataGateway,
                                   ProcessFlowEngine flowEngine, ConfirmFlowLogService flowLogService) {
        this.mapper = mapper;
        this.equityCardService = equityCardService;
        this.summaryService = summaryService;
        this.metadataGateway = metadataGateway;
        this.flowEngine = flowEngine;
        this.flowLogService = flowLogService;
    }

    /** 各审批状态对应的责任人(节点处理人/角色)。 */
    private String responderOf(String status) {
        if (ConfirmApply.STATUS_COMPLIANCE.equals(status)) {
            return "合规管控小组";
        }
        if (ConfirmApply.STATUS_MANAGER.equals(status)) {
            return "数字化部主管";
        }
        if (ConfirmApply.STATUS_DIRECTOR.equals(status)) {
            return "确权经理";
        }
        return "申请人";
    }

    @Override
    public MetadataGateway.MetadataInfo autofill(String assetId) {
        return metadataGateway.autofill(assetId);
    }

    @Override
    @Transactional
    public String saveDraft(ConfirmApply apply) {
        validate(apply);
        if (StringUtils.hasText(apply.getApplyId())) {
            ConfirmApply exist = require(apply.getApplyId());
            if (!ConfirmApply.STATUS_DRAFT.equals(exist.getStatus())) {
                throw new BizException("仅草稿状态可编辑");
            }
            mapper.updateById(apply);
            return apply.getApplyId();
        }
        apply.setStatus(ConfirmApply.STATUS_DRAFT);
        if (!StringUtils.hasText(apply.getApplyNo())) {
            apply.setApplyNo(generateApplyNo());
        }
        mapper.insert(apply);
        return apply.getApplyId();
    }

    @Override
    @Transactional
    public void delete(String applyId) {
        ConfirmApply apply = require(applyId);
        if (!ConfirmApply.STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BizException("仅草稿状态的确权申请可删除;已提交/审批中请走撤回或驳回");
        }
        mapper.deleteById(applyId);
    }

    @Override
    @Transactional
    public String createReConfirm(String assetId, String assetName, String rightType,
                                  String reason, String sourceRef) {
        if (!StringUtils.hasText(assetId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "关联资产ID不能为空");
        }
        ConfirmApply apply = new ConfirmApply();
        apply.setAssetId(assetId);
        apply.setAssetName(StringUtils.hasText(assetName) ? assetName : assetId);
        apply.setRightType(StringUtils.hasText(rightType) ? rightType : "数据持有权");
        apply.setPurpose(StringUtils.hasText(reason) ? reason : "监测联动派生重确权");
        apply.setReConfirm(Boolean.TRUE);
        apply.setSourceRef(sourceRef);
        apply.setStatus(ConfirmApply.STATUS_DRAFT);
        apply.setApplyNo(generateApplyNo());
        mapper.insert(apply);
        return apply.getApplyId();
    }

    @Override
    @Transactional
    public void submit(String applyId) {
        ConfirmApply apply = require(applyId);
        if (!ConfirmApply.STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BizException("仅草稿状态可提交");
        }
        validate(apply);
        // 表2:勾选 B–F 来源识别或 G–J 关联识别(涉第三方/敏感)时,必须补充第三方权益信息(表2)
        if (requiresTable2(apply) && !StringUtils.hasText(apply.getThirdPartyInfo())) {
            throw new BizException("涉第三方/敏感数据(来源B–F或关联G–J),须填写表2第三方权益信息");
        }
        // 元数据质量门禁:质量评分<80 自动驳回确权(需求§5.5 接口③)。
        // 注意:此处"持久化驳回"而非抛异常——抛异常会令本事务回滚,驳回状态无法落库。
        int score = metadataGateway.qualityScore(apply.getAssetId());
        if (score < QUALITY_THRESHOLD) {
            String reason = "元数据质量评分 " + score + " 低于 " + QUALITY_THRESHOLD + ",自动驳回,请先治理元数据质量后重新提交";
            updateNode(applyId, ConfirmApply.STATUS_REJECTED, null, reason, null);
            flowLogService.record(apply, ConfirmApply.STATUS_DRAFT, ConfirmApply.STATUS_REJECTED, "元数据质量门禁", reason);
            return;
        }
        // 节点40归集审查通过 -> 由流程引擎启动确权审批链(首状态:合规审核)
        String first = flowEngine.start(FlowDefinitions.DPR_CONFIRM, applyId);
        updateNode(applyId, first, nodeOf(first), null, null);
        flowLogService.record(apply, ConfirmApply.STATUS_DRAFT, first, "申请人", null);
    }

    /**
     * 审批推进(对齐附录F 八节点):
     * 合规审核(50,合规管控小组:生成表3/表4+认定意见) -> 主管复核(60) -> 经理终审(70) -> 制卡(80)。
     * @return 终审通过(节点80)时返回生成的权益卡片ID,否则 null
     */
    @Override
    @Transactional
    public String approve(String applyId) {
        ConfirmApply apply = require(applyId);
        String from = apply.getStatus();
        if (!flowEngine.canAdvance(FlowDefinitions.DPR_CONFIRM, from)) {
            throw new BizException("当前状态不可审批:" + from);
        }
        // 节点50合规审核通过的随状态副作用:生成表3/表4与认定意见
        String opinion = null;
        if (ConfirmApply.STATUS_COMPLIANCE.equals(from)) {
            if (!StringUtils.hasText(apply.getRecognitionOpinion())) {
                apply.setRecognitionOpinion("合规管控小组审核通过,权属认定符合三权分置要求");
            }
            opinion = apply.getRecognitionOpinion();
            summaryService.generate(apply);
        }
        // 由流程引擎推进(去除硬编码 switch):合规->主管->经理->制卡完成
        FlowTransition t = flowEngine.advance(FlowDefinitions.DPR_CONFIRM, applyId, from);
        updateNode(applyId, t.nextState(), nodeOf(t.nextState()), null, opinion);
        // 流转留痕 + 进度通知(责任人=本节点审批人)
        flowLogService.record(apply, from, t.nextState(), responderOf(from), opinion);
        // 终态(节点80制卡):生成权益卡片并回写台账(EquityCardService 内完成)
        return t.terminal() ? equityCardService.generateFromApply(apply) : null;
    }

    /** 确权状态 -> 附录F 节点编号(纯映射,非流程逻辑) */
    private Integer nodeOf(String status) {
        if (ConfirmApply.STATUS_MANAGER.equals(status)) {
            return ConfirmApply.NODE_MANAGER;
        }
        if (ConfirmApply.STATUS_DIRECTOR.equals(status)) {
            return ConfirmApply.NODE_DIRECTOR;
        }
        if (ConfirmApply.STATUS_DONE.equals(status)) {
            return ConfirmApply.NODE_DONE;
        }
        return ConfirmApply.NODE_COMPLIANCE;
    }

    @Override
    @Transactional
    public void reject(String applyId, String reason) {
        ConfirmApply apply = require(applyId);
        // 仅审批链中(可推进)的状态可驳回——与引擎判定一致
        if (!flowEngine.canAdvance(FlowDefinitions.DPR_CONFIRM, apply.getStatus())) {
            throw new BizException("当前状态不可驳回:" + apply.getStatus());
        }
        String from = apply.getStatus();
        updateNode(applyId, ConfirmApply.STATUS_REJECTED, null, reason, null);
        flowLogService.record(apply, from, ConfirmApply.STATUS_REJECTED, responderOf(from), reason);
    }

    @Override
    public ConfirmApply getById(String applyId) {
        return require(applyId);
    }

    @Override
    public PageResult<ConfirmApply> page(ConfirmApplyQuery query) {
        IPage<ConfirmApply> page = mapper.selectPage(query.toPage(), historyWrapper(query));
        return PageResult.of(page);
    }

    /** 历史记录多维过滤:数据集(资产名)/权属类型/状态/人员(权属人)/申请时间范围。 */
    private LambdaQueryWrapper<ConfirmApply> historyWrapper(ConfirmApplyQuery query) {
        LambdaQueryWrapper<ConfirmApply> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(query.getAssetName()), ConfirmApply::getAssetName, query.getAssetName())
                .eq(StringUtils.hasText(query.getRightType()), ConfirmApply::getRightType, query.getRightType())
                .eq(StringUtils.hasText(query.getStatus()), ConfirmApply::getStatus, query.getStatus())
                .like(StringUtils.hasText(query.getRightHolder()), ConfirmApply::getRightHolder, query.getRightHolder())
                .ge(StringUtils.hasText(query.getCreateTimeStart()), ConfirmApply::getCreateTime, query.getCreateTimeStart())
                .le(StringUtils.hasText(query.getCreateTimeEnd()), ConfirmApply::getCreateTime, query.getCreateTimeEnd())
                .orderByDesc(ConfirmApply::getCreateTime);
        return w;
    }

    @Override
    public byte[] exportHistory(ConfirmApplyQuery query) {
        List<ConfirmApply> list = mapper.selectList(historyWrapper(query));
        StringBuilder sb = new StringBuilder("﻿"); // UTF-8 BOM
        sb.append("申请编号,资产名称,权属类型,权属人,状态,驳回原因,申请时间,处理时效\n");
        for (ConfirmApply a : list) {
            sb.append(csv(a.getApplyNo())).append(',')
                    .append(csv(a.getAssetName())).append(',')
                    .append(csv(a.getRightType())).append(',')
                    .append(csv(a.getRightHolder())).append(',')
                    .append(csv(a.getStatus())).append(',')
                    .append(csv(a.getRejectReason())).append(',')
                    .append(csv(String.valueOf(a.getCreateTime()))).append(',')
                    .append(csv(durationOf(a))).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /** 处理时效:终态(已完成/已驳回)用 updateTime-createTime,在途用 now-createTime。 */
    private String durationOf(ConfirmApply a) {
        if (a.getCreateTime() == null) {
            return "";
        }
        boolean terminal = ConfirmApply.STATUS_DONE.equals(a.getStatus())
                || ConfirmApply.STATUS_REJECTED.equals(a.getStatus());
        java.time.LocalDateTime end = terminal && a.getUpdateTime() != null
                ? a.getUpdateTime() : java.time.LocalDateTime.now();
        long mins = java.time.Duration.between(a.getCreateTime(), end).toMinutes();
        if (mins < 0) {
            mins = 0;
        }
        long days = mins / 1440;
        long hours = (mins % 1440) / 60;
        return days + "天" + hours + "小时" + (terminal ? "" : "(在途)");
    }

    private String csv(String s) {
        if (s == null || "null".equals(s)) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    @Override
    public com.csg.prm.confirm.dto.BatchResult batchSubmit(List<String> applyIds) {
        com.csg.prm.confirm.dto.BatchResult r = new com.csg.prm.confirm.dto.BatchResult();
        if (applyIds == null) {
            return r;
        }
        for (String id : applyIds) {
            try {
                submit(id);
                r.ok();
            } catch (RuntimeException e) {
                r.fail(id, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public com.csg.prm.confirm.dto.BatchResult batchApprove(List<String> applyIds) {
        com.csg.prm.confirm.dto.BatchResult r = new com.csg.prm.confirm.dto.BatchResult();
        if (applyIds == null) {
            return r;
        }
        for (String id : applyIds) {
            try {
                approve(id);
                r.ok();
            } catch (RuntimeException e) {
                r.fail(id, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public com.csg.prm.confirm.dto.BatchResult batchReject(List<String> applyIds, String reason) {
        com.csg.prm.confirm.dto.BatchResult r = new com.csg.prm.confirm.dto.BatchResult();
        if (applyIds == null) {
            return r;
        }
        for (String id : applyIds) {
            try {
                reject(id, reason);
                r.ok();
            } catch (RuntimeException e) {
                r.fail(id, e.getMessage());
            }
        }
        return r;
    }

    private void updateNode(String applyId, String status, Integer node, String rejectReason, String opinion) {
        ConfirmApply upd = new ConfirmApply();
        upd.setApplyId(applyId);
        upd.setStatus(status);
        upd.setCurrentNode(node);
        upd.setRejectReason(rejectReason);
        upd.setRecognitionOpinion(opinion);
        mapper.updateById(upd);
    }

    private ConfirmApply require(String applyId) {
        if (!StringUtils.hasText(applyId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "申请ID不能为空");
        }
        ConfirmApply apply = mapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "确权申请不存在");
        }
        return apply;
    }

    /** 表2 触发判定:来源识别含 B–F 或 关联识别含 G–J(涉第三方/敏感) */
    private boolean requiresTable2(ConfirmApply apply) {
        if (Boolean.TRUE.equals(apply.getInvolvesThirdParty())) {
            return true;
        }
        String src = apply.getSourceIdentification() == null ? "" : apply.getSourceIdentification().toUpperCase();
        String rel = apply.getRelationIdentification() == null ? "" : apply.getRelationIdentification().toUpperCase();
        for (String c : new String[]{"B", "C", "D", "E", "F"}) {
            if (src.contains(c)) {
                return true;
            }
        }
        for (String c : new String[]{"G", "H", "I", "J"}) {
            if (rel.contains(c)) {
                return true;
            }
        }
        return false;
    }

    private void validate(ConfirmApply apply) {
        if (apply == null) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(apply.getAssetId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "关联资产ID不能为空");
        }
        if (!StringUtils.hasText(apply.getAssetName())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产名称不能为空");
        }
        if (!StringUtils.hasText(apply.getRightType())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "权属类型不能为空");
        }
    }

    private String generateApplyNo() {
        return "QQ-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
