package com.csg.prm.common.evidence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.evidence.mapper.ChainEvidenceMapper;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChainEvidenceServiceImpl implements ChainEvidenceService {

    private final ChainEvidenceMapper mapper;
    private final BlockchainGateway blockchainGateway;

    public ChainEvidenceServiceImpl(ChainEvidenceMapper mapper, BlockchainGateway blockchainGateway) {
        this.mapper = mapper;
        this.blockchainGateway = blockchainGateway;
    }

    @Override
    @Transactional
    public String anchor(String bizType, String bizId, String summary, String payload) {
        return anchor(bizType, bizId, summary, payload, null, null);
    }

    @Override
    @Transactional
    public String anchor(String bizType, String bizId, String summary, String payload,
                         String provinceCode, String bureauCode) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "存证业务类型与主键不能为空");
        }
        String sm3 = Sm3Util.hashHex(payload == null ? "" : payload);
        ChainReceipt receipt = blockchainGateway.anchor(sm3);

        ChainEvidence e = new ChainEvidence();
        e.setBizType(bizType);
        e.setBizId(bizId);
        e.setSummary(summary);
        e.setSm3Hash(sm3);
        e.setChainTxHash(receipt.getTxHash());
        e.setBlockHeight(receipt.getBlockHeight());
        e.setAnchorStatus(ChainEvidence.STATUS_ANCHORED);
        e.setEvidenceTime(LocalDateTime.now());
        // 显式归口网级优先;为空则由公共字段填充器随用户上下文补全(strictInsertFill 仅填空值)
        if (StringUtils.hasText(provinceCode)) {
            e.setProvinceCode(provinceCode);
        }
        if (StringUtils.hasText(bureauCode)) {
            e.setBureauCode(bureauCode);
        }
        mapper.insert(e);
        return e.getEvidenceId();
    }

    @Override
    public boolean verify(String evidenceId, String payload) {
        ChainEvidence e = getById(evidenceId);
        String recomputed = Sm3Util.hashHex(payload == null ? "" : payload);
        return recomputed.equals(e.getSm3Hash());
    }

    @Override
    public ChainEvidence getById(String evidenceId) {
        if (!StringUtils.hasText(evidenceId)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "存证ID不能为空");
        }
        ChainEvidence e = mapper.selectById(evidenceId);
        if (e == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "存证记录不存在");
        }
        return e;
    }

    @Override
    public List<ChainEvidence> listByBiz(String bizId) {
        LambdaQueryWrapper<ChainEvidence> w = new LambdaQueryWrapper<>();
        w.eq(ChainEvidence::getBizId, bizId).orderByDesc(ChainEvidence::getCreateTime);
        return mapper.selectList(w);
    }

    @Override
    public PageResult<ChainEvidence> page(long current, long size, String bizType, String bizId) {
        LambdaQueryWrapper<ChainEvidence> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(bizType), ChainEvidence::getBizType, bizType)
                .eq(StringUtils.hasText(bizId), ChainEvidence::getBizId, bizId)
                .orderByDesc(ChainEvidence::getCreateTime);
        IPage<ChainEvidence> p = mapper.selectPage(
                new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        return PageResult.of(p);
    }
}
