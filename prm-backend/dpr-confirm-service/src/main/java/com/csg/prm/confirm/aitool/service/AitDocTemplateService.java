package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitDocTemplate;
import com.csg.prm.confirm.aitool.mapper.AitDocTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 资料模板库服务(1.4#3):确权书/授权函/权属证明等模板的 CRUD + 在线编辑 + 版本管理 + 下载。
 * 启动幂等种入标准模板。
 */
@Service
public class AitDocTemplateService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AitDocTemplateService.class);
    private static final String TPL_DIR = "aitool/templates/";
    private static final ObjectMapper OM = new ObjectMapper();

    private final AitDocTemplateMapper mapper;

    public AitDocTemplateService(AitDocTemplateMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("确权书", "数据确权书(标准模板)",
                "数据确权书\n\n权利主体:____\n数据客体:____\n权利类型:数据持有权/数据加工使用权/数据产品经营权\n数据来源:____\n确权结论:____\n出具单位(盖章):____\n日期:____");
        seed("授权函", "数据授权函(标准模板)",
                "数据授权函\n\n授权方:____\n被授权方:____\n授权范围:____\n使用边界:____\n授权期限:____\n签章:____");
        seed("权属证明", "数据权属证明(标准模板)",
                "数据权属证明\n\n兹证明下述数据之权属:\n数据名称:____\n权属主体:____\n来源方式:____\n证明单位(盖章):____\n日期:____");
        // 从南网确权授权业务指导书(附录D/E/F/G)拆出的真实填空式模板,资源驱动幂等种入
        seedFromResources();
    }

    /** 模板清单条目:类型 + 名称 + 正文资源文件名。 */
    public record TemplateEntry(String type, String name, String file) {
    }

    /** 读取 aitool/templates/manifest.json,把指导书内嵌模板按名称幂等种入。 */
    private void seedFromResources() {
        ClassPathResource manifest = new ClassPathResource(TPL_DIR + "manifest.json");
        if (!manifest.exists()) {
            return;
        }
        TemplateEntry[] entries;
        try (InputStream in = manifest.getInputStream()) {
            entries = OM.readValue(new String(in.readAllBytes(), StandardCharsets.UTF_8), TemplateEntry[].class);
        } catch (Exception ex) {
            log.warn("解析模板清单失败: {}", ex.getMessage());
            return;
        }
        int n = 0;
        for (TemplateEntry e : entries) {
            String content = readResource(e.file());
            if (StringUtils.hasText(e.name()) && StringUtils.hasText(content)) {
                seed(e.type(), e.name(), content);
                n++;
            }
        }
        log.info("指导书内嵌模板加载完成: 处理 {} 个", n);
    }

    private String readResource(String file) {
        ClassPathResource res = new ClassPathResource(TPL_DIR + file);
        if (!res.exists()) {
            log.warn("模板正文文件不存在: {}", TPL_DIR + file);
            return null;
        }
        try (InputStream in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("读取模板正文失败: file={}, 原因={}", file, ex.getMessage());
            return null;
        }
    }

    private void seed(String type, String name, String content) {
        Long n = mapper.selectCount(new LambdaQueryWrapper<AitDocTemplate>()
                .eq(AitDocTemplate::getTemplateName, name));
        if (n != null && n > 0) {
            return;
        }
        AitDocTemplate t = new AitDocTemplate();
        t.setTemplateType(type);
        t.setTemplateName(name);
        t.setVersion("v1");
        t.setContent(content);
        t.setIsLatest(true);
        mapper.insert(t);
    }

    public PageResult<AitDocTemplate> page(PageQuery query, String type, String name, boolean onlyLatest) {
        LambdaQueryWrapper<AitDocTemplate> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(type), AitDocTemplate::getTemplateType, type)
                .like(StringUtils.hasText(name), AitDocTemplate::getTemplateName, name)
                .eq(onlyLatest, AitDocTemplate::getIsLatest, true)
                .orderByDesc(AitDocTemplate::getUpdateTime);
        IPage<AitDocTemplate> p = mapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    public AitDocTemplate getById(String id) {
        AitDocTemplate t = mapper.selectById(id);
        if (t == null) {
            throw new BizException("模板不存在");
        }
        return t;
    }

    /** 新建模板(v1)。 */
    @Transactional
    public String create(AitDocTemplate t) {
        if (!StringUtils.hasText(t.getTemplateName())) {
            throw new BizException("模板名称不能为空");
        }
        t.setTemplateId(null);
        t.setVersion("v1");
        t.setIsLatest(true);
        mapper.insert(t);
        return t.getTemplateId();
    }

    /** 在线编辑(就地更新当前版本正文/附件)。 */
    @Transactional
    public void updateContent(AitDocTemplate t) {
        if (!StringUtils.hasText(t.getTemplateId())) {
            throw new BizException("模板ID不能为空");
        }
        AitDocTemplate db = getById(t.getTemplateId());
        db.setTemplateName(StringUtils.hasText(t.getTemplateName()) ? t.getTemplateName() : db.getTemplateName());
        db.setTemplateType(StringUtils.hasText(t.getTemplateType()) ? t.getTemplateType() : db.getTemplateType());
        db.setContent(t.getContent());
        if (StringUtils.hasText(t.getFileData())) {
            db.setFileData(t.getFileData());
            db.setFileName(t.getFileName());
        }
        mapper.updateById(db);
    }

    /** 发布新版本(版本号自增,旧最新置否)。 */
    @Transactional
    public String newVersion(AitDocTemplate t) {
        if (!StringUtils.hasText(t.getTemplateName())) {
            throw new BizException("模板名称不能为空");
        }
        List<AitDocTemplate> history = mapper.selectList(new LambdaQueryWrapper<AitDocTemplate>()
                .eq(AitDocTemplate::getTemplateName, t.getTemplateName()));
        int max = 0;
        for (AitDocTemplate h : history) {
            max = Math.max(max, parseVer(h.getVersion()));
            if (Boolean.TRUE.equals(h.getIsLatest())) {
                h.setIsLatest(false);
                mapper.updateById(h);
            }
        }
        t.setTemplateId(null);
        t.setVersion("v" + (max + 1));
        t.setIsLatest(true);
        mapper.insert(t);
        return t.getTemplateId();
    }

    /** 设为最新版(同名其余置否)。 */
    @Transactional
    public void setLatest(String id) {
        AitDocTemplate t = getById(id);
        List<AitDocTemplate> sib = mapper.selectList(new LambdaQueryWrapper<AitDocTemplate>()
                .eq(AitDocTemplate::getTemplateName, t.getTemplateName()));
        for (AitDocTemplate s : sib) {
            boolean latest = s.getTemplateId().equals(id);
            if (!latest == Boolean.TRUE.equals(s.getIsLatest())) {
                s.setIsLatest(latest);
                mapper.updateById(s);
            }
        }
    }

    public List<AitDocTemplate> versions(String templateName) {
        return mapper.selectList(new LambdaQueryWrapper<AitDocTemplate>()
                .eq(AitDocTemplate::getTemplateName, templateName)
                .orderByDesc(AitDocTemplate::getVersion));
    }

    /** 下载:有附件则解码 Base64,否则导出在线编辑正文。 */
    public byte[] download(String id) {
        AitDocTemplate t = getById(id);
        if (StringUtils.hasText(t.getFileData())) {
            try {
                return Base64.getDecoder().decode(t.getFileData());
            } catch (IllegalArgumentException ignore) {
                // 非 Base64,退回正文
            }
        }
        return (t.getContent() == null ? "" : t.getContent()).getBytes(StandardCharsets.UTF_8);
    }

    private static int parseVer(String v) {
        if (v == null) {
            return 0;
        }
        try {
            return Integer.parseInt(v.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
