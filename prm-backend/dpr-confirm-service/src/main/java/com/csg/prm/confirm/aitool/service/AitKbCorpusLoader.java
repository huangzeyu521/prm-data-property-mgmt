package com.csg.prm.confirm.aitool.service;

import com.csg.prm.confirm.aitool.entity.AitKbDoc;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 产权知识体系语料长久加载器(可研 2.1#1/#6):
 * 把仓库内的权威制度语料(南网确权授权/对外开放/数据产品业务指导书等,见 aitool/kb/manifest.json)
 * 在每次启动时按标题幂等入库 —— 全新库自动种入、既有库只补缺、已存在则跳过,不依赖 DB 方言种子。
 * 排在 {@link AitKbService} 内置基础语料之后执行。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AitKbCorpusLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AitKbCorpusLoader.class);
    private static final String KB_DIR = "aitool/kb/";
    private static final String MANIFEST = KB_DIR + "manifest.json";
    private static final ObjectMapper OM = new ObjectMapper();

    private final AitKbService kb;

    public AitKbCorpusLoader(AitKbService kb) {
        this.kb = kb;
    }

    /** 清单条目:文档元数据 + 正文资源文件名。 */
    public record CorpusEntry(String docType, String domain, String title, String source,
                              String effectiveDate, String scope, String version, String file) {
    }

    @Override
    public void run(ApplicationArguments args) {
        List<CorpusEntry> entries = readManifest();
        if (entries.isEmpty()) {
            return;
        }
        int ingested = 0;
        int skipped = 0;
        for (CorpusEntry e : entries) {
            try {
                if (ingestOne(e)) {
                    ingested++;
                } else {
                    skipped++;
                }
            } catch (RuntimeException ex) {
                log.warn("知识语料入库失败,跳过: title={}, file={}, 原因={}", e.title(), e.file(), ex.getMessage());
            }
        }
        log.info("产权知识语料加载完成: 新增 {} 篇, 已存在跳过 {} 篇", ingested, skipped);
    }

    /** @return true=本次新增入库;false=已存在或正文缺失而跳过。 */
    private boolean ingestOne(CorpusEntry e) {
        if (!StringUtils.hasText(e.title()) || !StringUtils.hasText(e.file())) {
            log.warn("知识语料清单条目缺少 title/file,跳过: {}", e);
            return false;
        }
        // 幂等:同名文档已存在则不重复入库(既有库只补缺)
        if (!kb.versions(e.title()).isEmpty()) {
            return false;
        }
        String content = readContent(e.file());
        if (!StringUtils.hasText(content)) {
            log.warn("知识语料正文为空,跳过: file={}", e.file());
            return false;
        }
        AitKbDoc doc = new AitKbDoc();
        doc.setDocType(e.docType());
        doc.setDomain(e.domain());
        doc.setTitle(e.title());
        doc.setSource(e.source());
        doc.setEffectiveDate(e.effectiveDate());
        doc.setScope(e.scope());
        doc.setVersion(StringUtils.hasText(e.version()) ? e.version() : "v1");
        kb.addDoc(doc, content);
        log.info("产权知识语料入库: 《{}》[{}/{}]", e.title(), e.docType(), e.domain());
        return true;
    }

    private List<CorpusEntry> readManifest() {
        ClassPathResource res = new ClassPathResource(MANIFEST);
        if (!res.exists()) {
            log.info("未找到知识语料清单 {},跳过语料加载", MANIFEST);
            return List.of();
        }
        try (InputStream in = res.getInputStream()) {
            byte[] bytes = in.readAllBytes();
            String json = new String(bytes, StandardCharsets.UTF_8);
            CorpusEntry[] arr = OM.readValue(json, CorpusEntry[].class);
            return List.of(arr);
        } catch (Exception ex) {
            log.warn("解析知识语料清单失败: {}", ex.getMessage());
            return List.of();
        }
    }

    private String readContent(String file) {
        ClassPathResource res = new ClassPathResource(KB_DIR + file);
        if (!res.exists()) {
            log.warn("知识语料正文文件不存在: {}", KB_DIR + file);
            return null;
        }
        try (InputStream in = res.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("读取知识语料正文失败: file={}, 原因={}", file, ex.getMessage());
            return null;
        }
    }
}
