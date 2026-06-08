package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.authorize.service.BatchAuthListService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 表6 数据批量授权清单接口(IM-DAM-DPR-03-001-001-005)。草案->申报稿->批准。 */
@RestController
@RequestMapping("/api/dpr/auth/batch-list")
public class BatchAuthListController {

    private final BatchAuthListService service;

    public BatchAuthListController(BatchAuthListService service) {
        this.service = service;
    }

    @PostMapping
    public R<String> create(@RequestBody BatchAuthList list) {
        return R.ok(service.create(list));
    }

    @PostMapping("/{batchListId}/submit")
    public R<Void> submit(@PathVariable String batchListId) {
        service.submit(batchListId);
        return R.ok();
    }

    @PostMapping("/{batchListId}/approve")
    public R<Void> approve(@PathVariable String batchListId) {
        service.approve(batchListId);
        return R.ok();
    }

    @GetMapping("/{batchListId}")
    public R<BatchAuthList> detail(@PathVariable String batchListId) {
        return R.ok(service.getById(batchListId));
    }

    @GetMapping("/page")
    public R<PageResult<BatchAuthList>> page(@RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String listYear,
                                             @RequestParam(required = false) String listStatus) {
        return R.ok(service.page(current, size, listYear, listStatus));
    }
}
