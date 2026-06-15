package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.ai.DawatAiGateway;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitKbChunk;
import com.csg.prm.confirm.aitool.entity.AitKbDoc;
import com.csg.prm.confirm.aitool.gateway.AiToolParseGateway;
import com.csg.prm.confirm.aitool.mapper.AitKbChunkMapper;
import com.csg.prm.confirm.aitool.mapper.AitKbDocMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 产权知识库与检索增强服务(可研 2.1):
 * 多类型知识库 + 知识域(#1)、切片/标签/向量/元数据(#2)、关键词/语义/混合检索(#3)、
 * 检索结果关联输出依据(#4)、RAG 知识增强生成(#5)、增量/失效替换/版本回溯(#6)。
 */
@Service
public class AitKbService implements ApplicationRunner {

    public static final String MODE_KEYWORD = "keyword";
    public static final String MODE_SEMANTIC = "semantic";
    public static final String MODE_HYBRID = "hybrid";
    private static final ObjectMapper OM = new ObjectMapper();

    private final AitKbDocMapper docMapper;
    private final AitKbChunkMapper chunkMapper;
    private final AiToolParseGateway gateway;
    private final DawatAiGateway ai;

    public AitKbService(AitKbDocMapper docMapper, AitKbChunkMapper chunkMapper,
                        AiToolParseGateway gateway, DawatAiGateway ai) {
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
        this.gateway = gateway;
        this.ai = ai;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long n = docMapper.selectCount(new LambdaQueryWrapper<>());
        if (n != null && n > 0) {
            return;
        }
        // #1 内置数据安全/个人信息保护/网络安全/数据确权 基础语料(多类型知识库 + 知识域)
        seed("法规", "数据安全", "中华人民共和国数据安全法", "全国人大常委会", "2021-09-01", "全域",
                "第二十一条 国家建立数据分类分级保护制度,对数据实行分类分级保护。\n"
                + "第二十七条 开展数据处理活动应当依照法律法规建立健全全流程数据安全管理制度。\n"
                + "第三十二条 任何组织个人收集数据应采取合法正当方式,不得窃取或以其他非法方式获取数据。");
        seed("法规", "个人信息保护", "中华人民共和国个人信息保护法", "全国人大常委会", "2021-11-01", "含个人信息场景",
                "第十三条 取得个人同意方可处理个人信息,法律另有规定的除外。\n"
                + "第二十八条 敏感个人信息处理应具有特定目的和充分必要性,并采取严格保护措施。\n"
                + "第五十一条 个人信息处理者应采取加密去标识化等安全技术措施。");
        seed("法规", "网络安全", "中华人民共和国网络安全法", "全国人大常委会", "2017-06-01", "网络与数据基础设施",
                "第二十一条 国家实行网络安全等级保护制度,保障网络免受干扰破坏或未经授权的访问。\n"
                + "第四十二条 网络运营者不得泄露篡改毁损其收集的个人信息,应采取技术措施确保安全。");
        seed("法规", "数据确权", "关于构建数据基础制度更好发挥数据要素作用的意见(数据二十条)", "中共中央国务院", "2022-12-19", "数据要素全域",
                "建立数据资源持有权数据加工使用权数据产品经营权等分置的产权运行机制。\n"
                + "推进实施公共数据确权授权机制,保障数据持有主体合法权益。\n"
                + "对各类市场主体在生产经营活动中产生的数据,依法依规予以保护。");
        seed("内部制度", "数据确权", "南方电网数据确权授权业务指导书", "中国南方电网", "2024-01-01", "南网数据资产",
                "确权遵循先确权后授权原则,授权范围不得超出确权边界。\n"
                + "数据确权应核验权属证明材料并加盖公章,留存确权结论并上链存证。");
        seed("审核规则", "数据确权", "数据确权材料审核规则", "数据资产管理部", "2024-06-01", "确权审核",
                "权属证明缺少公章的材料判定为存疑,需补充盖章版。\n"
                + "材料权属类型与申请表单不一致的,应退回申请人核对。");
        seed("典型案例", "数据确权", "典型数据确权争议案例汇编", "数据资产管理部", "2024-09-01", "争议处置参考",
                "案例:同一资产存在两个权属主体主张持有权,经三权分置协商划归唯一持有主体后确权。");
        // #1 行业标准类型基础语料(覆盖 数据安全 / 个人信息保护 知识域)
        seed("行业标准", "数据安全", "信息安全技术 网络数据分类分级要求(GB/T 43697-2024)", "全国信息安全标准化技术委员会", "2024-10-01", "数据分类分级",
                "数据按重要程度分为一般数据、重要数据、核心数据,实行分类分级保护。\n"
                + "确定数据级别应综合考虑数据规模、精度、领域及遭篡改泄露后的危害程度。\n"
                + "重要数据与核心数据的处理活动应进行风险评估并落实加强保护措施。");
        seed("行业标准", "个人信息保护", "信息安全技术 个人信息安全规范(GB/T 35273-2020)", "全国信息安全标准化技术委员会", "2020-10-01", "含个人信息处理",
                "收集个人信息应遵循最小必要原则,仅收集实现业务功能所必需的最少信息。\n"
                + "处理个人敏感信息前应取得个人信息主体的明示同意,并采取加密等安全措施。\n"
                + "共享、转让个人信息应进行个人信息安全影响评估并留存记录。");
    }

    private void seed(String type, String domain, String title, String source, String date, String scope, String content) {
        AitKbDoc doc = new AitKbDoc();
        doc.setDocType(type);
        doc.setDomain(domain);
        doc.setTitle(title);
        doc.setSource(source);
        doc.setEffectiveDate(date);
        doc.setScope(scope);
        addDoc(doc, content);
    }

    /** #2 新增知识文档:建文档(v1/有效)+ 分段切片 + 向量化入库。 */
    @Transactional
    public String addDoc(AitKbDoc doc, String content) {
        if (!StringUtils.hasText(doc.getTitle())) {
            throw new BizException("文档标题不能为空");
        }
        doc.setDocId(null);
        doc.setVersion(StringUtils.hasText(doc.getVersion()) ? doc.getVersion() : "v1");
        doc.setIsLatest(true);
        doc.setStatus(AitKbDoc.STATUS_VALID);
        doc.setContent(content);
        docMapper.insert(doc);
        chunkAndStore(doc, content);
        return doc.getDocId();
    }

    /** 分段(按行/条)→ 标签 → 向量 → 落库。 */
    private void chunkAndStore(AitKbDoc doc, String content) {
        if (!StringUtils.hasText(content)) {
            return;
        }
        String[] lines = content.split("\\r?\\n");
        int seq = 0;
        for (String line : lines) {
            String text = line.trim();
            if (text.isEmpty()) {
                continue;
            }
            seq++;
            AitKbChunk c = new AitKbChunk();
            c.setDocId(doc.getDocId());
            c.setDocType(doc.getDocType());
            c.setDomain(doc.getDomain());
            c.setTitle(doc.getTitle());
            c.setClauseNo(detectClause(text));
            c.setTags(doc.getDomain() + "," + doc.getDocType());
            c.setContent(text);
            c.setEffectiveDate(doc.getEffectiveDate());
            c.setScope(doc.getScope());
            c.setStatus(AitKbChunk.STATUS_VALID);
            c.setSeq(seq);
            float[] vec = safeEmbed(text);
            c.setVectorJson(toJson(vec));
            c.setVectorDim(vec == null ? 0 : vec.length);
            chunkMapper.insert(c);
        }
    }

    private static String detectClause(String text) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(第[\\d一二三四五六七八九十百零]+条)").matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        if (text.startsWith("案例")) {
            return "案例";
        }
        return null;
    }

    // ---- 检索(#3) ----

    public record SearchHit(String chunkId, String docId, String docType, String domain, String title,
                            String clauseNo, String content, String effectiveDate, String scope,
                            double score, String citation) {
    }

    public List<SearchHit> search(String query, String mode, String domain, String docType, int topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        String m = StringUtils.hasText(mode) ? mode : MODE_HYBRID;
        int k = topK <= 0 ? 5 : topK;
        LambdaQueryWrapper<AitKbChunk> w = new LambdaQueryWrapper<AitKbChunk>()
                .eq(AitKbChunk::getStatus, AitKbChunk.STATUS_VALID)
                .eq(StringUtils.hasText(domain), AitKbChunk::getDomain, domain)
                .eq(StringUtils.hasText(docType), AitKbChunk::getDocType, docType);
        List<AitKbChunk> candidates = chunkMapper.selectList(w);
        if (candidates.isEmpty()) {
            return List.of();
        }
        float[] qvec = MODE_KEYWORD.equals(m) ? null : safeEmbed(query);
        List<String> qGrams = bigrams(query);

        List<SearchHit> hits = new ArrayList<>();
        for (AitKbChunk c : candidates) {
            double kw = keywordScore(qGrams, c.getContent());
            double sem = (qvec == null) ? 0 : cosine(qvec, parseVec(c.getVectorJson(), c.getVectorDim()));
            double score = switch (m) {
                case MODE_KEYWORD -> kw;
                case MODE_SEMANTIC -> sem;
                default -> 0.5 * kw + 0.5 * sem;
            };
            if (score <= 0) {
                continue;
            }
            hits.add(new SearchHit(c.getChunkId(), c.getDocId(), c.getDocType(), c.getDomain(), c.getTitle(),
                    c.getClauseNo(), c.getContent(), c.getEffectiveDate(), c.getScope(),
                    round(score), citation(c)));
        }
        hits.sort(Comparator.comparingDouble(SearchHit::score).reversed());
        return hits.size() > k ? hits.subList(0, k) : hits;
    }

    private static String citation(AitKbChunk c) {
        return "《" + c.getTitle() + "》" + (StringUtils.hasText(c.getClauseNo()) ? " " + c.getClauseNo() : "");
    }

    // ---- RAG 生成(#4/#5) ----

    public record RagResult(String answer, List<SearchHit> hits, List<String> citations, double confidence) {
    }

    /** #5 基于知识库的 RAG:真实检索命中 → 拼接依据上下文 → 大模型生成,citations 来自真实命中。 */
    public RagResult rag(String query, String domain) {
        List<SearchHit> hits = search(query, MODE_HYBRID, domain, null, 5);
        List<String> citations = new ArrayList<>();
        StringBuilder ctx = new StringBuilder("【问题】").append(query).append("\n【检索到的产权知识依据】\n");
        for (SearchHit h : hits) {
            ctx.append("- ").append(h.citation()).append(":").append(h.content()).append('\n');
            citations.add(h.citation());
        }
        ctx.append("\n请仅基于以上依据作答,逐条引用依据条款,不要脱离依据泛化。");
        String answer;
        double conf = 0.6;
        try {
            DawatAiGateway.RagAnswer ans = ai.ask(ctx.toString());
            answer = ans.answer();
            conf = ans.confidence();
        } catch (RuntimeException e) {
            answer = hits.isEmpty()
                    ? "知识库未检索到相关依据,建议补充知识库或人工研判。"
                    : "基于检索到的依据,核心依据为:" + String.join(";", citations) + "。请结合上述条款作出审核结论。";
        }
        return new RagResult(answer, hits, citations, conf);
    }

    // ---- 治理(#6) ----

    /** 发布新版本:旧版本文档 isLatest=false 且其切片失效;新版本切片重新向量化。 */
    @Transactional
    public String newVersion(AitKbDoc doc, String content) {
        if (!StringUtils.hasText(doc.getTitle())) {
            throw new BizException("文档标题不能为空");
        }
        List<AitKbDoc> history = docMapper.selectList(new LambdaQueryWrapper<AitKbDoc>()
                .eq(AitKbDoc::getTitle, doc.getTitle()));
        int max = 0;
        for (AitKbDoc h : history) {
            max = Math.max(max, parseVer(h.getVersion()));
            if (Boolean.TRUE.equals(h.getIsLatest())) {
                h.setIsLatest(false);
                docMapper.updateById(h);
                invalidateChunks(h.getDocId());
            }
        }
        doc.setVersion("v" + (max + 1));
        return addDoc(doc, content);
    }

    /** 失效条款替换:旧条款切片置失效,插入同条款号新内容(重新向量化)。 */
    @Transactional
    public void replaceClause(String docId, String clauseNo, String newContent) {
        AitKbDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        List<AitKbChunk> old = chunkMapper.selectList(new LambdaQueryWrapper<AitKbChunk>()
                .eq(AitKbChunk::getDocId, docId).eq(AitKbChunk::getClauseNo, clauseNo)
                .eq(AitKbChunk::getStatus, AitKbChunk.STATUS_VALID));
        for (AitKbChunk c : old) {
            c.setStatus(AitKbChunk.STATUS_INVALID);
            chunkMapper.updateById(c);
        }
        AitKbChunk c = new AitKbChunk();
        c.setDocId(docId);
        c.setDocType(doc.getDocType());
        c.setDomain(doc.getDomain());
        c.setTitle(doc.getTitle());
        c.setClauseNo(clauseNo);
        c.setTags(doc.getDomain() + "," + doc.getDocType());
        c.setContent(newContent);
        c.setEffectiveDate(doc.getEffectiveDate());
        c.setScope(doc.getScope());
        c.setStatus(AitKbChunk.STATUS_VALID);
        c.setSeq(0);
        float[] vec = safeEmbed(newContent);
        c.setVectorJson(toJson(vec));
        c.setVectorDim(vec == null ? 0 : vec.length);
        chunkMapper.insert(c);
    }

    /** 失效整篇文档(下线)。 */
    @Transactional
    public void invalidate(String docId) {
        AitKbDoc doc = docMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        doc.setStatus(AitKbDoc.STATUS_INVALID);
        doc.setIsLatest(false);
        docMapper.updateById(doc);
        invalidateChunks(docId);
    }

    /** 版本回溯:把指定版本设为最新并恢复其切片,同名其余版本下线。 */
    @Transactional
    public void rollback(String docId) {
        AitKbDoc target = docMapper.selectById(docId);
        if (target == null) {
            throw new BizException("文档不存在");
        }
        List<AitKbDoc> sib = docMapper.selectList(new LambdaQueryWrapper<AitKbDoc>()
                .eq(AitKbDoc::getTitle, target.getTitle()));
        for (AitKbDoc s : sib) {
            boolean isTarget = s.getDocId().equals(docId);
            s.setIsLatest(isTarget);
            s.setStatus(isTarget ? AitKbDoc.STATUS_VALID : AitKbDoc.STATUS_INVALID);
            docMapper.updateById(s);
            setChunksStatus(s.getDocId(), isTarget ? AitKbChunk.STATUS_VALID : AitKbChunk.STATUS_INVALID);
        }
    }

    public List<AitKbDoc> versions(String title) {
        return docMapper.selectList(new LambdaQueryWrapper<AitKbDoc>()
                .eq(AitKbDoc::getTitle, title).orderByDesc(AitKbDoc::getVersion));
    }

    public PageResult<AitKbDoc> docPage(PageQuery query, String docType, String domain) {
        LambdaQueryWrapper<AitKbDoc> w = new LambdaQueryWrapper<AitKbDoc>()
                .eq(StringUtils.hasText(docType), AitKbDoc::getDocType, docType)
                .eq(StringUtils.hasText(domain), AitKbDoc::getDomain, domain)
                .orderByDesc(AitKbDoc::getUpdateTime);
        IPage<AitKbDoc> p = docMapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    public List<AitKbChunk> chunks(String docId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<AitKbChunk>()
                .eq(AitKbChunk::getDocId, docId).orderByAsc(AitKbChunk::getSeq));
    }

    private void invalidateChunks(String docId) {
        setChunksStatus(docId, AitKbChunk.STATUS_INVALID);
    }

    private void setChunksStatus(String docId, String status) {
        List<AitKbChunk> list = chunkMapper.selectList(new LambdaQueryWrapper<AitKbChunk>()
                .eq(AitKbChunk::getDocId, docId));
        for (AitKbChunk c : list) {
            c.setStatus(status);
            chunkMapper.updateById(c);
        }
    }

    // ---- 向量/打分工具 ----

    private float[] safeEmbed(String text) {
        try {
            float[] v = gateway.embed(text);
            return v == null ? new float[0] : v;
        } catch (RuntimeException e) {
            return new float[0];
        }
    }

    private static String toJson(float[] v) {
        try {
            return OM.writeValueAsString(v);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static float[] parseVec(String json, Integer dim) {
        if (!StringUtils.hasText(json)) {
            return new float[0];
        }
        try {
            return OM.readValue(json, float[].class);
        } catch (Exception e) {
            return new float[0];
        }
    }

    private static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static List<String> bigrams(String s) {
        List<String> out = new ArrayList<>();
        if (s == null) {
            return out;
        }
        String t = s.trim();
        for (int i = 0; i + 1 < t.length(); i++) {
            out.add(t.substring(i, i + 2));
        }
        if (out.isEmpty() && !t.isEmpty()) {
            out.add(t);
        }
        return out;
    }

    private static double keywordScore(List<String> qGrams, String content) {
        if (qGrams.isEmpty() || !StringUtils.hasText(content)) {
            return 0;
        }
        int hit = 0;
        for (String g : qGrams) {
            if (content.contains(g)) {
                hit++;
            }
        }
        return (double) hit / qGrams.size();
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
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
