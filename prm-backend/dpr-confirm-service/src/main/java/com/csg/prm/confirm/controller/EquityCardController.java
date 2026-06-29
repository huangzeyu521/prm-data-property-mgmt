package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCardLog;
import com.csg.prm.confirm.service.EquityCardService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
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
@Validated
@RequestMapping("/api/dpr/confirm/card")
public class EquityCardController {

    private final EquityCardService service;

    public EquityCardController(EquityCardService service) {
        this.service = service;
    }

    /** 按卡片编号查询(授权服务先确后授真实校验用;不存在返回 data=null) */
    @GetMapping("/by-no/{cardNo}")
    public Result<com.csg.prm.confirm.entity.EquityCard> byNo(@PathVariable String cardNo) {
        return Result.success(service.findByNo(cardNo));
    }

    @GetMapping("/{cardId}")
    public Result<EquityCard> detail(@PathVariable String cardId) {
        return Result.success(service.getById(cardId));
    }

    /** 冻结(风险熔断):依工作指引§3.3.3 权益风险处置,由数据产权合规管控小组(review)/管理员(admin)处置。 */
    @RequiresRole({"admin", "review"})
    @PostMapping("/{cardId}/freeze")
    public Result<Void> freeze(@PathVariable String cardId) {
        service.freeze(cardId);
        return Result.success();
    }

    /** 解冻(风险解除):同冻结,合规管控小组/管理员处置。 */
    @RequiresRole({"admin", "review"})
    @PostMapping("/{cardId}/unfreeze")
    public Result<Void> unfreeze(@PathVariable String cardId) {
        service.unfreeze(cardId);
        return Result.success();
    }

    /** 注销/撤销(治理处置):依§6 可追溯+§3.3.3,由管理员(数字化部主管口径)/合规管控小组处置,全程留痕。 */
    @RequiresRole({"admin", "review"})
    @PostMapping("/{cardId}/revoke")
    public Result<Void> revoke(@PathVariable String cardId, @RequestParam(required = false) String reason) {
        service.revoke(cardId, reason);
        return Result.success();
    }

    @GetMapping("/{cardId}/logs")
    public Result<List<EquityCardLog>> logs(@PathVariable String cardId) {
        return Result.success(service.listLogs(cardId));
    }

    @PostMapping("/page")
    public Result<PageResult<EquityCard>> page(@Valid @RequestBody com.csg.prm.confirm.dto.EquityCardQuery query) {
        return Result.success(service.page(query));
    }

    /** 权益卡片概览统计(总/正常/冻结/失效/即将到期),按过滤聚合(忽略 status),供生成管理页概览条。 */
    @PostMapping("/stats")
    public Result<com.csg.prm.confirm.dto.EquityCardStats> stats(@RequestBody com.csg.prm.confirm.dto.EquityCardQuery query) {
        return Result.success(service.stats(query));
    }

    /** 待重确权清单(F指导书"按季度定期重新确权"/权益到期):有效期在 daysAhead 天内(含已到期)的正常卡片。 */
    @GetMapping("/re-confirm-due")
    public Result<List<EquityCard>> reConfirmDue(@RequestParam(required = false, defaultValue = "90") int daysAhead) {
        return Result.success(service.listReConfirmDue(daysAhead));
    }
}
