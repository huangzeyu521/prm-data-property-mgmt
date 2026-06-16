package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitKbChunk;
import com.csg.prm.confirm.aitool.entity.AitKbDoc;
import com.csg.prm.confirm.aitool.service.AitKbCorpusLoader;
import com.csg.prm.confirm.aitool.service.AitKbService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 2.1#1/#6 产权知识体系语料长久入库验证:
 * 南网确权授权/对外开放/数据产品三本业务指导书在启动时入库、切片向量化、可检索可 RAG、二次加载幂等。
 */
@SpringBootTest
@ActiveProfiles("test")
class AitKbCorpusLoaderTest {

    private static final String F_TITLE = "中国南方电网有限责任公司数据确权授权业务指导书";
    private static final String G_TITLE = "中国南方电网有限责任公司数据对外开放管理业务指导书";
    private static final String H_TITLE = "中国南方电网有限责任公司数据产品管理业务指导书";

    @Autowired private AitKbService kb;
    @Autowired private AitKbCorpusLoader loader;

    /** #1 三本业务指导书均已入库,且元数据为 内部制度/数据确权。 */
    @Test
    void three_guidance_docs_loaded_with_metadata() {
        for (String title : List.of(F_TITLE, G_TITLE, H_TITLE)) {
            List<AitKbDoc> vs = kb.versions(title);
            assertFalse(vs.isEmpty(), "应已入库:" + title);
            AitKbDoc d = vs.get(0);
            assertEquals("内部制度", d.getDocType(), "类型应为内部制度:" + title);
            assertEquals("数据确权", d.getDomain(), "知识域应为数据确权:" + title);
            assertEquals(AitKbDoc.STATUS_VALID, d.getStatus(), "应为有效:" + title);
            assertTrue(Boolean.TRUE.equals(d.getIsLatest()), "应为最新版:" + title);
        }
    }

    /** #2 整篇指导书已切片向量化(切片数应较多)。 */
    @Test
    void guidance_chunked_and_vectorized() {
        String docId = kb.versions(F_TITLE).get(0).getDocId();
        List<AitKbChunk> chunks = kb.chunks(docId);
        assertTrue(chunks.size() > 50, "F 指导书应切出较多条款切片,实际=" + chunks.size());
        assertTrue(chunks.stream().anyMatch(c -> c.getContent().contains("三权分置")), "应含三权分置切片");
        assertTrue(chunks.stream().allMatch(c -> c.getVectorDim() != null && c.getVectorDim() > 0),
                "每个切片应已向量化(本地 hash 向量)");
    }

    /** #3 关键词检索可召回指导书核心条款,引用指向 F。 */
    @Test
    void keyword_recall_hits_guidance() {
        List<AitKbService.SearchHit> hits = kb.search("三权分置先确后授", AitKbService.MODE_KEYWORD, null, null, 5);
        assertFalse(hits.isEmpty(), "应召回含三权分置/先确后授的条款");
        assertTrue(hits.stream().anyMatch(h -> F_TITLE.equals(h.title())),
                "命中应包含确权授权业务指导书");
    }

    /** #3 按数据确权域召回对外开放/数据产品指导书内容。 */
    @Test
    void domain_recall_open_and_product() {
        assertFalse(kb.search("数据对外开放原则", AitKbService.MODE_HYBRID, "数据确权", null, 5).isEmpty(),
                "应召回对外开放相关条款");
        assertFalse(kb.search("数据产品管理", AitKbService.MODE_HYBRID, "数据确权", null, 5).isEmpty(),
                "应召回数据产品相关条款");
    }

    /** #4/#5 RAG 基于指导书条款生成并给出真实引用。 */
    @Test
    void rag_grounded_on_guidance() {
        AitKbService.RagResult r = kb.rag("数据确权授权应遵循哪些基本原则", "数据确权");
        assertFalse(r.hits().isEmpty(), "RAG 应有检索命中");
        assertFalse(r.citations().isEmpty(), "RAG 应给出真实引用");
    }

    /** #6 二次加载幂等:已存在的指导书不重复入库。 */
    @Test
    void reload_is_idempotent() {
        int before = kb.versions(F_TITLE).size();
        loader.run(null);
        int after = kb.versions(F_TITLE).size();
        assertEquals(before, after, "二次加载不应产生重复文档版本");
    }
}
