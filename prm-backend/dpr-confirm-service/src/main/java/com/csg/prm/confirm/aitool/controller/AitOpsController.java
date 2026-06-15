package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitRunLog;
import com.csg.prm.confirm.aitool.entity.AitTask;
import com.csg.prm.confirm.aitool.service.AiToolFacade;
import com.csg.prm.confirm.aitool.service.AitRunLogService;
import com.csg.prm.confirm.aitool.service.AitTaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 开放对接与运行支撑接口(可研 3.3):统一工具适配层 + 批量任务 + 统一运行日志 + 模型配置。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/ops")
public class AitOpsController {

    private final AiToolFacade facade;
    private final AitTaskService taskService;
    private final AitRunLogService runLogService;

    public AitOpsController(AiToolFacade facade, AitTaskService taskService, AitRunLogService runLogService) {
        this.facade = facade;
        this.taskService = taskService;
        this.runLogService = runLogService;
    }

    // ---- #2/#3 统一工具适配层 + 模型配置 ----

    /** #1/#2 能力清单(供 MCP/ACP/CLI/REST 编排接入)。 */
    @GetMapping("/tools/capabilities")
    public R<List<Map<String, Object>>> capabilities() {
        return R.ok(facade.capabilities());
    }

    /** #2 统一工具调用入口。 */
    @PostMapping("/tools/invoke")
    public R<Object> invoke(@RequestBody Map<String, Object> body) {
        Object tool = body.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = body.get("params") instanceof Map ? (Map<String, Object>) body.get("params") : Map.of();
        return R.ok(facade.invoke(tool == null ? null : tool.toString(), params));
    }

    /** #3 模型/平台配置(OpenAI 兼容,内网可配)。 */
    @GetMapping("/tools/model-config")
    public R<Map<String, Object>> modelConfig() {
        return R.ok(facade.modelConfig());
    }

    // ---- #5/#4 批量任务 + 监控 ----

    /** #5 创建批量任务(并发/重试可配)。 */
    @PostMapping("/task/create")
    public R<String> createTask(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> items = body.get("items") instanceof List ? (List<String>) body.get("items") : List.of();
        return R.ok(taskService.create(str(body, "taskType"), items,
                intVal(body, "concurrency"), intVal(body, "retryMax"), str(body, "name")));
    }

    /** #5 运行任务(并发处理 + 断点续跑 + 失败重试 + 留痕)。 */
    @PostMapping("/task/{taskId}/run")
    public R<AitTask> runTask(@PathVariable String taskId) {
        return R.ok(taskService.run(taskId));
    }

    @PostMapping("/task/{taskId}/pause")
    public R<Void> pauseTask(@PathVariable String taskId) {
        taskService.pause(taskId);
        return R.ok();
    }

    /** #4 任务监控:详情。 */
    @GetMapping("/task/{taskId}")
    public R<AitTask> getTask(@PathVariable String taskId) {
        return R.ok(taskService.get(taskId));
    }

    /** #4 任务监控:列表。 */
    @GetMapping("/task/page")
    public R<PageResult<AitTask>> taskPage(PageQuery query,
                                           @RequestParam(required = false) String taskType,
                                           @RequestParam(required = false) String status) {
        return R.ok(taskService.page(query, taskType, status));
    }

    // ---- #6 统一运行日志 ----

    @GetMapping("/runlog/page")
    public R<PageResult<AitRunLog>> runLogPage(PageQuery query,
                                               @RequestParam(required = false) String logType,
                                               @RequestParam(required = false) String source,
                                               @RequestParam(required = false) String result) {
        return R.ok(runLogService.page(query, logType, source, result));
    }

    @GetMapping("/runlog/stats")
    public R<Map<String, Object>> runLogStats() {
        return R.ok(runLogService.stats());
    }

    /** #4 异常告警:运行日志中类型=告警 的记录。 */
    @GetMapping("/alerts")
    public R<PageResult<AitRunLog>> alerts(PageQuery query) {
        return R.ok(runLogService.page(query, AitRunLog.T_ALERT, null, null));
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }

    private static Integer intVal(Map<String, Object> m, String k) {
        Object v = m.get(k);
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return v == null ? null : Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
