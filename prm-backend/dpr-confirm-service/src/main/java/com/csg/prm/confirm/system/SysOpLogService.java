package com.csg.prm.confirm.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 系统操作日志服务:统一记录登录与用户管理类操作,供审计查询。
 * 记录失败绝不影响主业务(record 内部吞异常)。
 */
@Service
public class SysOpLogService {

    private final SysOpLogMapper mapper;

    public SysOpLogService(SysOpLogMapper mapper) {
        this.mapper = mapper;
    }

    /** 以当前登录上下文为操作人记录一条日志。 */
    public void record(String action, String target, String detail, String result) {
        UserContext ctx = UserContextHolder.get();
        String uid = ctx == null ? null : ctx.getUserId();
        String uname = ctx == null ? null : ctx.getUserName();
        record(uid, uname, action, target, detail, result, null);
    }

    /** 指定操作人记录(登录时上下文尚非该用户,需显式传入)。 */
    public void record(String userId, String userName, String action,
                       String target, String detail, String result, String ip) {
        try {
            SysOpLog log = new SysOpLog();
            log.setLogId(UUID.randomUUID().toString().replace("-", ""));
            log.setUserId(userId);
            log.setUserName(userName);
            log.setAction(action);
            log.setTarget(truncate(target, 255));
            log.setDetail(truncate(detail, 1000));
            log.setResult(result);
            log.setIp(ip);
            log.setCreateTime(LocalDateTime.now());
            mapper.insert(log);
        } catch (Exception ignore) {
            // 审计日志失败不阻断主流程
        }
    }

    public PageResult<SysOpLog> page(SysOpLogQuery query) {
        LambdaQueryWrapper<SysOpLog> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUserName())) {
            w.like(SysOpLog::getUserName, query.getUserName());
        }
        if (StringUtils.hasText(query.getAction())) {
            w.eq(SysOpLog::getAction, query.getAction());
        }
        if (StringUtils.hasText(query.getResult())) {
            w.eq(SysOpLog::getResult, query.getResult());
        }
        if (StringUtils.hasText(query.getCreateTimeStart())) {
            w.ge(SysOpLog::getCreateTime, query.getCreateTimeStart());
        }
        if (StringUtils.hasText(query.getCreateTimeEnd())) {
            w.le(SysOpLog::getCreateTime, query.getCreateTimeEnd());
        }
        w.orderByDesc(SysOpLog::getCreateTime);
        IPage<SysOpLog> page = mapper.selectPage(query.toPage(), w);
        return PageResult.of(page);
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
