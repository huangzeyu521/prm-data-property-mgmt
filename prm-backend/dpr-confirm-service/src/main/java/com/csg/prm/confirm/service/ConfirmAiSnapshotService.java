package com.csg.prm.confirm.service;

import com.csg.prm.common.aitrace.AiSnapshotService;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 确权 AI 校验快照(服务端化 + 防篡改):薄封装,复用 prm-common 跨域共享的 {@link AiSnapshotService}
 * 计 SM3 + 上链 + 关联留痕,封装结果存入确权申请单的 CEC_AI_SNAPSHOT 列。
 */
@Service
public class ConfirmAiSnapshotService {

    public static final String EVIDENCE_TYPE = "确权AI校验快照";

    private final ConfirmApplyMapper applyMapper;
    private final AiSnapshotService aiSnapshotService;

    public ConfirmAiSnapshotService(ConfirmApplyMapper applyMapper, AiSnapshotService aiSnapshotService) {
        this.applyMapper = applyMapper;
        this.aiSnapshotService = aiSnapshotService;
    }

    /** 固化防篡改快照并存入确权申请单。 */
    @Transactional
    public void save(String applyId, String clientJson) {
        ConfirmApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException("确权申请不存在");
        }
        String sealed = aiSnapshotService.seal(EVIDENCE_TYPE, applyId, apply.getAssetName(), clientJson);
        ConfirmApply upd = new ConfirmApply();
        upd.setApplyId(applyId);
        upd.setAiSnapshot(sealed);
        applyMapper.updateById(upd);
    }

    /** 校验已固化快照的完整性(重算 SM3 比对存证)。 */
    public Map<String, Object> verify(String applyId) {
        ConfirmApply apply = applyMapper.selectById(applyId);
        Map<String, Object> r = aiSnapshotService.verify(apply == null ? null : apply.getAiSnapshot());
        r.put("applyId", applyId);
        return r;
    }
}
