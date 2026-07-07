package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.org.Jurisdiction;
import com.csg.prm.common.org.OrgService;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.common.writeback.LedgerWritebackGateway;
import com.csg.prm.common.writeback.RightsEvent;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmTableItem;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;
import com.csg.prm.confirm.mapper.ConfirmTableItemMapper;
import com.csg.prm.confirm.mapper.EquityCardLogMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.EquityCardService;
import com.csg.prm.confirm.service.EquityCertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class EquityCardServiceImpl implements EquityCardService {

    /** 确权范围口径:授权侧据此判定"授权范围⊆确权边界" */
    private static final String SCOPE_FULL = "全字段";
    private static final String SCOPE_PARTIAL = "约定字段";

    private final EquityCardMapper mapper;
    private final EquityCardLogMapper logMapper;
    private final ChainEvidenceService chainEvidenceService;
    private final LedgerWritebackGateway ledgerWriteback;
    private final EquityCertService certService;
    private final ConfirmTableItemMapper tableItemMapper;
    private final OrgService orgService;

    public EquityCardServiceImpl(EquityCardMapper mapper, EquityCardLogMapper logMapper,
                                 ChainEvidenceService chainEvidenceService,
                                 LedgerWritebackGateway ledgerWriteback,
                                 EquityCertService certService,
                                 ConfirmTableItemMapper tableItemMapper,
                                 OrgService orgService) {
        this.mapper = mapper;
        this.logMapper = logMapper;
        this.chainEvidenceService = chainEvidenceService;
        this.ledgerWriteback = ledgerWriteback;
        this.certService = certService;
        this.tableItemMapper = tableItemMapper;
        this.orgService = orgService;
    }

    /** 归口网级:按权属人(权属单位)优先解析,退回责任部门;供卡片/存证 province_code/bureau_code 回填。 */
    private Jurisdiction jurisdictionOf(ConfirmApply apply) {
        Jurisdiction j = orgService.resolve(apply.getRightHolder());
        if (j.isEmpty()) {
            j = orgService.resolve(apply.getRespDept());
        }
        return j;
    }

    /**
     * 确权范围:确权列了表级清单(M02 库表级)=只确了约定字段;否则=整资产全字段。
     * 授权侧 HttpEquityCardGateway.boundary 读此值,使"授权范围⊆确权边界"在生产真正生效。
     */
    private String deriveScope(String applyId) {
        Long n = tableItemMapper.selectCount(new LambdaQueryWrapper<ConfirmTableItem>()
                .eq(ConfirmTableItem::getApplyId, applyId));
        return (n != null && n > 0) ? SCOPE_PARTIAL : SCOPE_FULL;
    }


    @Override
    public com.csg.prm.confirm.entity.EquityCard findByNo(String cardNo) {
        return mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.csg.prm.confirm.entity.EquityCard>()
                .eq(com.csg.prm.confirm.entity.EquityCard::getCardNo, cardNo).last("limit 1"));
    }

    @Override
    @Transactional
    public String generateFromApply(ConfirmApply apply) {
        // 制卡颗粒(对齐附录F 表4《数据权益内部管理汇总表》逐"库表×权属"一行):
        //   ① 有表级清单(系统级确权)→ 逐(库表 × 单一权属)制卡,权益卡片打在每一张数据资产卡片(库表)上;
        //   ② 无表级清单(单卡/旧数据/测试)→ 退回系统级一卡一权(tableCode=null),保持向后兼容。
        // 多权拼接串(如"持有、使用、经营")按「、,，」拆分,每权一卡,与授权侧 rightType 精确匹配一致。
        boolean isChange = Boolean.TRUE.equals(apply.getReConfirm())
                || "确权变更".equals(apply.getRegisterType());
        Jurisdiction jur = jurisdictionOf(apply);
        List<String> rights = parseRights(apply.getRightType());
        List<com.csg.prm.confirm.entity.ConfirmTableItem> items = tableItemMapper.selectList(
                new LambdaQueryWrapper<com.csg.prm.confirm.entity.ConfirmTableItem>()
                        .eq(com.csg.prm.confirm.entity.ConfirmTableItem::getApplyId, apply.getApplyId()));
        String sysName = apply.getAssetName();
        String firstCardId = null;
        if (items.isEmpty()) {
            // 系统级回退:tableCode=null,scope 由表项数派生(此处为 0 → 全量)
            String scope = deriveScope(apply.getApplyId());
            for (String right : rights) {
                String id = issueCardForRight(apply, null, null, sysName, scope, right, jur, isChange);
                if (firstCardId == null) {
                    firstCardId = id;
                }
            }
        } else {
            for (com.csg.prm.confirm.entity.ConfirmTableItem it : items) {
                String tname = StringUtils.hasText(it.getTableName()) ? it.getTableName() : it.getTableCode();
                String scope = sysName + " / " + tname;  // 库表级确权范围
                for (String right : rights) {
                    String id = issueCardForRight(apply, it, it.getTableCode(), tname, scope, right, jur, isChange);
                    if (firstCardId == null) {
                        firstCardId = id;
                    }
                }
            }
        }
        return firstCardId;
    }

    /** 权属类型拆分为逐权列表(「、,，」分隔,去空去重保序);无权属类型则不制卡。 */
    private List<String> parseRights(String rightType) {
        java.util.LinkedHashSet<String> rights = new java.util.LinkedHashSet<>();
        if (StringUtils.hasText(rightType)) {
            for (String r : rightType.split("[、,，]")) {
                String t = r.trim();
                if (!t.isEmpty()) {
                    rights.add(t);
                }
            }
        }
        return new java.util.ArrayList<>(rights);
    }

    /** 权益内容摘要(表4):按权属类型 + 该库表来源约束派生。 */
    private String rightsContentOf(String rightType, String assetName, com.csg.prm.confirm.entity.ConfirmTableItem item) {
        String base;
        if (rightType.contains("持有")) {
            base = "对「" + assetName + "」享有持有权(系统建设投入形成,依法持有、管理、处置)";
        } else if (rightType.contains("使用") || rightType.contains("加工")) {
            base = "对「" + assetName + "」享有使用权(在确权约束与授权范围内加工使用)";
        } else if (rightType.contains("经营") || rightType.contains("产品")) {
            base = "对「" + assetName + "」享有经营权(对外经营依公司对外开放目录与授权)";
        } else {
            base = rightType;
        }
        if (item != null && StringUtils.hasText(item.getSourceDesc())) {
            base += ";来源/约束:" + item.getSourceDesc();
        }
        return base;
    }

    /** 权益凭证附件或说明(表4):确权认定资料,涉第三方/非自行生产时附第三方权益证明说明。 */
    private String rightsCredentialOf(ConfirmApply apply, com.csg.prm.confirm.entity.ConfirmTableItem item) {
        StringBuilder sb = new StringBuilder("确权认定资料(确权单 ")
                .append(StringUtils.hasText(apply.getApplyNo()) ? apply.getApplyNo() : apply.getApplyId()).append(")");
        boolean thirdParty = false;
        if (item != null) {
            String st = item.getSourceType();
            boolean nonSelfSource = StringUtils.hasText(st) && "BCDEF".indexOf(st.trim().charAt(0)) >= 0;
            thirdParty = nonSelfSource || "是".equals(item.getHFlag()) || "是".equals(item.getIFlag()) || "是".equals(item.getJFlag());
        }
        if (thirdParty) {
            sb.append(";含第三方权益证明/授权说明");
        }
        return sb.toString();
    }

    /**
     * 单一权益制卡(库表级):卡片粒度=(系统 assetId × 库表 tableCode × 权属 rightType);
     * 版本链按"该资产+该库表+该权利"独立递增/取代;tableCode=null 为系统级回退卡。一卡一权,授权侧精确命中。
     */
    private String issueCardForRight(ConfirmApply apply, com.csg.prm.confirm.entity.ConfirmTableItem item,
                                     String tableCode, String assetName,
                                     String scope, String rightType, Jurisdiction jur, boolean isChange) {
        int newVersion = 1;
        String supersededNo = null;
        if (isChange) {
            // 前序卡含"正常"与"冻结"两态:冻结也是该(资产+库表+权利)的现存卡,变更生效须一并取代并参与版本递增,
            // 否则冻结卡被漏算→新卡版本号与冻结卡撞号、且冻结卡未失效,出现两张同号当前卡(唯一性破坏)
            LambdaQueryWrapper<EquityCard> pw = new LambdaQueryWrapper<EquityCard>()
                    .eq(EquityCard::getAssetId, apply.getAssetId())
                    .eq(EquityCard::getRightType, rightType)
                    .in(EquityCard::getCardStatus, EquityCard.STATUS_NORMAL, EquityCard.STATUS_FROZEN)
                    .orderByDesc(EquityCard::getVersion);
            // 库表维度精确取代:同库表(含系统级回退卡 tableCode IS NULL)的前序卡才参与版本链
            if (tableCode == null) {
                pw.isNull(EquityCard::getTableCode);
            } else {
                pw.eq(EquityCard::getTableCode, tableCode);
            }
            List<EquityCard> priors = mapper.selectList(pw);
            for (EquityCard prior : priors) {
                int pv = prior.getVersion() == null ? 1 : prior.getVersion();
                if (pv + 1 > newVersion) newVersion = pv + 1;
                if (supersededNo == null) supersededNo = prior.getCardNo(); // 版本最高者为直接前序
                transition(prior.getCardId(), prior.getCardStatus(), EquityCard.STATUS_INVALID,
                        "被取代", "确权变更生成新版,本卡失效被取代");
            }
        }
        // 归口网级回填:按申报组织(权属人/责任部门)解析省/地市码,落到卡片与制卡存证(补此前的 null)
        EquityCard card = new EquityCard();
        card.setCardNo(generateCardNo());
        card.setApplyId(apply.getApplyId());
        card.setAssetId(apply.getAssetId());
        card.setAssetName(assetName);      // 库表名(库表级)/系统名(系统级回退)
        card.setTableCode(tableCode);
        card.setRightType(rightType);
        card.setRightOwner(apply.getRightHolder());
        card.setRightSource("确权认定");
        // 表4《数据权益内部管理汇总表》权益要素逐(库表×权属)填充
        card.setSchemaName(item != null ? item.getSchemaName() : null);     // 模式名称
        card.setRightsContent(rightsContentOf(rightType, assetName, item));  // 权益内容摘要
        card.setRightsCredential(rightsCredentialOf(apply, item));           // 权益凭证附件或说明
        card.setAcquireMode("认定");                                          // 权益取得方式:确权认定(表4 A 认定)
        card.setAuthorizingUnit(null);                                       // 认定取得不填授权单位(授权取得才填)
        card.setConfirmTime(java.time.LocalDateTime.now());                  // 确权时间(= 制卡时间)
        card.setScope(scope);
        // 权益期限口径(35号文附录 表3):认定取得→无固定期限(null);授权取得→确权时间起默认两年;
        // 显式约定(如确权变更「权益到期」续期填入的新期限)优先。制卡恒为「认定」→常态无固定期限。
        card.setValidDate(resolveValidDate(card.getAcquireMode(), card.getConfirmTime(), apply.getValidDate()));
        card.setCardStatus(EquityCard.STATUS_NORMAL);
        card.setVersion(newVersion);
        card.setSupersededCardNo(supersededNo);
        // 权益归集原则(F指导书):确权时网公司直接取得权益、直接归属中国南方电网有限责任公司
        //(五所口径:不存在"分省先确权再转让"的动作,确权即直接确给网公司)
        card.setConsolidatedUnit("中国南方电网有限责任公司");
        if (StringUtils.hasText(jur.provinceCode())) {
            card.setProvinceCode(jur.provinceCode());
        }
        if (StringUtils.hasText(jur.bureauCode())) {
            card.setBureauCode(jur.bureauCode());
        }
        mapper.insert(card);
        recordLog(card.getCardId(), "生成", null, EquityCard.STATUS_NORMAL, "确权终审通过自动制卡");
        // 关键节点上链存证(确权制卡):SM3 指纹锚定上链,防篡改、可追溯;带归口网级省/地市码
        chainEvidenceService.anchor("确权制卡", card.getCardId(),
                "权益卡片 " + card.getCardNo() + " / " + card.getAssetName(),
                String.join("|", card.getCardNo(), card.getApplyId(), card.getAssetId(),
                        card.getRightType(), card.getRightOwner() == null ? "" : card.getRightOwner()),
                jur.provinceCode(), jur.bureauCode());
        // P0-① 产权事件回写:确权制卡 -> 台账更新确权状态/卡片 + 变更留痕(实时一致)
        ledgerWriteback.apply(RightsEvent.confirmed(card.getAssetId(), card.getAssetName(),
                card.getRightType(), card.getRightOwner(), card.getCardNo(), apply.getApplyId()));
        // 制卡后按权益类型自动签发标准化权益证书(可研 -006 自动生成,与卡片一一对应)
        certService.autoIssueForCard(card);
        return card.getCardId();
    }

    @Override
    public EquityCard getById(String cardId) {
        EquityCard card = mapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "权益卡片不存在");
        }
        return card;
    }

    @Override
    @Transactional
    public void freeze(String cardId) {
        EquityCard cur = getById(cardId);
        if (EquityCard.STATUS_INVALID.equals(cur.getCardStatus())) {
            throw new BusinessException("已注销卡片不可冻结");
        }
        transition(cardId, cur.getCardStatus(), EquityCard.STATUS_FROZEN, "冻结", "风险/争议冻结");
    }

    @Override
    @Transactional
    public void unfreeze(String cardId) {
        EquityCard cur = getById(cardId);
        if (!EquityCard.STATUS_FROZEN.equals(cur.getCardStatus())) {
            throw new BusinessException("仅冻结状态可解冻");
        }
        transition(cardId, cur.getCardStatus(), EquityCard.STATUS_NORMAL, "解冻", "风险解除恢复");
    }

    @Override
    @Transactional
    public void revoke(String cardId, String reason) {
        EquityCard cur = getById(cardId);
        if (EquityCard.STATUS_INVALID.equals(cur.getCardStatus())) {
            throw new BusinessException("卡片已注销");
        }
        transition(cardId, cur.getCardStatus(), EquityCard.STATUS_INVALID, "注销",
                reason == null ? "权属灭失/确权撤销" : reason);
    }

    @Override
    public EquityCard findCurrentValid(String assetId, String rightType) {
        if (!StringUtils.hasText(assetId)) {
            return null;
        }
        return mapper.selectOne(new LambdaQueryWrapper<EquityCard>()
                .eq(EquityCard::getAssetId, assetId)
                .eq(StringUtils.hasText(rightType), EquityCard::getRightType, rightType)
                .eq(EquityCard::getCardStatus, EquityCard.STATUS_NORMAL)
                .orderByDesc(EquityCard::getVersion)
                .last("LIMIT 1"));
    }

    @Override
    public List<EquityCardLog> listLogs(String cardId) {
        LambdaQueryWrapper<EquityCardLog> w = new LambdaQueryWrapper<>();
        w.eq(EquityCardLog::getCardId, cardId).orderByDesc(EquityCardLog::getCreateTime);
        return logMapper.selectList(w);
    }

    private void transition(String cardId, String from, String to, String action, String reason) {
        EquityCard upd = new EquityCard();
        upd.setCardId(cardId);
        upd.setCardStatus(to);
        mapper.updateById(upd);
        recordLog(cardId, action, from, to, reason);
    }

    private void recordLog(String cardId, String action, String from, String to, String reason) {
        EquityCardLog log = new EquityCardLog();
        log.setCardId(cardId);
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setReason(reason);
        logMapper.insert(log);
    }

    @Override
    public PageResult<EquityCard> page(com.csg.prm.confirm.dto.EquityCardQuery query) {
        IPage<EquityCard> page = mapper.selectPage(query.toPage(), cardWrapper(query));
        return PageResult.of(page);
    }

    /** 权益卡片多维过滤(库表级):系统名(assetId like SYS:系统名)/库表名(assetName like)/状态(eq)/权属类型(like 短名命中全名)。 */
    private LambdaQueryWrapper<EquityCard> cardWrapper(com.csg.prm.confirm.dto.EquityCardQuery q) {
        LambdaQueryWrapper<EquityCard> w = new LambdaQueryWrapper<>();
        w.like(org.springframework.util.StringUtils.hasText(q.getSysName()), EquityCard::getAssetId, q.getSysName())
                .like(org.springframework.util.StringUtils.hasText(q.getTableName()), EquityCard::getAssetName, q.getTableName())
                .eq(org.springframework.util.StringUtils.hasText(q.getCardStatus()), EquityCard::getCardStatus, q.getCardStatus())
                .like(org.springframework.util.StringUtils.hasText(q.getRightType()), EquityCard::getRightType, q.getRightType())
                .orderByDesc(EquityCard::getCreateTime);
        return w;
    }

    @Override
    public com.csg.prm.confirm.dto.EquityCardStats stats(com.csg.prm.confirm.dto.EquityCardQuery query) {
        // 概览忽略 status 过滤(否则分布退化):克隆查询清空 status,按系统名/权属类型聚合
        com.csg.prm.confirm.dto.EquityCardQuery q = new com.csg.prm.confirm.dto.EquityCardQuery();
        q.setSysName(query.getSysName());
        q.setTableName(query.getTableName());
        q.setRightType(query.getRightType());
        List<EquityCard> all = mapper.selectList(cardWrapper(q));
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime due = now.plusDays(90);
        com.csg.prm.confirm.dto.EquityCardStats s = new com.csg.prm.confirm.dto.EquityCardStats();
        s.setTotal(all.size());
        for (EquityCard c : all) {
            String st = c.getCardStatus();
            if (EquityCard.STATUS_NORMAL.equals(st)) {
                s.setNormal(s.getNormal() + 1);
                if (c.getValidDate() != null && !c.getValidDate().isAfter(due) && c.getValidDate().isAfter(now)) {
                    s.setDueSoon(s.getDueSoon() + 1);
                }
            } else if (EquityCard.STATUS_FROZEN.equals(st)) {
                s.setFrozen(s.getFrozen() + 1);
            } else if (EquityCard.STATUS_INVALID.equals(st)) {
                s.setExpired(s.getExpired() + 1);
            }
        }
        return s;
    }

    @Override
    public List<EquityCard> listReConfirmDue(int daysAhead) {
        java.time.LocalDateTime threshold = java.time.LocalDateTime.now()
                .plusDays(daysAhead <= 0 ? 90 : daysAhead);
        return mapper.selectList(new LambdaQueryWrapper<EquityCard>()
                .eq(EquityCard::getCardStatus, EquityCard.STATUS_NORMAL)
                .isNotNull(EquityCard::getValidDate)
                .le(EquityCard::getValidDate, threshold)
                .orderByAsc(EquityCard::getValidDate));
    }

    /**
     * 权益期限(有效期至)口径 —— 严格对齐 35 号文附录 表3《数据确权登记表》「权益期限」列:
     * <ul>
     *   <li>认定取得(A) → 无固定期限:返回 {@code null},前端渲染为「长期」;不参与季度到期扫描。</li>
     *   <li>授权取得(B) → 默认两年:自确权时间起算 {@code confirmTime + 2 年}(表头注释「授权方式取得默认为两年」)。</li>
     *   <li>显式约定优先:确权变更「权益到期」续期等场景填入的期限({@code explicit})直接生效,覆盖上述默认。</li>
     * </ul>
     */
    private java.time.LocalDateTime resolveValidDate(String acquireMode, java.time.LocalDateTime confirmTime,
                                                     java.time.LocalDateTime explicit) {
        if (explicit != null) {
            return explicit;                                   // 特殊说明 / 续期:显式期限优先
        }
        if ("授权".equals(acquireMode)) {                       // 授权取得:默认两年,从确权时间起算
            java.time.LocalDateTime base = confirmTime != null ? confirmTime : java.time.LocalDateTime.now();
            return base.plusYears(2);
        }
        return null;                                           // 认定取得:无固定期限(长期)
    }

    /** 全局唯一权益资产编码(生产可替换为雪花算法) */
    private String generateCardNo() {
        return "EC-PRA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
