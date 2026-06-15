package com.csg.prm.confirm.aitool;

import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitRunLog;
import com.csg.prm.confirm.aitool.entity.AitTask;
import com.csg.prm.confirm.aitool.service.AiToolFacade;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.aitool.service.AitRunLogService;
import com.csg.prm.confirm.aitool.service.AitTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 3.3 开放对接与运行支撑(#2~#6)能力测试。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitOpsTest {

    @Autowired private AiToolFacade facade;
    @Autowired private AitTaskService taskService;
    @Autowired private AitRunLogService runLogService;
    @Autowired private AitMaterialService materialService;

    private String material(String name) {
        AitMaterial m = new AitMaterial();
        m.setFileName(name);
        m.setContent("数据持有权,广东电网,自行生产,已盖章");
        return materialService.upload(m);
    }

    /** #2 统一工具适配层:能力清单 + 统一调用;#6 模型调用日志。 */
    @Test
    void facade_capabilities_and_invoke() {
        List<Map<String, Object>> caps = facade.capabilities();
        assertTrue(caps.size() >= 6, "应暴露 ≥6 类工具能力");
        assertTrue(caps.stream().anyMatch(c -> "embedding".equals(c.get("tool")))
                && caps.stream().anyMatch(c -> "parse".equals(c.get("tool"))), "应含 embedding/parse");

        Object out = facade.invoke("embedding", Map.of("text", "数据确权审核"));
        assertTrue(((Map<?, ?>) out).get("dim") instanceof Integer, "embedding 应返回维度");

        // #6 模型调用日志
        assertTrue(runLogService.page(new PageQuery(), AitRunLog.T_MODEL, null, null).getTotal() >= 1,
                "应记录模型调用日志");
    }

    /** #3 OpenAI 兼容模型配置(内网可配)。 */
    @Test
    void model_config_openai_compatible() {
        Map<String, Object> cfg = facade.modelConfig();
        assertEquals(Boolean.TRUE, cfg.get("openaiCompatible"));
        assertTrue(cfg.containsKey("baseUrl") && cfg.containsKey("model"), "应暴露 baseUrl/model 供内网切换");
    }

    /** #5 批量任务:并发处理 + 失败重试 + 断点续跑;#4 任务监控;#6 告警日志。 */
    @Test
    void batch_task_concurrency_retry_resume() {
        String m1 = material("批量1.pdf");
        String m2 = material("批量2.pdf");
        // 含一个无效ID → 触发失败+重试+告警
        String taskId = taskService.create(AitTask.TYPE_PARSE, List.of(m1, m2, "NO-SUCH-MATERIAL"), 3, 1, "批量解析测试");

        AitTask t = taskService.run(taskId);
        assertEquals(2, t.getDone(), "两个有效材料应解析成功");
        assertEquals(1, t.getFailed(), "无效项应失败");
        assertEquals(AitTask.ST_PARTIAL, t.getStatus(), "部分失败状态");

        // #6 告警日志(失败项)
        assertTrue(runLogService.page(new PageQuery(), AitRunLog.T_ALERT, null, null).getTotal() >= 1, "失败应产生告警");

        // #5 断点续跑:重跑跳过已成功,仍仅失败项重试
        AitTask t2 = taskService.run(taskId);
        assertEquals(2, t2.getDone(), "断点续跑应保持已成功项");

        // #4 任务监控
        assertEquals(taskId, taskService.get(taskId).getTaskId());
        assertTrue(taskService.page(new PageQuery(), AitTask.TYPE_PARSE, null).getTotal() >= 1, "任务列表可查");
    }

    /** #6 统一运行日志查询与统计。 */
    @Test
    void run_log_stats() {
        facade.invoke("embedding", Map.of("text", "日志统计测试"));
        Map<String, Object> stats = runLogService.stats();
        assertTrue(((Number) stats.get("total")).intValue() >= 1);
        assertFalse(((Map<?, ?>) stats.get("byType")).isEmpty(), "应按类型汇总日志");
    }
}
