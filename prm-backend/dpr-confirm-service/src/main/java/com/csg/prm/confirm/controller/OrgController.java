package com.csg.prm.confirm.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.common.org.Jurisdiction;
import com.csg.prm.common.org.OrgNode;
import com.csg.prm.common.org.OrgService;
import com.csg.prm.common.org.SysOrganization;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织机构主数据只读接口(平台/4A 同步,PRM 不写):
 * 供部门/归口下拉、Dashboard 部门筛选、组织树选择消费真实组织,替代自由文本/写死选项。
 */
@Validated
@RestController
@RequestMapping("/api/dpr/org")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    /** 组织树(根=网级,逐层 children)。 */
    @GetMapping("/tree")
    public R<List<OrgNode>> tree() {
        return R.ok(orgService.tree());
    }

    /** 组织清单;level 可选(网级/省级/地市),用于按层级取下拉项。 */
    @GetMapping("/list")
    public R<List<SysOrganization>> list(@RequestParam(required = false) String level) {
        return R.ok(orgService.listByLevel(level));
    }

    /** 归口网级解析:传组织名/缩写/ID,返回省/地市归属(province_code/bureau_code)。 */
    @GetMapping("/resolve")
    public R<Jurisdiction> resolve(@RequestParam("org") @NotBlank(message = "组织标识不能为空") String org) {
        return R.ok(orgService.resolve(org));
    }
}
