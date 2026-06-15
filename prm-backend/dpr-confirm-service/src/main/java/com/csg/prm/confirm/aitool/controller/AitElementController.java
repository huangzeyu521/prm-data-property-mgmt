package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.confirm.aitool.service.AitElementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 智能确权辅助工具-确权要素识别与特征抽取接口(可研 1.3 / M1.3)。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/element")
public class AitElementController {

    private final AitElementService service;

    public AitElementController(AitElementService service) {
        this.service = service;
    }

    /** 抽取要素并生成确权画像(规则+模型混合)。 */
    @PostMapping("/{materialId}/extract")
    public R<AitElementService.ProfileDTO> extract(@PathVariable String materialId,
                                                   @RequestParam(defaultValue = "true") boolean useModel) {
        return R.ok(service.extract(materialId, useModel));
    }

    /** 取材料的确权画像(主体/约束/结构化 JSON)。 */
    @GetMapping("/{materialId}/profile")
    public R<AitElementService.ProfileDTO> profile(@PathVariable String materialId) {
        return R.ok(service.profile(materialId));
    }

    /** #6 表级 + 附件级 确权特征视图(按数据表/申请聚合)。 */
    @GetMapping("/view")
    public R<Map<String, Object>> view(@RequestParam(required = false) String dataTableRef,
                                       @RequestParam(required = false) String applyId) {
        return R.ok(service.view(dataTableRef, applyId));
    }
}
