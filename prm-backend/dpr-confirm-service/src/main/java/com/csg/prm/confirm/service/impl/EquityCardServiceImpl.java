package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.evidence.ChainEvidenceService;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
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

    public EquityCardServiceImpl(EquityCardMapper mapper, EquityCardLogMapper logMapper,
                                 ChainEvidenceService chainEvidenceService,
                                 LedgerWritebackGateway ledgerWriteback,
                                 EquityCertService certService,
                                 ConfirmTableItemMapper tableItemMapper) {
        this.mapper = mapper;
        this.logMapper = logMapper;
        this.chainEvidenceService = chainEvidenceService;
        this.ledgerWriteback = ledgerWriteback;
        this.certService = certService;
        this.tableItemMapper = tableItemMapper;
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
        // 确权变更:取代该资产+权利的前序"正常"卡片,保证"当前有效权益"唯一并形成版本链
        boolean isChange = Boolean.TRUE.equals(apply.getReConfirm())
                || "确权变更".equals(apply.getRegisterType());
        int newVersion = 1;
        String supersededNo = null;
        if (isChange) {
            List<EquityCard> priors = mapper.selectList(new LambdaQueryWrapper<EquityCard>()
                    .eq(EquityCard::getAssetId, apply.getAssetId())
                    .eq(EquityCard::getRightType, apply.getRightType())
                    .eq(EquityCard::getCardStatus, EquityCard.STATUS_NORMAL)
                    .orderByDesc(EquityCard::getVersion));
            for (EquityCard prior : priors) {
                int pv = prior.getVersion() == null ? 1 : prior.getVersion();
                if (pv + 1 > newVersion) newVersion = pv + 1;
                if (supersededNo == null) supersededNo = prior.getCardNo(); // 版本最高者为直接前序
                transition(prior.getCardId(), prior.getCardStatus(), EquityCard.STATUS_INVALID,
                        "被取代", "确权变更生成新版,本卡失效被取代");
            }
        }
        EquityCard card = new EquityCard();
        card.setCardNo(generateCardNo());
        card.setApplyId(apply.getApplyId());
        card.setAssetId(apply.getAssetId());
        card.setAssetName(apply.getAssetName());
        card.setRightType(apply.getRightType());
        card.setRightOwner(apply.getRightHolder());
        card.setRightSource("确权认定");
        card.setScope(deriveScope(apply.getApplyId()));
        card.setValidDate(apply.getValidDate());
        card.setCardStatus(EquityCard.STATUS_NORMAL);
        card.setVersion(newVersion);
        card.setSupersededCardNo(supersededNo);
        // 权益归集原则(F指导书):确权时网公司直接取得权益、直接归属中国南方电网有限责任公司
        //(五所口径:不存在"分省先确权再转让"的动作,确权即直接确给网公司)
        card.setConsolidatedUnit("中国南方电网有限责任公司");
        mapper.insert(card);
        recordLog(card.getCardId(), "生成", null, EquityCard.STATUS_NORMAL, "确权终审通过自动制卡");
        // 关键节点上链存证(确权制卡):SM3 指纹锚定上链,防篡改、可追溯
        chainEvidenceService.anchor("确权制卡", card.getCardId(),
                "权益卡片 " + card.getCardNo() + " / " + card.getAssetName(),
                String.join("|", card.getCardNo(), card.getApplyId(), card.getAssetId(),
                        card.getRightType(), card.getRightOwner() == null ? "" : card.getRightOwner()));
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
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "权益卡片不存在");
        }
        return card;
    }

    @Override
    @Transactional
    public void freeze(String cardId) {
        EquityCard cur = getById(cardId);
        if (EquityCard.STATUS_INVALID.equals(cur.getCardStatus())) {
            throw new BizException("已注销卡片不可冻结");
        }
        transition(cardId, cur.getCardStatus(), EquityCard.STATUS_FROZEN, "冻结", "风险/争议冻结");
    }

    @Override
    @Transactional
    public void unfreeze(String cardId) {
        EquityCard cur = getById(cardId);
        if (!EquityCard.STATUS_FROZEN.equals(cur.getCardStatus())) {
            throw new BizException("仅冻结状态可解冻");
        }
        transition(cardId, cur.getCardStatus(), EquityCard.STATUS_NORMAL, "解冻", "风险解除恢复");
    }

    @Override
    @Transactional
    public void revoke(String cardId, String reason) {
        EquityCard cur = getById(cardId);
        if (EquityCard.STATUS_INVALID.equals(cur.getCardStatus())) {
            throw new BizException("卡片已注销");
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
    public PageResult<EquityCard> page(PageQuery query) {
        LambdaQueryWrapper<EquityCard> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(EquityCard::getCreateTime);
        IPage<EquityCard> page = mapper.selectPage(query.toPage(), wrapper);
        return PageResult.of(page);
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

    /** 全局唯一权益资产编码(生产可替换为雪花算法) */
    private String generateCardNo() {
        return "EC-PRA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
