package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.R;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.confirm.aitool.entity.AitParseConfig;
import com.csg.prm.confirm.aitool.service.AitParseConfigService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能确权辅助工具-解析元数据配置接口(1.4#4,管理员):字段映射/提取逻辑/置信度阈值。
 */
@RestController
@RequestMapping("/api/dpr/confirm/aitool/parse-config")
@RequiresRole({"admin"})
public class AitParseConfigController {

    private final AitParseConfigService service;

    public AitParseConfigController(AitParseConfigService service) {
        this.service = service;
    }

    @GetMapping
    public R<List<AitParseConfig>> list() {
        return R.ok(service.list());
    }

    @PostMapping
    public R<String> save(@RequestBody AitParseConfig config) {
        return R.ok(service.save(config));
    }

    @DeleteMapping("/{configId}")
    public R<Void> delete(@PathVariable String configId) {
        service.delete(configId);
        return R.ok();
    }
}
