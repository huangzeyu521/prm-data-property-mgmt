package com.csg.prm.confirm.aitool.controller;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.Result;
import com.csg.prm.common.auth.RequiresRole;
import com.csg.prm.common.query.PageRequest;
import com.csg.prm.confirm.aitool.entity.AitKbChunk;
import com.csg.prm.confirm.aitool.entity.AitKbDoc;
import com.csg.prm.confirm.aitool.service.AitKbService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 产权知识库与检索增强接口(可研 2.1)。检索/RAG 开放;知识库治理需管理员。
 */
@RestController
@Validated
@RequestMapping("/api/dpr/confirm/aitool/kb")
public class AitKbController {

    private final AitKbService service;

    public AitKbController(AitKbService service) {
        this.service = service;
    }

    // ---- 检索 / RAG ----

    /** #3 检索:mode=keyword|semantic|hybrid;可按 domain/docType 选择性召回。 */
    @PostMapping("/search")
    public Result<List<AitKbService.SearchHit>> search(@RequestBody Map<String, Object> body) {
        return Result.success(service.search(str(body, "query"), str(body, "mode"),
                str(body, "domain"), str(body, "docType"), intVal(body, "topK", 5)));
    }

    /** #4/#5 RAG 知识增强生成:真实检索命中 → 大模型生成 + 真实引用。 */
    @PostMapping("/rag")
    public Result<AitKbService.RagResult> rag(@RequestBody Map<String, Object> body) {
        return Result.success(service.rag(str(body, "query"), str(body, "domain")));
    }

    // ---- 知识库浏览 ----

    @GetMapping("/doc/page")
    public Result<PageResult<AitKbDoc>> docPage(@Valid PageRequest query,
                                           @RequestParam(required = false) String docType,
                                           @RequestParam(required = false) String domain) {
        return Result.success(service.docPage(query, docType, domain));
    }

    @GetMapping("/doc/{docId}/chunks")
    public Result<List<AitKbChunk>> chunks(@PathVariable String docId) {
        return Result.success(service.chunks(docId));
    }

    @GetMapping("/doc/versions")
    public Result<List<AitKbDoc>> versions(@RequestParam String title) {
        return Result.success(service.versions(title));
    }

    // ---- 治理(管理员)----

    @RequiresRole({"admin"})
    @PostMapping("/doc")
    public Result<String> addDoc(@Valid @RequestBody AitKbDoc doc) {
        return Result.success(service.addDoc(doc, doc.getContent()));
    }

    @RequiresRole({"admin"})
    @PostMapping("/doc/new-version")
    public Result<String> newVersion(@Valid @RequestBody AitKbDoc doc) {
        return Result.success(service.newVersion(doc, doc.getContent()));
    }

    @RequiresRole({"admin"})
    @PostMapping("/doc/{docId}/replace-clause")
    public Result<Void> replaceClause(@PathVariable String docId, @RequestBody Map<String, String> body) {
        service.replaceClause(docId, body.get("clauseNo"), body.get("content"));
        return Result.success();
    }

    @RequiresRole({"admin"})
    @PostMapping("/doc/{docId}/invalidate")
    public Result<Void> invalidate(@PathVariable String docId) {
        service.invalidate(docId);
        return Result.success();
    }

    @RequiresRole({"admin"})
    @PostMapping("/doc/{docId}/rollback")
    public Result<Void> rollback(@PathVariable String docId) {
        service.rollback(docId);
        return Result.success();
    }

    private static String str(Map<String, Object> m, String k) {
        Object v = m == null ? null : m.get(k);
        return v == null ? null : v.toString();
    }

    private static int intVal(Map<String, Object> m, String k, int dft) {
        Object v = m == null ? null : m.get(k);
        if (v instanceof Number num) {
            return num.intValue();
        }
        try {
            return v == null ? dft : Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return dft;
        }
    }
}
