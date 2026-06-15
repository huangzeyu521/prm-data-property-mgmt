package com.csg.prm.confirm.aitool;

import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitKbDoc;
import com.csg.prm.confirm.aitool.service.AitKbService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2.1 知识库构建与检索增强(#1~#6)能力测试。test profile → 本地确定性 hash 向量,可重复。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitKbTest {

    @Autowired private AitKbService kb;

    /** #1 多类型知识库 + 知识域 + 内置基础语料。 */
    @Test
    void seed_corpus_with_domains() {
        assertTrue(kb.docPage(new PageQuery(), null, null).getTotal() >= 7, "应种入多篇基础语料");
        assertFalse(kb.docPage(new PageQuery(), null, "数据安全").getRecords().isEmpty(), "应含数据安全域");
        assertFalse(kb.docPage(new PageQuery(), null, "个人信息保护").getRecords().isEmpty(), "应含个人信息保护域");
        assertFalse(kb.docPage(new PageQuery(), null, "网络安全").getRecords().isEmpty(), "应含网络安全域");
        assertFalse(kb.docPage(new PageQuery(), "法规", null).getRecords().isEmpty(), "应含法规类型");
    }

    /** #3 关键词检索。 */
    @Test
    void keyword_search() {
        List<AitKbService.SearchHit> hits = kb.search("公章", AitKbService.MODE_KEYWORD, null, null, 5);
        assertFalse(hits.isEmpty(), "关键词应召回含'公章'的条款");
        assertTrue(hits.get(0).content().contains("公章"));
    }

    /** #3 语义检索(本地 hash 向量,词面重叠近似语义)。 */
    @Test
    void semantic_search_ranks_related() {
        List<AitKbService.SearchHit> hits = kb.search("取得个人同意处理个人信息", AitKbService.MODE_SEMANTIC, null, null, 3);
        assertFalse(hits.isEmpty(), "语义检索应有召回");
        assertTrue(hits.get(0).title().contains("个人信息保护法"), "语义最相关应为个人信息保护法,实际=" + hits.get(0).title());
    }

    /** #1/#3 按知识域选择性召回。 */
    @Test
    void domain_selective_recall() {
        List<AitKbService.SearchHit> hits = kb.search("等级保护制度", AitKbService.MODE_HYBRID, "网络安全", null, 5);
        assertFalse(hits.isEmpty(), "网络安全域应召回");
        assertTrue(hits.stream().allMatch(h -> "网络安全".equals(h.domain())), "应只召回网络安全域");
    }

    /** #4/#5 RAG:真实检索命中 + 引用。 */
    @Test
    void rag_grounded_with_citations() {
        AitKbService.RagResult r = kb.rag("数据确权与授权的先后关系", null);
        assertFalse(r.hits().isEmpty(), "RAG 应有检索命中");
        assertFalse(r.citations().isEmpty(), "RAG 应给出真实引用");
        assertNotNull(r.answer(), "RAG 应有生成回答");
    }

    /** #6 增量新增 + 版本升级 + 失效旧版 + 版本回溯。 */
    @Test
    void governance_versioning_and_rollback() {
        String title = "自定义知识-" + System.nanoTime();
        AitKbDoc d1 = new AitKbDoc();
        d1.setDocType("内部制度");
        d1.setDomain("数据确权");
        d1.setTitle(title);
        d1.setContent("第一条 旧版条款ALPHA内容");
        kb.addDoc(d1, d1.getContent());
        assertFalse(kb.search("ALPHA", AitKbService.MODE_KEYWORD, null, null, 5).isEmpty(), "新增后应可检索");

        AitKbDoc d2 = new AitKbDoc();
        d2.setDocType("内部制度");
        d2.setDomain("数据确权");
        d2.setTitle(title);
        d2.setContent("第一条 新版条款BETA内容");
        kb.newVersion(d2, d2.getContent());

        assertTrue(kb.versions(title).size() >= 2, "应有两个版本");
        assertFalse(kb.search("BETA", AitKbService.MODE_KEYWORD, null, null, 5).isEmpty(), "应检索到新版");
        assertTrue(kb.search("ALPHA", AitKbService.MODE_KEYWORD, null, null, 5).isEmpty(), "旧版条款应已失效");

        // 失效条款替换
        String latestDocId = kb.versions(title).get(0).getDocId();
        kb.replaceClause(latestDocId, "第一条", "第一条 替换条款GAMMA内容");
        assertFalse(kb.search("GAMMA", AitKbService.MODE_KEYWORD, null, null, 5).isEmpty(), "替换后应检索到新条款");
        assertTrue(kb.search("BETA", AitKbService.MODE_KEYWORD, null, null, 5).isEmpty(), "被替换条款应失效");

        // 整篇失效
        kb.invalidate(latestDocId);
        assertTrue(kb.search("GAMMA", AitKbService.MODE_KEYWORD, null, null, 5).isEmpty(), "失效后不应检索到");

        // 版本回溯(恢复该版本)
        kb.rollback(latestDocId);
        assertFalse(kb.search("GAMMA", AitKbService.MODE_KEYWORD, null, null, 5).isEmpty(), "回溯后应恢复检索");
    }
}
