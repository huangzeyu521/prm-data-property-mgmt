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
    private static final String RIGHT_OPERATION = "数据产品经营权";

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
        mapper.deleteById(applyId); // 逻辑删除(@TableLogic delFlag)
    }

    @Override
    @Transactional
    public void submit(String applyId) {
        AuthApply apply = require(applyId);
        if (!AuthApply.STATUS_DRAFT.equals(apply.getStatus())) {
            throw new BusinessException("仅草稿状态可提交");
        }
        validate(apply);
        // 先确后授:必须引用有效权益卡片(冻结/失效/未确权则拦截)
        if (!equityCardGateway.isUsable(apply.getEquityCardId())) {
            throw new BusinessException("该数据未确权或权益卡片已冻结/失效,不可发起授权(先确后授)");
        }
        // 附录F 3.4.3:数据产品经营权授权范围仅限对外开放目录中的数据资源
        if (RIGHT_OPERATION.equals(apply.getRightType()) && !openCatalogGateway.isInOpenCatalog(apply.getAssetId())) {
            throw new BusinessException("数据产品经营权授权范围仅限对外开放目录中的数据资源");
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
        // 由流程引擎启动对应模式的审批链(首环节:合规审核)
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
            return "数据产品经营权授权仅限对外开放目录中的数据资源";
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
     * 多级审批推进:按授权模式沿审批链逐级流转;最终环节通过则生成授权证书。
     * 专项:合规->业务->主管->经理->副总->已生效;批量:合规->主管->经理->副总->领导小组->已生效。
     * @return 生效(发证)时返回证书ID,否则 null
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
            // 终审通过(已生效):二次校验先确后授(防审批期间卡片被冻结)
            if (!equityCardGateway.isUsable(apply.getEquityCardId())) {
                throw new BusinessException("权益卡片已冻结/失效,授权熔断");
            }
            updateStatus(applyId, t.nextState(), t.stepIndex(), null);
            flowLogService.record(apply, from, t.nextState(), responderOf(from), op);
            String certId = authCertService.generateFromApply(apply);
            // P0-① 产权事件回写:授权生效 -> 台账更新授权状态 + 变更留痕
            ledgerWriteback.apply(com.csg.prm.common.writeback.RightsEvent.authorized(
                    apply.getAssetId(), apply.getRightType(), apply.getGranteeOrg(), applyId));
            // 授权完成 -> 触发确权服务把产权/权益结论写回平台资产卡片(非致命,失败不影响授权)
            cardWriteback.writeback(apply.getAssetId());
            return certId;
        }
        updateStatus(applyId, t.nextState(), t.stepIndex(), null);
        flowLogService.record(apply, from, t.nextState(), responderOf(from), op);
        return null;
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
