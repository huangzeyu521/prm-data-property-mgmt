package com.csg.prm.confirm.integration;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.R;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.integration.dto.AssetArchiveRowVO;
import com.csg.prm.confirm.integration.dto.AssetEquityVO;
import com.csg.prm.confirm.integration.dto.AssetPropertyVO;
import com.csg.prm.confirm.integration.dto.PlatformCardRef;
import com.csg.prm.confirm.integration.dto.PlatformTableMeta;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据资产卡片集成接口:平台卡片「产权信息 / 权益基本信息」两个子Tab按资产ID(assetId)只读取数。
 * 产权模块为权威源,仅暴露只读视图,平台不另存产权数据(单写,避免双写不一致)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/asset")
public class AssetCardIntegrationController {

    private final AssetCardIntegrationService service;
    private final AssetCardArchiveService archiveService;
    private final AssetCardWritebackService writebackService;
    private final AssetTableMetaService tableMetaService;

    public AssetCardIntegrationController(AssetCardIntegrationService service,
                                          AssetCardArchiveService archiveService,
                                          AssetCardWritebackService writebackService,
                                          AssetTableMetaService tableMetaService) {
        this.service = service;
        this.archiveService = archiveService;
        this.writebackService = writebackService;
        this.tableMetaService = tableMetaService;
    }

    /** 关联数据资产卡片:按 名称/编码/系统·表 搜索可关联卡片(选卡片而非手填ID;平台为源,台账兜底)。 */
    @GetMapping("/cards")
    public R<List<PlatformCardRef>> searchCards(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false, defaultValue = "10") int limit) {
        return R.ok(archiveService.searchCards(keyword, limit));
    }

    /** 选卡片→自动带库表清单(确权粒度到库表,对齐附录F表2/表3)。平台未接入时返回桩合成清单。 */
    @GetMapping("/{assetId}/tables")
    public R<List<PlatformTableMeta>> tables(@PathVariable String assetId,
                                             @RequestParam(required = false) String assetName) {
        return R.ok(tableMetaService.listTableMeta(assetId, assetName));
    }

    /** 数据集产权档案管理:只读分页查询可见卡片的确权/授权概要(无新增)。 */
    @GetMapping("/archive")
    public R<PageResult<AssetArchiveRowVO>> archive(PageQuery query,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String state) {
        return R.ok(archiveService.page(query, keyword, state));
    }

    /** 写回平台:确权/授权完成后把产权/权益结论单向写回平台卡片(平台接口未接入时为 stub)。 */
    @PostMapping("/{assetId}/writeback")
    public R<AssetCardWritebackService.WritebackResult> writeback(@PathVariable String assetId) {
        return R.ok(writebackService.writeback(assetId));
    }

    /** 「产权信息」子Tab(语义契约)。 */
    @GetMapping("/{assetId}/property")
    public R<AssetPropertyVO> property(@PathVariable String assetId) {
        return R.ok(service.property(assetId));
    }

    /** 「权益基本信息」子Tab(权益条目列表)。 */
    @GetMapping("/{assetId}/equity")
    public R<List<AssetEquityVO>> equity(@PathVariable String assetId) {
        return R.ok(service.equity(assetId));
    }

    /** 「产权信息」按平台字段名输出(经适配层),供平台直接对接。 */
    @GetMapping("/{assetId}/property/platform")
    public R<Map<String, Object>> propertyForPlatform(@PathVariable String assetId) {
        return R.ok(service.propertyForPlatform(assetId));
    }
}
