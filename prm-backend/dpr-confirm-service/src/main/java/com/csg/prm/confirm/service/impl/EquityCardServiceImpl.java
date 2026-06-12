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
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;
import com.csg.prm.confirm.mapper.EquityCardLogMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.EquityCardService;
import com.csg.prm.confirm.service.EquityCertService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EquityCardServiceImpl implements EquityCardService {

    private final EquityCardMapper mapper;
    private final EquityCardLogMapper logMapper;
    private final ChainEvidenceService chainEvidenceService;
    private final LedgerWritebackGateway ledgerWriteback;
    private final EquityCertService certService;

    public EquityCardServiceImpl(EquityCardMapper mapper, EquityCardLogMapper logMapper,
                                 ChainEvidenceService chainEvidenceService,
                                 LedgerWritebackGateway ledgerWriteback,
                                 EquityCertService certService) {
        this.mapper = mapper;
        this.logMapper = logMapper;
        this.chainEvidenceService = chainEvidenceService;
        this.ledgerWriteback = ledgerWriteback;
        this.certService = certService;
    }


    @Override
    public com.csg.prm.confirm.entity.EquityCard findByNo(String cardNo) {
        return mapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.csg.prm.confirm.entity.EquityCard>()
                .eq(com.csg.prm.confirm.entity.EquityCard::getCardNo, cardNo).last("limit 1"));
    }

    @Override
    @Transactional
    public String generateFromApply(ConfirmApply apply) {
        EquityCard card = new EquityCard();
        card.setCardNo(generateCardNo());
        card.setApplyId(apply.getApplyId());
        card.setAssetId(apply.getAssetId());
        card.setAssetName(apply.getAssetName());
        card.setRightType(apply.getRightType());
        card.setRightOwner(apply.getRightHolder());
        card.setRightSource("确权认定");
        card.setValidDate(apply.getValidDate());
        card.setCardStatus(EquityCard.STATUS_NORMAL);
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

    /** 全局唯一权益资产编码(生产可替换为雪花算法) */
    private String generateCardNo() {
        return "EC-PRA-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
