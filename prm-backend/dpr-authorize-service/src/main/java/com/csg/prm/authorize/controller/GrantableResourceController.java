package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.gateway.OpenCatalogGateway;
import com.csg.prm.common.api.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 可授权资源池辅助接口。
 * <p>支撑"批量授权一站式"确权目录资源池的前置裁剪:前端以 getPropertyTree ∩ 生效权益卡片(卡片权益==所选权益)
 * 组装"先确后授 + 权属可授"的资源池,而"经营权仅限对外开放目录"(附录F §3.4.3)的判定为授权侧权威,
 * 由本接口暴露批量过滤,使经营权资源池在录入前即剔除非对外开放资产(而非提交时事后拦截)。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/auth/grantable")
public class GrantableResourceController {

    private final OpenCatalogGateway openCatalogGateway;

    public GrantableResourceController(OpenCatalogGateway openCatalogGateway) {
        this.openCatalogGateway = openCatalogGateway;
    }

    /**
     * 对外开放目录批量过滤:入参资产ID列表 → 仅保留在对外开放目录中的(去重、保持入参顺序)。
     * 供经营权资源池前端在构建确权目录树时裁剪非对外开放资产。
     */
    @PostMapping("/open-filter")
    public Result<List<String>> openFilter(@RequestBody(required = false) List<String> assetIds) {
        List<String> out = new ArrayList<>();
        if (assetIds != null) {
            Set<String> seen = new LinkedHashSet<>();
            for (String id : assetIds) {
                if (id != null && seen.add(id) && openCatalogGateway.isInOpenCatalog(id)) {
                    out.add(id);
                }
            }
        }
        return Result.success(out);
    }
}
