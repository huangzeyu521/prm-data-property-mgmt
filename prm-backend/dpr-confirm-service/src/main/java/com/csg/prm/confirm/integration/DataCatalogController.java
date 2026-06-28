package com.csg.prm.confirm.integration;

import com.csg.prm.common.api.Result;
import com.csg.prm.confirm.integration.dto.CatalogNode;
import com.csg.prm.confirm.integration.dto.PlatformTableMeta;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据目录(确权申请左侧范围树)接口:业务域 → 系统 → 一级功能模块 → 库表(卡片)。
 * 支撑"系统级确权":先选系统 → 多选一级功能模块 → 多选库表卡片。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/data-catalog")
public class DataCatalogController {

    private final DataCatalogService service;
    private final com.csg.prm.confirm.service.ConfirmChangeBaselineService changeBaselineService;

    public DataCatalogController(DataCatalogService service,
                                 com.csg.prm.confirm.service.ConfirmChangeBaselineService changeBaselineService) {
        this.service = service;
        this.changeBaselineService = changeBaselineService;
    }

    /** 懒加载目录树子节点。type:null/root=根(业务域);domain/system/module 取对应下一层。 */
    @GetMapping("/tree")
    public Result<List<CatalogNode>> tree(@RequestParam(required = false) String type,
                                          @RequestParam(required = false) String id,
                                          @RequestParam(required = false) String status) {
        return Result.success(service.tree(type, id, status));
    }

    /** 按系统(可再按一级功能模块筛)列出库表卡片;status:unconfirmed=未确权(初始)/confirmed=已确权(变更)。 */
    @GetMapping("/cards")
    public Result<List<PlatformTableMeta>> cards(@RequestParam String sysName,
                                                 @RequestParam(required = false) List<String> modules,
                                                 @RequestParam(required = false) String status) {
        return Result.success(service.cardsBySystem(sysName, modules, status));
    }

    /** 系统名→业务域映射:供授权侧资源池「所属业务域」按系统逐表带出(表5/表6 所属业务域)。 */
    @GetMapping("/system-domains")
    public Result<java.util.Map<String, String>> systemDomains() {
        return Result.success(service.systemDomainMap());
    }

    /** 数据资产确权目录全树(非懒加载):业务域→系统→功能模块→库表 + 各级确权状态;供「数据资产确权目录管理」整树渲染。 */
    @GetMapping("/full-tree")
    public Result<List<com.csg.prm.confirm.integration.dto.CatalogTreeNode>> fullTree() {
        return Result.success(service.fullTree());
    }

    /** 确权变更基线:某系统现有确权结论快照(供变更前→变更后 diff 与预填);无已确权库表返回 null。 */
    @GetMapping("/baseline")
    public Result<com.csg.prm.confirm.integration.dto.ChangeBaseline> baseline(@RequestParam String sysName) {
        return Result.success(service.baselineOf(sysName));
    }

    /**
     * 确权变更·完整基线:反查上一版真实确权结论(表3逐表行 + 权益卡片 + 认定意见)作变更底版。
     * fromRealConfirm=false 时退回身份级合成桩(base)、明细为空。
     */
    @GetMapping("/baseline-full")
    public Result<com.csg.prm.confirm.integration.dto.ChangeBaselineFull> baselineFull(
            @RequestParam(required = false) String assetId,
            @RequestParam(required = false) String sysName) {
        return Result.success(changeBaselineService.baselineOf(assetId, sysName));
    }

    /** 确权变更·授权影响(逐表精确):入参=本次选中的库表代码 + 变更触发动因,返回受影响授权 + 处置建议。 */
    @GetMapping("/auth-impact")
    public Result<com.csg.prm.confirm.integration.dto.AuthImpact> authImpact(@RequestParam String sysName,
                                                                             @RequestParam(required = false) List<String> tableCodes,
                                                                             @RequestParam(required = false) String trigger) {
        return Result.success(service.authImpactOf(sysName, tableCodes, trigger));
    }
}
