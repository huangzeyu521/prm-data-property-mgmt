package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.aitool.entity.AitParseConfig;
import com.csg.prm.confirm.aitool.mapper.AitParseConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析元数据配置服务(1.4#4):按场景提供 置信度阈值 / 字段映射规则 / 提取逻辑;管理员 CRUD。
 * 启动幂等种入 default 配置(阈值 0.95)。
 */
@Service
public class AitParseConfigService implements ApplicationRunner {

    public static final double DEFAULT_THRESHOLD = 0.95;
    private static final ObjectMapper OM = new ObjectMapper();

    private final AitParseConfigMapper mapper;

    public AitParseConfigService(AitParseConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long n = mapper.selectCount(new LambdaQueryWrapper<AitParseConfig>()
                .eq(AitParseConfig::getScene, AitParseConfig.DEFAULT_SCENE));
        if (n != null && n > 0) {
            return;
        }
        AitParseConfig c = new AitParseConfig();
        c.setScene(AitParseConfig.DEFAULT_SCENE);
        c.setConfidenceThreshold(DEFAULT_THRESHOLD);
        c.setFieldMappingJson("{}");
        c.setExtractLogicJson("{\"enableModel\":true,\"enableOcr\":true}");
        c.setEnabled(1);
        c.setRemark("默认解析配置");
        mapper.insert(c);
    }

    /** 取生效配置:指定场景(启用)→ default → 内置默认。 */
    public AitParseConfig effective(String scene) {
        AitParseConfig c = null;
        if (StringUtils.hasText(scene) && !AitParseConfig.DEFAULT_SCENE.equals(scene)) {
            c = mapper.selectOne(new LambdaQueryWrapper<AitParseConfig>()
                    .eq(AitParseConfig::getScene, scene).eq(AitParseConfig::getEnabled, 1)
                    .orderByDesc(AitParseConfig::getUpdateTime).last("LIMIT 1"));
        }
        if (c == null) {
            c = mapper.selectOne(new LambdaQueryWrapper<AitParseConfig>()
                    .eq(AitParseConfig::getScene, AitParseConfig.DEFAULT_SCENE).eq(AitParseConfig::getEnabled, 1)
                    .last("LIMIT 1"));
        }
        if (c == null) {
            c = new AitParseConfig();
            c.setScene(AitParseConfig.DEFAULT_SCENE);
            c.setConfidenceThreshold(DEFAULT_THRESHOLD);
        }
        return c;
    }

    /** 生效置信度阈值(供解析复核判定)。 */
    public double threshold(String scene) {
        Double t = effective(scene).getConfidenceThreshold();
        return t == null ? DEFAULT_THRESHOLD : t;
    }

    /** 生效字段映射规则:原始字段名 → 模板字段键(供清洗字段对齐扩展)。 */
    public Map<String, String> fieldMapping(String scene) {
        Map<String, String> out = new LinkedHashMap<>();
        String json = effective(scene).getFieldMappingJson();
        if (!StringUtils.hasText(json)) {
            return out;
        }
        try {
            JsonNode n = OM.readTree(json);
            if (n.isObject()) {
                n.fields().forEachRemaining(e -> {
                    if (StringUtils.hasText(e.getValue().asText())) {
                        out.put(e.getKey(), e.getValue().asText());
                    }
                });
            }
        } catch (Exception ignore) {
            // 配置 JSON 非法 → 视为无额外映射
        }
        return out;
    }

    public List<AitParseConfig> list() {
        return mapper.selectList(new LambdaQueryWrapper<AitParseConfig>()
                .orderByDesc(AitParseConfig::getUpdateTime));
    }

    public String save(AitParseConfig c) {
        if (!StringUtils.hasText(c.getScene())) {
            throw new BizException("场景不能为空");
        }
        if (c.getConfidenceThreshold() != null
                && (c.getConfidenceThreshold() < 0 || c.getConfidenceThreshold() > 1)) {
            throw new BizException("置信度阈值应在 0~1 之间");
        }
        if (StringUtils.hasText(c.getConfigId())) {
            mapper.updateById(c);
            return c.getConfigId();
        }
        if (c.getEnabled() == null) {
            c.setEnabled(1);
        }
        mapper.insert(c);
        return c.getConfigId();
    }

    public void delete(String configId) {
        AitParseConfig c = mapper.selectById(configId);
        if (c == null) {
            throw new BizException("配置不存在");
        }
        if (AitParseConfig.DEFAULT_SCENE.equals(c.getScene())) {
            throw new BizException("默认配置不可删除");
        }
        mapper.deleteById(configId);
    }
}
