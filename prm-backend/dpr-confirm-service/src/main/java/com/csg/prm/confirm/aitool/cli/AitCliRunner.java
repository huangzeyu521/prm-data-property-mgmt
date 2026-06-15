package com.csg.prm.confirm.aitool.cli;

import com.csg.prm.confirm.aitool.entity.AitTask;
import com.csg.prm.confirm.aitool.service.AitTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * CLI 批处理入口(3.3#1):仅当启动参数含 --ait.cli=<cmd> 时执行,否则无操作(不影响常规启动)。
 * 用法示例:
 *   java -jar dpr-confirm-service.jar --ait.cli=batch-audit --items=APPLY1,APPLY2 --concurrency=4 --retry=2
 *   java -jar dpr-confirm-service.jar --ait.cli=batch-parse --items=MAT1,MAT2
 */
@Component
@Order(100)
public class AitCliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AitCliRunner.class);

    private final AitTaskService taskService;

    public AitCliRunner(AitTaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void run(String... args) {
        String cmd = arg(args, "ait.cli");
        if (cmd == null) {
            return; // 非 CLI 模式
        }
        String itemsArg = arg(args, "items");
        if (itemsArg == null || itemsArg.isBlank()) {
            log.warn("[AIT-CLI] 缺少 --items 参数,跳过");
            return;
        }
        List<String> items = Arrays.stream(itemsArg.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).toList();
        String type = "batch-parse".equals(cmd) ? AitTask.TYPE_PARSE : AitTask.TYPE_AUDIT;
        Integer conc = intArg(args, "concurrency");
        Integer retry = intArg(args, "retry");
        log.info("[AIT-CLI] 命令={} 类型={} 项数={}", cmd, type, items.size());
        String taskId = taskService.create(type, items, conc, retry, "CLI-" + cmd);
        AitTask t = taskService.run(taskId);
        log.info("[AIT-CLI] 任务 {} 完成:成功 {} / 失败 {} / 状态 {}",
                taskId, t.getDone(), t.getFailed(), t.getStatus());
    }

    private static String arg(String[] args, String key) {
        String prefix = "--" + key + "=";
        for (String a : args) {
            if (a != null && a.startsWith(prefix)) {
                return a.substring(prefix.length());
            }
        }
        return null;
    }

    private static Integer intArg(String[] args, String key) {
        String v = arg(args, key);
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
