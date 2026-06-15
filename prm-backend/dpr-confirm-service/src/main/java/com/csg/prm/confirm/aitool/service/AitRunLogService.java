package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitRunLog;
import com.csg.prm.confirm.aitool.mapper.AitRunLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一运行日志服务(3.3#6):审计/操作/模型调用/规则命中/告警 统一记录、查询、统计。
 * 记录失败绝不影响主流程。
 */
@Service
public class AitRunLogService {

    private final AitRunLogMapper mapper;

    public AitRunLogService(AitRunLogMapper mapper) {
        this.mapper = mapper;
    }

    public void log(String type, String source, String action, String detail,
                    String model, Long durationMs, String result, String taskId) {
        try {
            AitRunLog l = new AitRunLog();
            l.setLogType(type);
            l.setSource(source);
            l.setAction(action);
            l.setDetail(cap(detail, 2000));
            l.setModel(model);
            l.setDurationMs(durationMs);
            l.setResult(result);
            l.setTaskId(taskId);
            l.setLogTime(LocalDateTime.now());
            mapper.insert(l);
        } catch (Exception ignore) {
            // 日志失败不阻断
        }
    }

    public void model(String source, String action, String model, long durationMs, String result) {
        log(AitRunLog.T_MODEL, source, action, null, model, durationMs, result, null);
    }

    public void rule(String source, String action, String detail) {
        log(AitRunLog.T_RULE, source, action, detail, null, null, "命中", null);
    }

    public void alert(String source, String action, String detail, String taskId) {
        log(AitRunLog.T_ALERT, source, action, detail, null, null, "告警", taskId);
    }

    public PageResult<AitRunLog> page(PageQuery query, String logType, String source, String result) {
        LambdaQueryWrapper<AitRunLog> w = new LambdaQueryWrapper<AitRunLog>()
                .eq(StringUtils.hasText(logType), AitRunLog::getLogType, logType)
                .like(StringUtils.hasText(source), AitRunLog::getSource, source)
                .eq(StringUtils.hasText(result), AitRunLog::getResult, result)
                .orderByDesc(AitRunLog::getLogTime);
        IPage<AitRunLog> p = mapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    public Map<String, Object> stats() {
        List<AitRunLog> all = mapper.selectList(new LambdaQueryWrapper<>());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", all.size());
        m.put("byType", all.stream().map(AitRunLog::getLogType).map(v -> v == null ? "未知" : v)
                .collect(Collectors.groupingBy(v -> v, LinkedHashMap::new, Collectors.counting())));
        m.put("byResult", all.stream().map(AitRunLog::getResult).map(v -> v == null ? "未知" : v)
                .collect(Collectors.groupingBy(v -> v, LinkedHashMap::new, Collectors.counting())));
        return m;
    }

    private static String cap(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
