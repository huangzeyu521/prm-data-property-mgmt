package com.csg.prm.confirm.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.confirm.entity.ConfirmAiRunLog;
import com.csg.prm.confirm.mapper.ConfirmAiRunLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 大模型校验操作留痕服务(南网"全流程留痕追溯"):每次确权内生 AI 调用逐条落库,
 * 计算输出 SM3 指纹(防篡改)、记录触发人/模型/耗时,供「AI 校验过程回放」与审计。
 */
@Service
public class ConfirmAiRunLogService {

    private static final int INPUT_SUMMARY_MAX = 1990;

    private final ConfirmAiRunLogMapper mapper;

    public ConfirmAiRunLogService(ConfirmAiRunLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 逐次留痕一次 AI 调用。output 计 SM3(防篡改),触发人取当前登录上下文(无则 system)。
     * 留痕失败不应影响主流程,故内部吞异常仅返回 null(由调用方按需忽略)。
     */
    @Transactional
    public ConfirmAiRunLog record(String applyId, String capability, String model,
                                  String inputSummary, String output, long durationMs) {
        try {
            ConfirmAiRunLog log = new ConfirmAiRunLog();
            log.setApplyId(applyId);
            log.setCapability(capability);
            log.setModel(model);
            log.setInputSummary(trim(inputSummary));
            log.setOutput(output);
            log.setDurationMs(durationMs);
            log.setSm3Hash(StringUtils.hasText(output) ? Sm3Util.hashHex(output) : null);
            log.setTriggerUser(currentUser());
            mapper.insert(log);
            return log;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 按申请取 AI 操作时间线(回放,按时间升序)。 */
    public List<ConfirmAiRunLog> listByApply(String applyId) {
        return mapper.selectList(new LambdaQueryWrapper<ConfirmAiRunLog>()
                .eq(ConfirmAiRunLog::getApplyId, applyId)
                .orderByAsc(ConfirmAiRunLog::getCreateTime));
    }

    private String currentUser() {
        UserContext uc = UserContextHolder.get();
        return uc != null && StringUtils.hasText(uc.getUserName()) ? uc.getUserName() : "system";
    }

    private String trim(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > INPUT_SUMMARY_MAX ? s.substring(0, INPUT_SUMMARY_MAX) : s;
    }
}
