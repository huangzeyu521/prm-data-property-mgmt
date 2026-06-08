package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;
import com.csg.prm.confirm.service.EquityCardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权益卡片接口(IM-DAM-DPR-02-001-003 权益卡片管理)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/card")
public class EquityCardController {

    private final EquityCardService service;

    public EquityCardController(EquityCardService service) {
        this.service = service;
    }

    @GetMapping("/{cardId}")
    public R<EquityCard> detail(@PathVariable String cardId) {
        return R.ok(service.getById(cardId));
    }

    @PostMapping("/{cardId}/freeze")
    public R<Void> freeze(@PathVariable String cardId) {
        service.freeze(cardId);
        return R.ok();
    }

    @PostMapping("/{cardId}/unfreeze")
    public R<Void> unfreeze(@PathVariable String cardId) {
        service.unfreeze(cardId);
        return R.ok();
    }

    @PostMapping("/{cardId}/revoke")
    public R<Void> revoke(@PathVariable String cardId, @RequestParam(required = false) String reason) {
        service.revoke(cardId, reason);
        return R.ok();
    }

    @GetMapping("/{cardId}/logs")
    public R<List<EquityCardLog>> logs(@PathVariable String cardId) {
        return R.ok(service.listLogs(cardId));
    }

    @PostMapping("/page")
    public R<PageResult<EquityCard>> page(@RequestBody PageQuery query) {
        return R.ok(service.page(query));
    }
}
