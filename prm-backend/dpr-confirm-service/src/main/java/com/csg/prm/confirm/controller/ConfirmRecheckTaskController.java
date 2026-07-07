package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.confirm.dto.RecheckHealthVO;
import com.csg.prm.confirm.dto.RecheckTaskQuery;
import com.csg.prm.confirm.entity.ConfirmRecheckTask;
import com.csg.prm.confirm.service.ConfirmRecheckTaskService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 变更联动工单(季度重确权闭环)接口:工单池查询 / 立即扫描 / 处置双出口 / 变更健康度。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/recheck")
public class ConfirmRecheckTaskController {

    private static final int QUARTER_DAYS = 90;

    private final ConfirmRecheckTaskService service;

    public ConfirmRecheckTaskController(ConfirmRecheckTaskService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<ConfirmRecheckTask>> page(@Valid @RequestBody RecheckTaskQuery query) {
        return Result.success(service.page(query));
    }

    @GetMapping("/{taskId}")
    public Result<ConfirmRecheckTask> detail(@PathVariable String taskId) {
        return Result.success(service.getById(taskId));
    }

    /** 立即执行季度到期扫描(平时由定时任务季度触发;此接口供管理员手工补扫/演示)。 */
    @com.csg.prm.common.auth.RequiresRole({"manager", "admin"})
    @PostMapping("/scan")
    public Result<Integer> scan() {
        return Result.success(service.scanDueCards(QUARTER_DAYS));
    }

    /** 处置出口①:派生确权变更草稿(工单号写 sourceRef,证据链可追溯)。 */
    @com.csg.prm.common.auth.RequiresRole({"apply", "manager", "admin"})
    @PostMapping("/{taskId}/derive")
    public Result<String> derive(@PathVariable String taskId) {
        return Result.success(service.deriveChange(taskId));
    }

    /** 处置出口②:复核确认无变化(主管确认,结论必填留痕)。 */
    @com.csg.prm.common.auth.RequiresRole({"manager", "admin"})
    @PostMapping("/{taskId}/no-change")
    public Result<Void> noChange(@PathVariable String taskId, @RequestParam String note) {
        service.confirmNoChange(taskId, note);
        return Result.success();
    }

    /** 手工销号(授权处置完成等)。 */
    @com.csg.prm.common.auth.RequiresRole({"apply", "manager", "admin"})
    @PostMapping("/{taskId}/complete")
    public Result<Void> complete(@PathVariable String taskId, @RequestParam(required = false) String note) {
        service.complete(taskId, note);
        return Result.success();
    }

    /** P2.2 变更健康度:待处置/逾期/复核率/版本链完整率。 */
    @GetMapping("/health")
    public Result<RecheckHealthVO> health() {
        return Result.success(service.health());
    }
}
