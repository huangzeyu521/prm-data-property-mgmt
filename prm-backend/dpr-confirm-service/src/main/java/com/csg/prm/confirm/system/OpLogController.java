package com.csg.prm.confirm.system;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.auth.RequiresRole;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统管理 - 操作日志查询(仅管理员)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/system/oplog")
@RequiresRole({"admin"})
public class OpLogController {

    private final SysOpLogService service;

    public OpLogController(SysOpLogService service) {
        this.service = service;
    }

    @PostMapping("/page")
    public Result<PageResult<SysOpLog>> page(@Valid @RequestBody SysOpLogQuery query) {
        return Result.success(service.page(query));
    }
}
