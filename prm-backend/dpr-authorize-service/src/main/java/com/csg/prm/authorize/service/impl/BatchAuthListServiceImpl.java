package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.dto.BatchComplianceResult;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.mapper.BatchAuthListMapper;
import com.csg.prm.authorize.service.AuthAgreementService;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchAuthListServiceImpl implements BatchAuthListService {

    private static final Logger log = LoggerFactory.getLogger(BatchAuthListServiceImpl.class);

    private final BatchAuthListMapper mapper;
    private final AuthApplyService applyService;
    private final AuthAgreementService agreementService;

    public BatchAuthListServiceImpl(BatchAuthListMapper mapper, AuthApplyService applyService,
                                    AuthAgreementService agreementService) {
        this.mapper = mapper;
        this.applyService = applyService;
        this.agreementService = agreementService;
    }

    @Override
    @Transactional
    public String create(BatchAuthList list) {
        if (!StringUtils.hasText(list.getListYear())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "授权年度不能为空");
        }
        if (!StringUtils.hasText(list.getListNo())) {
            list.setListNo("PLQD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase());
        }
        list.setListStatus(BatchAuthList.STATUS_DRAFT);
        if (list.getItemCount() == null) {
            list.setItemCount(0);
        }
        mapper.insert(list);
        return list.getBatchListId();
    }

    @Override
    @Transactional
    public void submit(String batchListId) {
        BatchAuthList l = require(batchListId);
        if (!BatchAuthList.STATUS_DRAFT.equals(l.getListStatus())) {
            throw new BusinessException("仅草案可提交为申报稿");
        }
        // 逐项合规门禁(与一事一议对齐):先拦第三方凭证红线,再逐条提交进批量审批链
        // ——之前 submit 只改清单状态、明细停在草稿从未进链;此处补齐"提交即逐项校验+入链"。
        List<AuthApply> items = applyService.byBatch(batchListId);
        if (items.isEmpty()) {
            throw new BusinessException("清单为空,不可提交申报稿(请先添加授权项)");
        }
        List<String> blocked = new ArrayList<>();
        for (AuthApply a : items) {
            if (!AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                continue; // 已入链的明细跳过(幂等)
            }
            if (StringUtils.hasText(a.getThirdPartySource()) && !StringUtils.hasText(a.getThirdPartyLicense())) {
                blocked.add(a.getAssetName() + "(涉第三方未提许可凭证)");
            }
        }
        if (!blocked.isEmpty()) {
            throw new BusinessException("以下明细未通过合规校验,清单不可提交:" + String.join("、", blocked));
        }
        // 逐条提交:复用一事一议同一套硬校验(先确后授+授权⊆确权边界+经营权仅限开放目录+默认2年)
        for (AuthApply a : items) {
            if (AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                applyService.submit(a.getApplyId());
            }
        }
        update(batchListId, BatchAuthList.STATUS_SUBMITTED);
    }

    /** 只读试跑:逐草稿明细做合规校验(第三方凭证红线 + submitBlockReason),返回被拦清单。不改状态。 */
    @Override
    public BatchComplianceResult complianceCheck(String batchListId) {
        require(batchListId);
        List<AuthApply> items = applyService.byBatch(batchListId);
        List<BatchComplianceResult.BlockedItem> blocked = new ArrayList<>();
        List<BatchComplianceResult.ItemCheck> checks = new ArrayList<>();
        int draftTotal = 0;
        for (AuthApply a : items) {
            if (!AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                continue; // 已入链明细跳过(与 submit 幂等一致)
            }
            draftTotal++;
            // 逐维度校验(通过项也返回,供前端展示完整明细):先确后授·第三方凭证·信息授权协议·授权范围/其他合规
            List<BatchComplianceResult.Dim> dims = new ArrayList<>();
            // ① 先确后授:须有生效权益卡片
            boolean cardOk = StringUtils.hasText(a.getEquityCardId());
            dims.add(new BatchComplianceResult.Dim("先确后授·生效卡片", cardOk,
                    cardOk ? a.getEquityCardId() : "缺生效权益卡片(请先完成确权)"));
            // ② 第三方许可凭证:涉第三方须有凭证(确权带出或补传)
            boolean thirdInvolved = StringUtils.hasText(a.getThirdPartySource());
            boolean thirdOk = !thirdInvolved || StringUtils.hasText(a.getThirdPartyLicense());
            dims.add(new BatchComplianceResult.Dim("第三方许可凭证", thirdOk,
                    !thirdInvolved ? "—" : (thirdOk ? "已具备(确权带出/补传)" : "涉第三方,缺许可凭证")));
            // ③ 信息授权协议:涉隐私/商密须有协议
            boolean sensInvolved = StringUtils.hasText(a.getSensitiveType()) && !"无".equals(a.getSensitiveType());
            boolean sensOk = !sensInvolved || StringUtils.hasText(a.getInfoAuthAgreement());
            dims.add(new BatchComplianceResult.Dim("信息授权协议", sensOk,
                    !sensInvolved ? "—" : (sensOk ? "已具备(确权带出/补传)" : "涉隐私/商密,缺信息授权协议")));
            // ④ 授权范围 ≤ 确权边界 / 其他合规:复用 submit 门禁
            String reason = applyService.submitBlockReason(a.getApplyId());
            boolean scopeOk = reason == null;
            dims.add(new BatchComplianceResult.Dim("授权范围 ≤ 确权边界", scopeOk,
                    scopeOk ? "通过" : reason));
            boolean pass = cardOk && thirdOk && sensOk && scopeOk;
            checks.add(new BatchComplianceResult.ItemCheck(a.getApplyId(), a.getAssetName(), pass, dims));
            if (!pass) {
                String r = !thirdOk ? "涉第三方未提许可凭证"
                        : !sensOk ? "涉隐私/商密未提信息授权协议"
                        : !cardOk ? "缺生效权益卡片(先确后授)" : reason;
                blocked.add(new BatchComplianceResult.BlockedItem(a.getApplyId(), a.getAssetName(), r));
            }
        }
        return new BatchComplianceResult(blocked.isEmpty(), draftTotal, blocked.size(), blocked, checks);
    }

    @Override
    @Transactional
    public void approve(String batchListId) {
        BatchAuthList l = require(batchListId);
        if (!BatchAuthList.STATUS_SUBMITTED.equals(l.getListStatus())) {
            throw new BusinessException("仅申报稿可由领导小组办公室批准");
        }
        // 清单须有真实授权明细(防 itemCount 与实际脱节的空清单被终批后无协议可生成)
        List<AuthApply> items = applyService.byBatch(batchListId);
        if (items.isEmpty()) {
            throw new BusinessException("清单无授权明细,不可提交领导小组批准(请先添加授权项并提交申报稿)");
        }
        // #3 守住官方5节点链(表1 50–90):清单内所有明细须已过 合规/主管/经理/副总(到达「领导小组审批中」或终态),
        // 否则领导小组不能终批——防止从申报稿直接跳节点终批。前4节点在明细 AuthApply 审核台逐项完成。
        List<String> notReady = new ArrayList<>();
        for (AuthApply a : items) {
            String s = a.getStatus();
            boolean reachedLeadership = AuthApply.STATUS_LEADERSHIP.equals(s)
                    || AuthApply.STATUS_EFFECTIVE.equals(s) || AuthApply.STATUS_REJECTED.equals(s);
            if (!reachedLeadership) {
                notReady.add(a.getAssetName() + "(" + s + ")");
            }
        }
        if (!notReady.isEmpty()) {
            throw new BusinessException("领导小组终批前,以下明细须先在「授权审核台」走完 合规→主管→经理→副总 审核:"
                    + String.join("、", notReady));
        }
        update(batchListId, BatchAuthList.STATUS_APPROVED);
        // 注:协议自动生成不在本事务内——generateForBatch 自带 @Transactional,若在此 try/catch 调用,
        // 其内部异常会把当前物理事务标记 rollback-only,导致 approve 提交时抛 UnexpectedRollbackException。
        // 改由 Controller 在 approve 提交后再以独立事务 best-effort 生成(见 autoGenerateAgreementAfterApprove)。
    }

    /**
     * #1 批准后「形成」《运营授权协议》草案(对齐 35号文 表1 step100)。
     * 必须在 approve 事务提交「之后」单独调用:本方法不开启外层事务,generateForBatch 在自己的事务里跑,
     * 失败只记日志、不影响已提交的批准结果(幂等,已生成则复用)。
     */
    @Override
    public String autoGenerateAgreementAfterApprove(String batchListId) {
        try {
            return agreementService.generateForBatch(batchListId);
        } catch (Exception e) {
            log.warn("[批量授权] 清单 {} 批准后自动生成《运营授权协议》失败(可在协议工作台重新生成):{}",
                    batchListId, e.getMessage());
            return null;
        }
    }

    @Override
    public BatchAuthList getById(String batchListId) {
        return require(batchListId);
    }

    @Override
    public PageResult<BatchAuthList> page(long current, long size, String listYear, String listStatus) {
        LambdaQueryWrapper<BatchAuthList> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(listYear), BatchAuthList::getListYear, listYear)
                .eq(StringUtils.hasText(listStatus), BatchAuthList::getListStatus, listStatus)
                .orderByDesc(BatchAuthList::getCreateTime);
        IPage<BatchAuthList> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }

    @Override
    @Transactional
    public void delete(String batchListId) {
        BatchAuthList l = require(batchListId);
        if (!BatchAuthList.STATUS_DRAFT.equals(l.getListStatus())) {
            throw new BusinessException("仅草案清单可删除;申报稿请先撤回,已批准不可删除");
        }
        // 级联删除其下草稿明细(草案清单的明细均为草稿),再删清单
        List<AuthApply> items = applyService.byBatch(batchListId);
        for (AuthApply a : items) {
            if (AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                applyService.deleteApply(a.getApplyId());
            }
        }
        mapper.deleteById(batchListId); // 逻辑删除(@TableLogic)
    }

    @Override
    @Transactional
    public void withdraw(String batchListId) {
        BatchAuthList l = require(batchListId);
        if (!BatchAuthList.STATUS_SUBMITTED.equals(l.getListStatus())) {
            throw new BusinessException("仅申报稿可撤回为草案;草案请直接编辑/删除,已批准不可撤回");
        }
        // 在审明细退回草稿(可再编辑/再提交),清单回草案
        List<AuthApply> items = applyService.byBatch(batchListId);
        for (AuthApply a : items) {
            applyService.returnToDraft(a.getApplyId());
        }
        update(batchListId, BatchAuthList.STATUS_DRAFT);
    }

    private void update(String id, String status) {
        BatchAuthList upd = new BatchAuthList();
        upd.setBatchListId(id);
        upd.setListStatus(status);
        mapper.updateById(upd);
    }

    private BatchAuthList require(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "清单ID不能为空");
        }
        BatchAuthList l = mapper.selectById(id);
        if (l == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "批量授权清单不存在");
        }
        return l;
    }
}
