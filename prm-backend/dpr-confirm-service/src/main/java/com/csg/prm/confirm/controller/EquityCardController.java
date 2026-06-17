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

    /** 按卡片编号查询(授权服务先确后授真实校验用;不存在返回 data=null) */
    @GetMapping("/by-no/{cardNo}")
    public R<com.csg.prm.confirm.entity.EquityCard> byNo(@PathVariable String cardNo) {
        return R.ok(service.findByNo(cardNo));
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

    /** 待重确权清单(F指导书"按季度定期重新确权"/权益到期):有效期在 daysAhead 天内(含已到期)的正常卡片。 */
    @GetMapping("/re-confirm-due")
    public R<List<EquityCard>> reConfirmDue(@RequestParam(required = false, defaultValue = "90") int daysAhead) {
        return R.ok(service.listReConfirmDue(daysAhead));
    }
}
