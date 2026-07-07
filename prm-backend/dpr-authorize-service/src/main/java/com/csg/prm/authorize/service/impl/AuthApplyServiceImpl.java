package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.gateway.EquityCardGateway;
import com.csg.prm.authorize.gateway.OpenCatalogGateway;
import com.csg.prm.authorize.mapper.AuthApplyMapper;
import com.csg.prm.authorize.service.AuthApplyService;
import com.csg.prm.authorize.service.AuthCertService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.workflow.FlowDefinitions;
import com.csg.prm.common.workflow.FlowTransition;
import com.csg.prm.common.workflow.ProcessFlowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class AuthApplyServiceImpl implements AuthApplyService {

    /** 产品经营权(对外开放目录校验适用) */
    private static final String RIGHT_OPERATION = "经营权";

    private final AuthApplyMapper mapper;
    private final AuthCertService authCertService;
    private final EquityCardGateway equityCardGateway;
    private final OpenCatalogGateway openCatalogGateway;
    private final com.csg.prm.common.writeback.LedgerWritebackGateway ledgerWriteback;
    private final ProcessFlowEngine flowEngine;
    private final com.csg.prm.authorize.service.AuthFlowLogService flowLogService;
    private final com.csg.prm.authorize.gateway.ConfirmWritebackGateway cardWriteback;

    public AuthApplyServiceImpl(AuthApplyMapper mapper, AuthCertService authCertService,
                               EquityCardGateway equityCardGateway, OpenCatalogGateway openCatalogGateway,
                               com.csg.prm.common.writeback.LedgerWritebackGateway ledgerWriteback,
                               ProcessFlowEngine flowEngine,
                               com.csg.prm.authorize.service.AuthFlowLogService flowLogService,
                               com.csg.prm.authorize.gateway.ConfirmWritebackGateway cardWriteback) {
        this.mapper = mapper;
        this.authCertService = authCertService;
        this.equityCardGateway = equityCardGateway;
        this.openCatalogGateway = openCatalogGateway;
        this.ledgerWriteback = ledgerWriteback;
        this.flowEngine = flowEngine;
        this.flowLogService = flowLogService;
        this.cardWriteback = cardWriteback;
    }

    // 授权审批逐节点角色门禁(对齐架构 AA-10/BA-05 与工作指引授权流程):各节点仅对应角色(及 all/admin)可审批/驳回。
    private static final java.util.Map<String, String> NODE_ROLE = java.util.Map.of(
            AuthApply.STATUS_UNIT, "unit",              // 单位初审 -> 申报单位(业务/数字化/分管领导,表2 20-50合并节点)
            AuthApply.STATUS_COMPLIANCE, "review",      // 合规审核 -> 数据产权合规管控小组
            AuthApply.STATUS_BUSINESS, "business",      // 业务审核 -> 业务管理部门团队
            AuthApply.STATUS_MANAGER, "manager",        // 主管审核 -> 数字化部主管
            AuthApply.STATUS_DIRECTOR, "director",      // 经理审核 -> 经理/高级经理
            AuthApply.STATUS_VP, "gm",                  // 副总审批 -> 副总经理/总经理
            AuthApply.STATUS_LEADERSHIP, "leadership"); // 领导小组审批 -> 领导小组办公室

    /**
     * 逐节点角色门禁:校验当前登录用户是否有权处理该节点(审批/驳回)。
     * 无用户上下文(内部调用/未启用认证/单测)或 all/admin 角色:放行;否则节点角色须匹配。
     */
    private void assertNodeRole(String status) {
        UserContext ctx = UserContextHolder.get();
        java.util.Set<String> roles = ctx == null ? null : ctx.getRoles();
        if (roles == null || roles.isEmpty() || roles.contains("all") || roles.contains("admin")) {
            return;
        }
        String need = NODE_ROLE.get(status);
        if (need != null && !roles.contains(need)) {
            throw new BusinessException(ResponseCode.FORBIDDEN.getCode(),
                    "无权限:【" + status + "】节点须由「" + responderOf(status) + "」角色处理");
        }
    }

    /** 各审批状态对应的责任人(节点处理人/角色)。 */
    private String responderOf(String status) {
        if (AuthApply.STATUS_UNIT.equals(status)) {
            return "申报单位(分管领导)";
        }
        if (AuthApply.STATUS_COMPLIANCE.equals(status)) {
            return "合规管控小组";
        }
        if (AuthApply.STATUS_BUSINESS.equals(status)) {
            return "业务部门";
        }
        if (AuthApply.STATUS_MANAGER.equals(status)) {
            return "数字化部主管";
        }
        if (AuthApply.STATUS_DIRECTOR.equals(status)) {
            return "数字化部经理";
        }
        if (AuthApply.STATUS_VP.equals(status)) {
            return "副总/总经理";
        }
        if (AuthApply.STATUS_LEADERSHIP.equals(status)) {
            return "领导小组办公室";
        }
        return "申请人";
    }

    @Override
    @Transactional
    public String saveDraft(AuthApply apply) {
        validate(apply);
        if (StringUtils.hasText(apply.getApplyId())) {
            AuthApply exist = require(apply.getApplyId());
            if (!AuthApply.STATUS_DRAFT.equals(exist.getStatus())) {
                throw new BusinessException("仅草稿状态可编辑");
            }
            mapper.updateById(apply);
            return apply.getApplyId();
        }
        apply.setStatus(AuthApply.STATUS_DRAFT);
        if (!StringUtils.hasText(apply.getAuthMode())) {
            apply.setAuthMode(AuthApply.MODE_SPECIAL);
        }
        if (!StringUtils.hasText(apply.getApplyNo())) {
            apply.setApplyNo(generateApplyNo());
        }
        mapper.insert(apply);
        return apply.getApplyId();
    }

    @Override
    @Transactional
    public void deleteApply(String applyId) {
        AuthApply apply = require(applyId);
        if (!AuthApply.STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BusinessException("仅草稿状态可删除,当前状态:" + apply.getStatus());
        }
        // 一事一议草稿=私有,仅本人可删;批量明细=清单级共享(走 BatchAuthList 级门禁),不作个人归属校验
        if (AuthApply.MODE_SPECIAL.equals(apply.getAuthMode())) {
            assertApplicant(apply);
        }
        mapper.deleteById(applyId); // 逻辑删除(@TableLogic delFlag)
    }

    @Override
    @Transactional
    public void submit(String applyId) {
        AuthApply apply = require(applyId);
        if (!AuthApply.STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BusinessException("仅草稿状态可提交");
        }
        // 一事一议草稿=私有,仅本人可提交;批量明细=清单级共享(由清单维护人统一提交),不作个人归属校验
        if (AuthApply.MODE_SPECIAL.equals(apply.getAuthMode())) {
            assertApplicant(apply);
        }
        validate(apply);
        // 先确后授:必须引用有效权益卡片(冻结/失效/未确权则拦截)
        if (!equityCardGateway.isUsable(apply.getEquityCardId())) {
            throw new BusinessException("该数据未确权或权益卡片已冻结/失效,不可发起授权(先确后授)");
        }
        // 附录F 3.4.3:经营权授权范围仅限对外开放目录中的数据资源
        if (RIGHT_OPERATION.equals(apply.getRightType()) && !openCatalogGateway.isInOpenCatalog(apply.getAssetId())) {
            throw new BusinessException("经营权授权范围仅限对外开放目录中的数据资源");
        }
        // 授权时效:无特殊说明默认两年(表5/表6)
        if (apply.getValidDate() == null) {
            apply.setValidDate(java.time.LocalDateTime.now().plusYears(2));
            AuthApply vd = new AuthApply();
            vd.setApplyId(applyId);
            vd.setValidDate(apply.getValidDate());
            mapper.updateById(vd);
        }
        // 授权范围/期限不得超出确权边界(下游 ⊆ 上游权益卡片)
        EquityCardGateway.CardBoundary b = equityCardGateway.boundary(apply.getEquityCardId());
        if (b.scope() != null && !"全字段".equals(b.scope())
                && StringUtils.hasText(apply.getScope()) && !b.scope().equals(apply.getScope())) {
            throw new BusinessException("授权范围超出确权边界(确权范围:" + b.scope() + ")");
        }
        if (apply.getValidDate() != null && b.validDate() != null && apply.getValidDate().isAfter(b.validDate())) {
            throw new BusinessException("授权期限超出确权有效期,不得超过确权边界");
        }
        // 由流程引擎启动对应模式的审批链(一事一议首环节:单位初审;批量首环节:合规审核)
        String first = flowEngine.start(flowKeyOf(apply.getAuthMode()), applyId);
        updateStatus(applyId, first, 0, null);
        flowLogService.record(apply, AuthApply.STATUS_DRAFT, first, "申请人", null);
    }

    /** 只读试跑:镜像 submit 的合规拦截条件(不改状态,validDate 为空按默认两年估算);通过返回 null。 */
    @Override
    public String submitBlockReason(String applyId) {
        AuthApply apply = require(applyId);
        if (!StringUtils.hasText(apply.getAssetId())) return "关联资产ID不能为空";
        if (!StringUtils.hasText(apply.getAssetName())) return "资产名称不能为空";
        if (!StringUtils.hasText(apply.getGranteeOrg())) return "被授权方不能为空";
        if (!equityCardGateway.isUsable(apply.getEquityCardId())) {
            return "未确权或权益卡片已冻结/失效(先确后授)";
        }
        if (RIGHT_OPERATION.equals(apply.getRightType()) && !openCatalogGateway.isInOpenCatalog(apply.getAssetId())) {
            return "经营权授权仅限对外开放目录中的数据资源";
        }
        EquityCardGateway.CardBoundary b = equityCardGateway.boundary(apply.getEquityCardId());
        if (b.scope() != null && !"全字段".equals(b.scope())
                && StringUtils.hasText(apply.getScope()) && !b.scope().equals(apply.getScope())) {
            return "授权范围超出确权边界(确权范围:" + b.scope() + ")";
        }
        java.time.LocalDateTime vd = apply.getValidDate() != null
                ? apply.getValidDate() : java.time.LocalDateTime.now().plusYears(2);
        if (b.validDate() != null && vd.isAfter(b.validDate())) {
            return "授权期限超出确权有效期,不得超过确权边界";
        }
        return null;
    }

    /**
     * 多级审批推进:按授权模式沿审批链逐级流转。
     * 专项(表2 20-100):单位初审->合规->业务->主管->经理->副总->批准(待双签);
     *   生效副作用(生效记录/台账/卡片回写)移至协议双签+承诺函归档时刻(markEffectiveAfterAgreement),先签约后执行授权。
     * 批量:合规->主管->经理->副总->领导小组->已生效(终审即生效,清单级协议另行编排)。
     * @return 批量终审生效时返回生效记录ID,否则 null
     */
    @Override
    @Transactional
    public String approve(String applyId, String opinion) {
        AuthApply apply = require(applyId);
        // 逐节点角色门禁:仅本节点对应角色(及 all/admin)可审批
        assertNodeRole(apply.getStatus());
        String flowKey = flowKeyOf(apply.getAuthMode());
        if (!flowEngine.canAdvance(flowKey, apply.getStatus())) {
            throw new BusinessException("当前状态不可审批:" + apply.getStatus());
        }
        String from = apply.getStatus();
        String op = StringUtils.hasText(opinion) ? opinion : "同意,符合授权要求";
        // 由流程引擎推进(去除硬编码 chain/indexInChain),按授权模式沿对应审批链流转
        FlowTransition t = flowEngine.advance(flowKey, applyId, apply.getStatus());
        if (t.terminal()) {
            // 终审通过:二次校验先确后授(防审批期间卡片被冻结)
            if (!equityCardGateway.isUsable(apply.getEquityCardId())) {
                throw new BusinessException("权益卡片已冻结/失效,授权熔断");
            }
            updateStatus(applyId, t.nextState(), t.stepIndex(), null);
            flowLogService.record(apply, from, t.nextState(), responderOf(from), op);
            if (AuthApply.MODE_BATCH.equals(apply.getAuthMode())) {
                // 批量终态=已生效:生效副作用就地执行
                return applyEffectiveSideEffects(apply, applyId);
            }
            // 专项终态=批准(待双签):不生效、不回写;协议双签+承诺函归档后 markEffectiveAfterAgreement 收口
            return null;
        }
        updateStatus(applyId, t.nextState(), t.stepIndex(), null);
        flowLogService.record(apply, from, t.nextState(), responderOf(from), op);
        return null;
    }

    /**
     * 协议双签+《保密承诺函》归档后收口(35号文 表2 110-130 先签约后执行授权):
     * 一事一议申请 批准->已生效,并执行生效副作用(生效记录/台账授权状态回写/资产卡片回写)。
     * 幂等:非「批准」态(已生效/已驳回/批量明细)直接返回 null 不重复处理。
     */
    @Override
    @Transactional
    public String markEffectiveAfterAgreement(String applyId) {
        // 容错:协议 applyId 悬空(存量脏数据/演示协议)不阻断协议归档,仅跳过生效收口并留日志
        AuthApply apply = mapper.selectById(applyId);
        if (apply == null) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("[授权] 协议归档收口:来源申请 {} 不存在,跳过生效标记(存量/演示数据)", applyId);
            return null;
        }
        if (!AuthApply.STATUS_APPROVED.equals(apply.getStatus())) {
            return null;
        }
        // 签约窗口期熔断复检:批准后、归档前卡片被冻结/失效则不得生效
        if (!equityCardGateway.isUsable(apply.getEquityCardId())) {
            throw new BusinessException("权益卡片已冻结/失效,授权熔断,协议不可归档生效");
        }
        updateStatus(applyId, AuthApply.STATUS_EFFECTIVE, null, null);
        flowLogService.record(apply, AuthApply.STATUS_APPROVED, AuthApply.STATUS_EFFECTIVE,
                "系统(协议双签归档)", "甲乙双签+保密承诺函齐备,协议归档,执行授权并记录");
        return applyEffectiveSideEffects(apply, applyId);
    }

    /** 生效副作用(单一真源):生成授权生效记录 + 台账授权状态回写 + 平台资产卡片回写。 */
    private String applyEffectiveSideEffects(AuthApply apply, String applyId) {
        String certId = authCertService.generateFromApply(apply);
        // P0-① 产权事件回写:授权生效 -> 台账更新授权状态 + 变更留痕
        ledgerWriteback.apply(com.csg.prm.common.writeback.RightsEvent.authorized(
                apply.getAssetId(), apply.getRightType(), apply.getGranteeOrg(), applyId));
        // 授权生效 -> 触发确权服务把产权/权益结论写回平台资产卡片(非致命,失败不影响授权)
        cardWriteback.writeback(apply.getAssetId());
        return certId;
    }

    @Override
    @Transactional
    public void reject(String applyId, String reason) {
        AuthApply apply = require(applyId);
        // 逐节点角色门禁:仅本节点对应角色(及 all/admin)可驳回
        assertNodeRole(apply.getStatus());
        // 仅审批链中(可推进)的状态可驳回——与引擎判定一致
        if (!flowEngine.canAdvance(flowKeyOf(apply.getAuthMode()), apply.getStatus())) {
            throw new BusinessException("当前状态不可驳回:" + apply.getStatus());
        }
        String from = apply.getStatus();
        updateStatus(applyId, AuthApply.STATUS_REJECTED, null, reason);
        flowLogService.record(apply, from, AuthApply.STATUS_REJECTED, responderOf(from), reason);
    }

    @Override
    public com.csg.prm.authorize.dto.BatchResult batchApprove(List<String> applyIds) {
        com.csg.prm.authorize.dto.BatchResult r = new com.csg.prm.authorize.dto.BatchResult();
        if (applyIds == null) {
            return r;
        }
        for (String id : applyIds) {
            try {
                approve(id, null);
                r.ok();
            } catch (RuntimeException e) {
                r.fail(id, e.getMessage());
            }
        }
        return r;
    }

    @Override
    public com.csg.prm.authorize.dto.BatchResult batchReject(List<String> applyIds, String reason) {
        com.csg.prm.authorize.dto.BatchResult r = new com.csg.prm.authorize.dto.BatchResult();
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

    @Override
    @Transactional
    public void withdraw(String applyId, String reason) {
        AuthApply apply = require(applyId);
        // 仅审批链活动态(可推进)可撤回;草稿(请删除)/批准/已生效/已驳回 均不可撤回
        if (!flowEngine.canAdvance(flowKeyOf(apply.getAuthMode()), apply.getStatus())) {
            throw new BusinessException("当前状态不可撤回:" + apply.getStatus() + ";草稿请删除,已批准/已生效/已驳回不可撤回");
        }
        // 撤回是申请人本人的动作(非节点审批角色门禁):校验调用者为原申请人
        assertApplicant(apply);
        String from = apply.getStatus();
        String why = StringUtils.hasText(reason) ? reason : "申请人主动撤回";
        updateStatus(applyId, AuthApply.STATUS_WITHDRAWN, null, why);
        flowLogService.record(apply, from, AuthApply.STATUS_WITHDRAWN, "申请人", why);
    }

    @Override
    @Transactional
    public void withdrawForm(String formNo) {
        List<AuthApply> rows = byForm(formNo);
        if (rows.isEmpty()) {
            throw new BusinessException("申请单不存在或无明细:" + formNo);
        }
        int done = 0;
        for (AuthApply a : rows) {
            if (flowEngine.canAdvance(flowKeyOf(a.getAuthMode()), a.getStatus())) {
                withdraw(a.getApplyId(), "整单撤回");
                done++;
            }
        }
        if (done == 0) {
            throw new BusinessException("该申请单无处于审批中的明细可撤回");
        }
    }

    @Override
    @Transactional
    public void returnToDraft(String applyId) {
        AuthApply apply = require(applyId);
        String from = apply.getStatus();
        if (AuthApply.STATUS_DRAFT.equals(from)) {
            return; // 已是草稿,幂等
        }
        // 仅审批链活动态可退回草稿;终态(已生效/批准/已驳回)跳过,不阻断批量撤回
        if (!flowEngine.canAdvance(flowKeyOf(apply.getAuthMode()), from)) {
            return;
        }
        updateStatus(applyId, AuthApply.STATUS_DRAFT, null, null);
        flowLogService.record(apply, from, AuthApply.STATUS_DRAFT, "申请人", "批量清单撤回:明细退回草稿");
    }

    /**
     * 撤回门禁:仅申请人本人可撤回(与节点审批角色相反)。
     * 无用户上下文(内部/未启用认证/单测)、admin/all 角色、或原单无 creatorId:放行;否则须 creatorId 匹配当前用户。
     */
    private void assertApplicant(AuthApply apply) {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || !StringUtils.hasText(ctx.getUserId())) {
            return;
        }
        java.util.Set<String> roles = ctx.getRoles();
        if (roles != null && (roles.contains("all") || roles.contains("admin"))) {
            return;
        }
        String creator = apply.getCreatorId();
        if (StringUtils.hasText(creator) && !creator.equals(ctx.getUserId())) {
            throw new BusinessException(ResponseCode.FORBIDDEN.getCode(), "仅申请人本人可撤回该授权申请");
        }
    }

    /**
     * 草稿可见性隔离:一事一议草稿=私有,仅创建人可见;批量明细草稿=清单级共享(不隔离);已提交及以上全量供审计。
     * 无用户上下文/未启用认证/单测,或 admin/all 角色:不隔离。
     * 条件:(status != 草稿) OR (authMode = 批量) OR (creatorId = 当前用户)。
     */
    private void applyDraftScope(LambdaQueryWrapper<AuthApply> w) {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null || !StringUtils.hasText(ctx.getUserId())) {
            return;
        }
        java.util.Set<String> roles = ctx.getRoles();
        if (roles != null && (roles.contains("all") || roles.contains("admin"))) {
            return;
        }
        String me = ctx.getUserId();
        w.and(x -> x.ne(AuthApply::getStatus, AuthApply.STATUS_DRAFT)
                .or().eq(AuthApply::getAuthMode, AuthApply.MODE_BATCH)
                .or().eq(AuthApply::getCreatorId, me));
    }

    /** 授权模式 -> 流程定义键 */
    private String flowKeyOf(String mode) {
        return AuthApply.MODE_BATCH.equals(mode)
                ? FlowDefinitions.DPR_AUTH_BATCH : FlowDefinitions.DPR_AUTH_SPECIAL;
    }

    @Override
    public AuthApply getById(String applyId) {
        return require(applyId);
    }

    @Override
    public PageResult<AuthApply> page(AuthApplyQuery query) {
        LambdaQueryWrapper<AuthApply> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getAssetName()), AuthApply::getAssetName, query.getAssetName())
                .eq(StringUtils.hasText(query.getAuthMode()), AuthApply::getAuthMode, query.getAuthMode())
                .eq(StringUtils.hasText(query.getStatus()), AuthApply::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getGranteeOrg()), AuthApply::getGranteeOrg, query.getGranteeOrg())
                .like(StringUtils.hasText(query.getApplicant()), AuthApply::getApplicantManager, query.getApplicant())
                .orderByDesc(AuthApply::getCreateTime);
        applyDraftScope(wrapper);
        IPage<AuthApply> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
    }

    @Override
    public java.util.List<AuthApply> byBatch(String batchListId) {
        if (!StringUtils.hasText(batchListId)) {
            return java.util.Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<AuthApply>()
                .eq(AuthApply::getBatchListId, batchListId)
                .orderByDesc(AuthApply::getCreateTime));
    }

    // ── 一事一议「单场景·多表」申请单(同 formNo 多张数据表) ──────────────────────

    @Override
    public String createForm() {
        return "SQF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Override
    public List<AuthApply> byForm(String formNo) {
        if (!StringUtils.hasText(formNo)) {
            return java.util.Collections.emptyList();
        }
        return mapper.selectList(new LambdaQueryWrapper<AuthApply>()
                .eq(AuthApply::getFormNo, formNo)
                .orderByDesc(AuthApply::getCreateTime));
    }

    @Override
    @Transactional
    public void submitForm(String formNo) {
        List<AuthApply> rows = byForm(formNo);
        if (rows.isEmpty()) {
            throw new BusinessException("申请单为空,请先加入数据表");
        }
        int submitted = 0;
        for (AuthApply a : rows) {
            if (!AuthApply.STATUS_DRAFT.equals(a.getStatus())) {
                continue; // 已提交/流转中的行跳过
            }
            try {
                submit(a.getApplyId()); // 复用单行流程:合规→业务→主管→经理→副总;同一事务,失败整单回滚
            } catch (BusinessException e) {
                throw new BusinessException("数据表「" + a.getAssetName() + "」提交失败:" + e.getMessage());
            }
            submitted++;
        }
        if (submitted == 0) {
            throw new BusinessException("无可提交的草稿数据表(可能已提交)");
        }
    }

    /**
     * 申请单逐表自检(只读试跑):硬门禁与批量 checkBatchCompliance 同源(先确后授卡片/第三方凭证/信息授权协议/范围⊆确权边界);
     * 利益分配/安全保障为附录D §3.4.4 协议要素提示(警告,不入硬门禁,与单行 runCheck 口径一致)。
     */
    @Override
    public com.csg.prm.authorize.dto.BatchComplianceResult checkFormCompliance(String formNo) {
        List<AuthApply> rows = byForm(formNo);
        java.util.List<com.csg.prm.authorize.dto.BatchComplianceResult.ItemCheck> items = new java.util.ArrayList<>();
        java.util.List<com.csg.prm.authorize.dto.BatchComplianceResult.BlockedItem> blocked = new java.util.ArrayList<>();
        for (AuthApply a : rows) {
            java.util.List<com.csg.prm.authorize.dto.BatchComplianceResult.Dim> dims = new java.util.ArrayList<>();
            // 硬门禁 4 维(与批量同源)
            boolean cardOk = StringUtils.hasText(a.getEquityCardId());
            dims.add(new com.csg.prm.authorize.dto.BatchComplianceResult.Dim("先确后授·生效卡片", cardOk,
                    cardOk ? a.getEquityCardId() : "缺生效权益卡片(请先完成确权)"));
            boolean thirdInvolved = StringUtils.hasText(a.getThirdPartySource());
            boolean thirdOk = !thirdInvolved || StringUtils.hasText(a.getThirdPartyLicense());
            dims.add(new com.csg.prm.authorize.dto.BatchComplianceResult.Dim("第三方许可凭证", thirdOk,
                    !thirdInvolved ? "—" : (thirdOk ? "已具备(确权带出/补传)" : "涉第三方,缺许可凭证")));
            boolean sensInvolved = StringUtils.hasText(a.getSensitiveType()) && !"无".equals(a.getSensitiveType());
            boolean sensOk = !sensInvolved || StringUtils.hasText(a.getInfoAuthAgreement());
            dims.add(new com.csg.prm.authorize.dto.BatchComplianceResult.Dim("信息授权协议", sensOk,
                    !sensInvolved ? "—" : (sensOk ? "已具备(确权带出/补传)" : "涉隐私/商密,缺信息授权协议")));
            String reason = submitBlockReason(a.getApplyId());
            boolean scopeOk = reason == null;
            dims.add(new com.csg.prm.authorize.dto.BatchComplianceResult.Dim("授权范围 ≤ 确权边界", scopeOk,
                    scopeOk ? "通过" : reason));
            // 协议要素提示(警告,不阻断:附录D §3.4.4,协议签订前补齐)
            boolean benefitOk = StringUtils.hasText(a.getBenefitAllocation());
            dims.add(new com.csg.prm.authorize.dto.BatchComplianceResult.Dim("利益分配约定(§3.4.4·建议)", benefitOk,
                    benefitOk ? "已约定" : "未填(协议签订前须补充)"));
            boolean secOk = StringUtils.hasText(a.getSecurityReq());
            dims.add(new com.csg.prm.authorize.dto.BatchComplianceResult.Dim("安全保障要求(§3.4.4·建议)", secOk,
                    secOk ? "已约定" : "未填(协议签订前须补充)"));

            boolean pass = cardOk && thirdOk && sensOk && scopeOk; // 仅 4 维硬门禁决定可否提交
            items.add(new com.csg.prm.authorize.dto.BatchComplianceResult.ItemCheck(a.getApplyId(), a.getAssetName(), pass, dims));
            if (!pass) {
                String r = !thirdOk ? "涉第三方未提许可凭证"
                        : !sensOk ? "涉隐私/商密未提信息授权协议"
                        : !cardOk ? "缺生效权益卡片(先确后授)" : reason;
                blocked.add(new com.csg.prm.authorize.dto.BatchComplianceResult.BlockedItem(a.getApplyId(), a.getAssetName(), r));
            }
        }
        boolean allPass = blocked.isEmpty() && !rows.isEmpty();
        return new com.csg.prm.authorize.dto.BatchComplianceResult(allPass, rows.size(), blocked.size(), blocked, items);
    }

    private void updateStatus(String applyId, String status, Integer node, String rejectReason) {
        AuthApply upd = new AuthApply();
        upd.setApplyId(applyId);
        upd.setStatus(status);
        upd.setCurrentNode(node);
        upd.setRejectReason(rejectReason);
        mapper.updateById(upd);
    }

    private AuthApply require(String applyId) {
        if (!StringUtils.hasText(applyId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "申请ID不能为空");
        }
        AuthApply apply = mapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "授权申请不存在");
        }
        return apply;
    }

    private void validate(AuthApply apply) {
        if (apply == null) {
            throw new BusinessException(ResponseCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(apply.getAssetId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "关联资产ID不能为空");
        }
        if (!StringUtils.hasText(apply.getAssetName())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "资产名称不能为空");
        }
        if (!StringUtils.hasText(apply.getGranteeOrg())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "被授权方不能为空");
        }
        if (!StringUtils.hasText(apply.getRightType())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "授权权益类型不能为空");
        }
        if (!StringUtils.hasText(apply.getEquityCardId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "必须引用权益卡片(先确后授)");
        }
    }

    private String generateApplyNo() {
        return "SQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
