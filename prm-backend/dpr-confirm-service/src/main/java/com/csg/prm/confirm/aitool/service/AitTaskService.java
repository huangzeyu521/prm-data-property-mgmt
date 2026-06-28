package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitTask;
import com.csg.prm.confirm.aitool.mapper.AitTaskMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量任务服务(3.3#5):并发控制 + 断点续跑(跳过已成功)+ 失败重试 + 全程留痕 + 任务监控(#4)。
 */
@Service
public class AitTaskService {

    private static final ObjectMapper OM = new ObjectMapper();
    private static final String DONE = "成功";
    private static final String FAIL = "失败";
    private static final String PENDING = "待处理";

    private final AitTaskMapper taskMapper;
    private final AitMaterialService materialService;
    private final AitAuditAgentService agentService;
    private final AitRunLogService runLog;

    public AitTaskService(AitTaskMapper taskMapper, AitMaterialService materialService,
                          AitAuditAgentService agentService, AitRunLogService runLog) {
        this.taskMapper = taskMapper;
        this.materialService = materialService;
        this.agentService = agentService;
        this.runLog = runLog;
    }

    public String create(String taskType, List<String> items, Integer concurrency, Integer retryMax, String name) {
        if (!StringUtils.hasText(taskType) || items == null || items.isEmpty()) {
            throw new BusinessException("任务类型与处理项不能为空");
        }
        AitTask t = new AitTask();
        t.setTaskType(taskType);
        t.setTaskName(StringUtils.hasText(name) ? name : taskType + "-" + items.size() + "项");
        t.setItemsJson(toJson(items));
        Map<String, String> state = new java.util.LinkedHashMap<>();
        items.forEach(id -> state.put(id, PENDING));
        t.setItemStateJson(toJson(state));
        t.setTotal(items.size());
        t.setDone(0);
        t.setFailed(0);
        t.setStatus(AitTask.ST_PENDING);
        t.setConcurrency(concurrency == null || concurrency < 1 ? 4 : Math.min(concurrency, 16));
        t.setRetryMax(retryMax == null || retryMax < 0 ? 2 : retryMax);
        t.setCursor(0);
        taskMapper.insert(t);
        runLog.log("操作", "AitTaskService", "创建任务", t.getTaskName(), null, null, "成功", t.getTaskId());
        return t.getTaskId();
    }

    /** 运行任务:并发处理未成功项,失败重试,断点续跑(已成功跳过),全程留痕。 */
    public AitTask run(String taskId) {
        AitTask t = require(taskId);
        List<String> items = fromJsonList(t.getItemsJson());
        Map<String, String> state = new ConcurrentHashMap<>(fromJsonMap(t.getItemStateJson()));
        // 断点续跑:仅处理未成功项
        List<String> pending = items.stream().filter(id -> !DONE.equals(state.get(id))).toList();
        t.setStatus(AitTask.ST_RUNNING);
        taskMapper.updateById(t);
        runLog.log("操作", "AitTaskService", "运行任务",
                "断点续跑,待处理 " + pending.size() + "/" + items.size(), null, null, "成功", taskId);

        AtomicInteger failed = new AtomicInteger(0);
        int poolSize = Math.max(1, Math.min(t.getConcurrency() == null ? 4 : t.getConcurrency(), Math.max(1, pending.size())));
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            List<Future<?>> futures = new ArrayList<>();
            int retryMax = t.getRetryMax() == null ? 2 : t.getRetryMax();
            for (String id : pending) {
                futures.add(pool.submit(() -> {
                    String err = processWithRetry(t.getTaskType(), id, retryMax, taskId);
                    if (err == null) {
                        state.put(id, DONE);
                    } else {
                        state.put(id, FAIL);
                        failed.incrementAndGet();
                        runLog.alert("AitTaskService", "任务项失败:" + id, err, taskId);
                    }
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (Exception ignore) {
                    // 单项异常已在内部处理
                }
            }
        } finally {
            pool.shutdown();
        }
        long doneCount = items.stream().filter(id -> DONE.equals(state.get(id))).count();
        long failCount = items.stream().filter(id -> FAIL.equals(state.get(id))).count();
        t.setItemStateJson(toJson(state));
        t.setDone((int) doneCount);
        t.setFailed((int) failCount);
        t.setCursor(items.size());
        t.setStatus(failCount > 0 ? AitTask.ST_PARTIAL : AitTask.ST_DONE);
        t.setLastError(failCount > 0 ? ("有 " + failCount + " 项失败,可重跑续处理") : null);
        taskMapper.updateById(t);
        runLog.log("操作", "AitTaskService", "任务结束",
                "完成 " + doneCount + "/失败 " + failCount, null, null, failCount > 0 ? "失败" : "成功", taskId);
        return t;
    }

    /** 单项处理 + 失败重试;成功返回 null,否则返回最后错误。 */
    private String processWithRetry(String taskType, String id, int retryMax, String taskId) {
        Exception last = null;
        for (int attempt = 0; attempt <= retryMax; attempt++) {
            try {
                dispatch(taskType, id);
                return null;
            } catch (Exception e) {
                last = e;
                runLog.log("操作", "AitTaskService", "重试 " + (attempt + 1) + "/" + retryMax + " 项 " + id,
                        e.getMessage(), null, null, "失败", taskId);
            }
        }
        return last == null ? "未知错误" : last.getMessage();
    }

    private void dispatch(String taskType, String id) {
        switch (taskType) {
            case AitTask.TYPE_PARSE -> materialService.parse(id);
            case AitTask.TYPE_AUDIT -> agentService.audit(id);
            default -> throw new BusinessException("不支持的任务类型:" + taskType);
        }
    }

    public AitTask get(String taskId) {
        return require(taskId);
    }

    public void pause(String taskId) {
        AitTask t = require(taskId);
        t.setStatus(AitTask.ST_PAUSED);
        taskMapper.updateById(t);
        runLog.log("操作", "AitTaskService", "暂停任务", taskId, null, null, "成功", taskId);
    }

    public PageResult<AitTask> page(PageQuery query, String taskType, String status) {
        LambdaQueryWrapper<AitTask> w = new LambdaQueryWrapper<AitTask>()
                .eq(StringUtils.hasText(taskType), AitTask::getTaskType, taskType)
                .eq(StringUtils.hasText(status), AitTask::getStatus, status)
                .orderByDesc(AitTask::getCreateTime);
        IPage<AitTask> p = taskMapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    private AitTask require(String taskId) {
        AitTask t = taskMapper.selectById(taskId);
        if (t == null) {
            throw new BusinessException("任务不存在");
        }
        return t;
    }

    private static String toJson(Object o) {
        try {
            return OM.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static List<String> fromJsonList(String json) {
        try {
            return OM.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static Map<String, String> fromJsonMap(String json) {
        try {
            return OM.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (Exception e) {
            return new java.util.LinkedHashMap<>();
        }
    }
}
