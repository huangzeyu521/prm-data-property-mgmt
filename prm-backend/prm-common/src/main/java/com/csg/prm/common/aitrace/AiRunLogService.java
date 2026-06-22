package com.csg.prm.common.aitrace;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.aitrace.mapper.AiRunLogMapper;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.crypto.Sm3Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 大模型校验操作留痕服务(跨域共享:确权 + 授权)。每次内生 AI 调用逐条落库,
 * 计算输出 SM3 指纹(防篡改)、记录触发人/模型/耗时,供「AI 校验过程回放」与审计。
 */
@Service
public class AiRunLogService {

    private static final int INPUT_SUMMARY_MAX = 1990;

    private final AiRunLogMapper mapper;

    public AiRunLogService(AiRunLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 逐次留痕一次 AI 调用。output 计 SM3(防篡改),触发人取当前登录上下文(无则 system)。
     * REQUIRES_NEW:审计留痕须独立于业务事务提交——即便调用方业务事务回滚,"AI 调用已发生"的
     * 留痕也必须保留(否则违背"全环节操作留痕·可审计")。留痕失败不影响主流程,内部吞异常返回 null。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiRunLog record(String bizType, String bizId, String capability, String model,
                           String inputSummary, String output, long durationMs) {
        try {
            AiRunLog log = new AiRunLog();
            log.setBizType(bizType);
            log.setBizId(bizId);
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

    /** 按业务主键取 AI 操作时间线(回放,按时间升序)。 */
    public List<AiRunLog> listByBiz(String bizId) {
        return mapper.selectList(new LambdaQueryWrapper<AiRunLog>()
                .eq(AiRunLog::getBizId, bizId)
                .orderByAsc(AiRunLog::getCreateTime));
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
