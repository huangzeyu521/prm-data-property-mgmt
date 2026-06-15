package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.crypto.Sm3Util;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitAuditBase;
import com.csg.prm.confirm.aitool.entity.AitAuditResult;
import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitDecision;
import com.csg.prm.confirm.aitool.entity.AitDocSegment;
import com.csg.prm.confirm.aitool.entity.AitEvidence;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.mapper.AitAuditBaseMapper;
import com.csg.prm.confirm.aitool.mapper.AitAuditResultMapper;
import com.csg.prm.confirm.aitool.mapper.AitConflictMapper;
import com.csg.prm.confirm.aitool.mapper.AitDocSegmentMapper;
import com.csg.prm.confirm.aitool.mapper.AitEvidenceMapper;
import com.csg.prm.confirm.aitool.mapper.AitMaterialMapper;
import com.csg.prm.confirm.aitool.mapper.AitParseResultMapper;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 智能确权 Agent(可研 3.1):以 Agent 编排多阶段审核链路 —
 * ①数据分类分级 ②合规扫描 ③授权判断 ④风险识别 ⑤结论生成;
 * 规则预筛后 低风险走快速通道 / 复杂对象走深度审核(规则预判+知识召回+模型研判+结果校核);
 * 工具式调用 解析(OCR/NLP)/规则引擎/冲突检测/知识库检索/大模型,按任务适配模型;
 * 输出 分类/级别/权利类型/授权建议/限制条件/风险等级/补正建议 + 理由/命中依据/建议动作。
 */
@Service
public class AitAuditAgentService {

    private static final ObjectMapper OM = new ObjectMapper();

    private final ConfirmApplyMapper applyMapper;
    private final AitMaterialMapper materialMapper;
    private final AitParseResultMapper parseMapper;
    private final AitConflictMapper conflictMapper;
    private final AitDecisionService decisionService;
    private final AitKbService kbService;
    private final AitAuditResultMapper auditMapper;
    private final AitEvidenceMapper evidenceMapper;
    private final AitDocSegmentMapper segmentMapper;
    private final AitAuditBaseMapper auditBaseMapper;

    public AitAuditAgentService(ConfirmApplyMapper applyMapper, AitMaterialMapper materialMapper,
                                AitParseResultMapper parseMapper, AitConflictMapper conflictMapper,
                                AitDecisionService decisionService, AitKbService kbService,
                                AitAuditResultMapper auditMapper, AitEvidenceMapper evidenceMapper,
                                AitDocSegmentMapper segmentMapper, AitAuditBaseMapper auditBaseMapper) {
        this.applyMapper = applyMapper;
        this.materialMapper = materialMapper;
        this.parseMapper = parseMapper;
        this.conflictMapper = conflictMapper;
        this.decisionService = decisionService;
        this.kbService = kbService;
        this.auditMapper = auditMapper;
        this.evidenceMapper = evidenceMapper;
        this.segmentMapper = segmentMapper;
        this.auditBaseMapper = auditBaseMapper;
    }

    @Transactional
    public AitAuditResult audit(String applyId) {
        ConfirmApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException("确权申请不存在");
        }
        String assetId = apply.getAssetId();
        List<Map<String, Object>> stages = new ArrayList<>();
        List<Map<String, Object>> tools = new ArrayList<>();

        // 阶段① 数据分类分级(工具:智能解析 OCR/NLP)
        List<AitMaterial> materials = materialMapper.selectList(
                new LambdaQueryWrapper<AitMaterial>().eq(AitMaterial::getApplyId, applyId));
        List<AitParseResult> parses = new ArrayList<>();
        for (AitMaterial m : materials) {
            AitParseResult r = parseMapper.selectOne(
                    new LambdaQueryWrapper<AitParseResult>().eq(AitParseResult::getMaterialId, m.getMaterialId()));
            if (r != null) {
                parses.add(r);
            }
        }
        String dataClass = parses.stream().map(AitParseResult::getSensitiveType)
                .filter(StringUtils::hasText).findFirst().orElse("电网生产数据");
        String dataGrade = grade(dataClass);
        tool(tools, "智能解析(OCR/NLP)", "qwen-vl/qwen");
        stage(stages, "数据分类分级", "数据分类=" + dataClass + ",数据级别=" + dataGrade, "工具+规则");

        // 阶段② 合规扫描(工具:规则引擎)
        boolean materialsOk = !materials.isEmpty() && parses.size() == materials.size();
        boolean sealedOk = parses.stream().allMatch(r -> "有效".equals(r.getSealValid()));
        boolean rightStd = isStandardRight(apply.getRightType());
        List<String> compIssues = new ArrayList<>();
        if (materials.isEmpty()) {
            compIssues.add("未上传权属证明材料");
        } else if (!materialsOk) {
            compIssues.add("存在未解析材料");
        }
        if (!materials.isEmpty() && !sealedOk) {
            compIssues.add("存在印章缺失/可疑材料");
        }
        if (!rightStd) {
            compIssues.add("权利类型非三权分置标准权利");
        }
        boolean compliancePass = compIssues.isEmpty();
        tool(tools, "规则引擎", "-");
        stage(stages, "合规扫描", compliancePass ? "合规预筛通过" : "合规问题:" + String.join(";", compIssues), "规则");

        // 阶段③ 授权判断(工具:规则引擎)
        String restrictions = restrictions(dataGrade, apply.getRightType());
        stage(stages, "授权判断", "权利类型=" + apply.getRightType() + ";限制条件=" + restrictions
                + ";遵循先确后授", "规则");

        // 阶段④ 风险识别(工具:冲突检测)
        List<AitConflict> conflicts = conflictMapper.selectList(new LambdaQueryWrapper<AitConflict>()
                .eq(AitConflict::getAssetId, assetId).eq(AitConflict::getStatus, AitConflict.STATUS_OPEN));
        long highN = conflicts.stream().filter(c -> "高".equals(c.getRiskLevel())).count();
        long medN = conflicts.stream().filter(c -> "中".equals(c.getRiskLevel())).count();
        String riskLevel = highN > 0 ? "高" : (medN > 0 ? "中" : "低");
        tool(tools, "权属冲突检测", "-");
        stage(stages, "风险识别", "未处置冲突=" + conflicts.size() + "(高" + highN + "/中" + medN + "),风险等级=" + riskLevel, "规则");

        // 通道分流(#2):规则预筛 低风险走快速通道,复杂对象进深度审核
        boolean fast = compliancePass && "低".equals(riskLevel);
        AitAuditResult result = new AitAuditResult();
        result.setApplyId(applyId);
        result.setAssetId(assetId);
        result.setDataClass(dataClass);
        result.setDataGrade(dataGrade);
        result.setRightType(apply.getRightType());
        result.setRestrictions(restrictions);
        result.setRiskLevel(riskLevel);
        result.setAuthLevel(authLevel(dataGrade)); // 3.2#2 授权级别

        // 3.2#4 证据链素材:规则命中项(合规问题+冲突)、KB命中、模型理由
        List<Map<String, Object>> ruleHitsEv = new ArrayList<>();
        for (String iss : compIssues) {
            ruleHitsEv.add(ruleHit("合规规则", iss));
        }
        for (AitConflict c : conflicts) {
            ruleHitsEv.add(ruleHit("冲突规则:" + c.getConflictType(), nz(c.getConflictDesc())));
        }
        List<Map<String, Object>> kbHitsEv = new ArrayList<>();
        String modelReasonEv;

        if (fast) {
            // 快速通道:直接输出结论(不调模型)
            result.setChannel(AitAuditResult.CH_FAST);
            result.setAuthAdvice("建议通过");
            result.setScore(92d);
            result.setSupplement("材料齐备,无需补正");
            result.setCitations("规则预筛:合规通过 + 无未处置冲突");
            result.setReason("规则预筛判定为低风险(合规通过、风险等级低),快速通道直接给出通过结论");
            result.setAction(actionOf("建议通过"));
            modelReasonEv = "快速通道:规则预筛低风险,未调用大模型";
            stage(stages, "结论生成", "快速通道:建议通过", "规则");
        } else {
            // 深度审核:规则预判 + 知识召回 + 模型研判 + 结果校核
            result.setChannel(AitAuditResult.CH_DEEP);
            // 知识召回(工具:知识库检索 / 向量)
            String kbQuery = nz(apply.getAssetName()) + " " + nz(apply.getRightType()) + " " + dataClass + " 确权审核依据";
            AitKbService.RagResult rag;
            try {
                rag = kbService.rag(kbQuery, null);
            } catch (RuntimeException e) {
                rag = null;
            }
            tool(tools, "知识库检索(RAG)", "text-embedding-v3");
            stage(stages, "知识召回", rag == null || rag.citations().isEmpty()
                    ? "未检索到匹配依据" : "命中依据:" + String.join("、", rag.citations()), "知识库");
            if (rag != null) {
                for (AitKbService.SearchHit h : rag.hits()) {
                    Map<String, Object> hit = new LinkedHashMap<>();
                    hit.put("citation", h.citation());
                    hit.put("snippet", h.content());
                    kbHitsEv.add(hit);
                }
            }

            // 模型研判(工具:大模型),复用决策分析(因子+RAG+AI预测一致性)
            AitDecision dec = decisionService.analyze(applyId);
            modelReasonEv = nz(dec.getRagAdvice());
            tool(tools, "大模型研判", "qwen3.7-max");
            stage(stages, "模型研判", "模型预测=" + nz(dec.getAiPrediction()) + ",综合评分=" + dec.getScore(), "模型");

            // 结果校核(规则 vs 模型 一致性 + 知识依据交叉)
            boolean consistent = dec.getAiPrediction() == null || dec.getPrediction().equals(dec.getAiPrediction());
            stage(stages, "结果校核", consistent ? "规则与模型结论一致" : "规则与模型结论不一致,建议人工复核", "校核");

            String advice = dec.getPrediction(); // 建议通过/建议驳回/建议补充材料
            // 风险高但模型建议通过 → 降级为有条件通过(交叉推理)
            if ("建议通过".equals(advice) && "高".equals(riskLevel)) {
                advice = "有条件通过";
            }
            result.setAuthAdvice(advice);
            result.setScore(dec.getScore());
            result.setSupplement(dec.getSupplementMaterials());
            List<String> cites = new ArrayList<>();
            if (rag != null) {
                cites.addAll(rag.citations());
            }
            if (StringUtils.hasText(dec.getRagCitations())) {
                cites.add(dec.getRagCitations());
            }
            result.setCitations(cites.isEmpty() ? "（无命中依据)" : String.join(";", cites));
            result.setReason(dec.getReason() + (consistent ? "" : "(规则与模型不一致,需人工复核)")
                    + (riskLevel.equals("高") ? ";存在高风险冲突" : ""));
            result.setAction(actionOf(advice));
            stage(stages, "结论生成", "深度审核:" + advice + "(评分" + dec.getScore() + ")", "模型+规则");
        }

        result.setStageTraceJson(toJson(stages));
        result.setToolTraceJson(toJson(tools));
        auditMapper.delete(new LambdaQueryWrapper<AitAuditResult>().eq(AitAuditResult::getApplyId, applyId));
        auditMapper.insert(result);

        // 3.2#4 审核证据链档案:材料片段 + KB命中 + 规则命中 + 模型理由 + 结论 + SM3 留痕
        buildEvidence(applyId, assetId, materials, kbHitsEv, ruleHitsEv, modelReasonEv,
                result.getAuthAdvice() + " / " + result.getAction());
        return result;
    }

    /** 3.2#4 证据链档案落档(覆盖式)。 */
    private void buildEvidence(String applyId, String assetId, List<AitMaterial> materials,
                              List<Map<String, Object>> kbHits, List<Map<String, Object>> ruleHits,
                              String modelReason, String conclusion) {
        List<Map<String, Object>> snippets = new ArrayList<>();
        for (AitMaterial m : materials) {
            List<AitDocSegment> segs = segmentMapper.selectList(new LambdaQueryWrapper<AitDocSegment>()
                    .eq(AitDocSegment::getMaterialId, m.getMaterialId())
                    .orderByAsc(AitDocSegment::getSegIndex).last("LIMIT 3"));
            if (segs.isEmpty() && StringUtils.hasText(m.getContent())) {
                Map<String, Object> sp = new LinkedHashMap<>();
                sp.put("file", m.getFileName());
                sp.put("snippet", cap(m.getContent(), 200));
                snippets.add(sp);
            }
            for (AitDocSegment s : segs) {
                Map<String, Object> sp = new LinkedHashMap<>();
                sp.put("file", m.getFileName());
                sp.put("granularity", s.getGranularity());
                sp.put("snippet", cap(s.getContent(), 200));
                snippets.add(sp);
            }
        }
        AitEvidence ev = new AitEvidence();
        ev.setApplyId(applyId);
        ev.setAssetId(assetId);
        ev.setMaterialSnippetsJson(toJson(snippets));
        ev.setKbHitsJson(toJson(kbHits));
        ev.setRuleHitsJson(toJson(ruleHits));
        ev.setModelReason(modelReason);
        ev.setConclusion(conclusion);
        ev.setSm3Hash("0x" + Sm3Util.hashHex(applyId + "|" + toJson(snippets) + "|" + toJson(kbHits)
                + "|" + toJson(ruleHits) + "|" + modelReason + "|" + conclusion));
        evidenceMapper.delete(new LambdaQueryWrapper<AitEvidence>().eq(AitEvidence::getApplyId, applyId));
        evidenceMapper.insert(ev);
    }

    public AitEvidence getEvidence(String applyId) {
        AitEvidence e = evidenceMapper.selectOne(new LambdaQueryWrapper<AitEvidence>()
                .eq(AitEvidence::getApplyId, applyId).orderByDesc(AitEvidence::getCreateTime).last("LIMIT 1"));
        if (e == null) {
            throw new BizException("尚无审核证据链,请先发起 Agent 审核");
        }
        return e;
    }

    /** 3.2#1 批量审核:逐个 applyId 走 Agent,返回结果列表(形成台账)。 */
    @Transactional
    public List<AitAuditResult> batchAudit(List<String> applyIds) {
        List<AitAuditResult> out = new ArrayList<>();
        if (applyIds == null) {
            return out;
        }
        for (String id : applyIds) {
            try {
                out.add(audit(id));
            } catch (RuntimeException ignore) {
                // 单条失败不阻断批量
            }
        }
        return out;
    }

    /** 3.2#1 字段级结论:取该申请各材料的清洗审核底表(字段级)。 */
    public List<AitAuditBase> fieldLevel(String applyId) {
        List<String> matIds = materialMapper.selectList(new LambdaQueryWrapper<AitMaterial>()
                        .eq(AitMaterial::getApplyId, applyId)).stream()
                .map(AitMaterial::getMaterialId).collect(Collectors.toList());
        if (matIds.isEmpty()) {
            return List.of();
        }
        return auditBaseMapper.selectList(new LambdaQueryWrapper<AitAuditBase>()
                .in(AitAuditBase::getMaterialId, matIds).orderByAsc(AitAuditBase::getRowNo));
    }

    /** 3.2#5 审核台账分页(按 资产/业务域(数据分类)/风险等级/通道/授权建议 筛选)。 */
    public PageResult<AitAuditResult> ledgerPage(PageQuery query, String assetId, String dataClass,
                                                 String riskLevel, String channel, String authAdvice) {
        LambdaQueryWrapper<AitAuditResult> w = new LambdaQueryWrapper<AitAuditResult>()
                .eq(StringUtils.hasText(assetId), AitAuditResult::getAssetId, assetId)
                .eq(StringUtils.hasText(dataClass), AitAuditResult::getDataClass, dataClass)
                .eq(StringUtils.hasText(riskLevel), AitAuditResult::getRiskLevel, riskLevel)
                .eq(StringUtils.hasText(channel), AitAuditResult::getChannel, channel)
                .eq(StringUtils.hasText(authAdvice), AitAuditResult::getAuthAdvice, authAdvice)
                .orderByDesc(AitAuditResult::getCreateTime);
        IPage<AitAuditResult> p = auditMapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    /** 3.2#5 汇总统计:按 风险等级/数据级别/授权建议/通道 分组计数。 */
    public Map<String, Object> ledgerStats() {
        List<AitAuditResult> all = auditMapper.selectList(new LambdaQueryWrapper<>());
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("byRisk", group(all, AitAuditResult::getRiskLevel));
        stats.put("byGrade", group(all, AitAuditResult::getDataGrade));
        stats.put("byAdvice", group(all, AitAuditResult::getAuthAdvice));
        stats.put("byChannel", group(all, AitAuditResult::getChannel));
        stats.put("byDataClass", group(all, AitAuditResult::getDataClass));
        return stats;
    }

    private Map<String, Long> group(List<AitAuditResult> all, java.util.function.Function<AitAuditResult, String> f) {
        return all.stream().map(f).map(v -> v == null ? "未知" : v)
                .collect(Collectors.groupingBy(v -> v, LinkedHashMap::new, Collectors.counting()));
    }

    /** 3.2#5 审核台账导出 Excel。 */
    public byte[] exportLedgerExcel(String assetId, String dataClass, String riskLevel,
                                    String channel, String authAdvice) {
        PageQuery q = new PageQuery();
        q.setSize(10000);
        List<AitAuditResult> list = ledgerPage(q, assetId, dataClass, riskLevel, channel, authAdvice).getRecords();
        try (org.apache.poi.ss.usermodel.Workbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet s = wb.createSheet("审核台账");
            String[] heads = {"申请ID", "资产ID", "通道", "数据分类", "数据级别", "权利类型",
                    "授权建议", "授权级别", "风险等级", "限制条件", "补正建议", "命中依据", "建议动作", "评分"};
            org.apache.poi.ss.usermodel.Row h = s.createRow(0);
            for (int i = 0; i < heads.length; i++) {
                h.createCell(i).setCellValue(heads[i]);
                s.setColumnWidth(i, 5000);
            }
            int ri = 1;
            for (AitAuditResult r : list) {
                org.apache.poi.ss.usermodel.Row row = s.createRow(ri++);
                String[] v = {nz(r.getApplyId()), nz(r.getAssetId()), nz(r.getChannel()), nz(r.getDataClass()),
                        nz(r.getDataGrade()), nz(r.getRightType()), nz(r.getAuthAdvice()), nz(r.getAuthLevel()),
                        nz(r.getRiskLevel()), nz(r.getRestrictions()), nz(r.getSupplement()), nz(r.getCitations()),
                        nz(r.getAction()), r.getScore() == null ? "" : r.getScore().toString()};
                for (int i = 0; i < v.length; i++) {
                    row.createCell(i).setCellValue(v[i]);
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new BizException("导出审核台账失败:" + ex.getMessage());
        }
    }

    /** 3.2#3 审核报告(Word)。 */
    public byte[] exportReportWord(String applyId) {
        AitAuditResult r = getByApply(applyId);
        return word("智能确权审核报告", List.of(
                "申请ID:" + nz(r.getApplyId()), "资产ID:" + nz(r.getAssetId()), "审核通道:" + nz(r.getChannel()),
                "数据分类:" + nz(r.getDataClass()) + "  数据级别:" + nz(r.getDataGrade()),
                "权利类型:" + nz(r.getRightType()), "授权建议:" + nz(r.getAuthAdvice()) + "  授权级别:" + nz(r.getAuthLevel()),
                "风险等级:" + nz(r.getRiskLevel()), "限制条件:" + nz(r.getRestrictions()),
                "整改/补正建议:" + nz(r.getSupplement()), "法律依据(命中):" + nz(r.getCitations()),
                "结论理由:" + nz(r.getReason()), "建议动作:" + nz(r.getAction()), "综合评分:" + r.getScore()));
    }

    /** 3.2#3 确权登记辅助材料(Word)。 */
    public byte[] registrationDoc(String applyId) {
        AitAuditResult r = getByApply(applyId);
        ConfirmApply a = applyMapper.selectById(applyId);
        return word("数据确权登记辅助材料", List.of(
                "资产名称:" + (a == null ? "" : nz(a.getAssetName())) + "(" + nz(r.getAssetId()) + ")",
                "权利主体:" + (a == null ? "" : nz(a.getRightHolder())), "权利类型:" + nz(r.getRightType()),
                "数据分类:" + nz(r.getDataClass()) + "  数据级别:" + nz(r.getDataGrade()),
                "确权结论:" + nz(r.getAuthAdvice()), "授权级别:" + nz(r.getAuthLevel()),
                "使用限制条件:" + nz(r.getRestrictions()),
                "备注:本材料由智能确权 Agent 辅助生成,供确权登记参考,需人工复核盖章。"));
    }

    /** 3.2#3 法律意见辅助材料(Word)。 */
    public byte[] legalOpinion(String applyId) {
        AitAuditResult r = getByApply(applyId);
        return word("数据确权法律意见辅助材料", List.of(
                "资产ID:" + nz(r.getAssetId()), "权利类型:" + nz(r.getRightType()),
                "数据级别:" + nz(r.getDataGrade()) + "(风险等级:" + nz(r.getRiskLevel()) + ")",
                "适用法律依据:" + nz(r.getCitations()),
                "合规要点:依据上述法条,结合数据级别落实使用限制 ——" + nz(r.getRestrictions()),
                "初步法律意见:" + nz(r.getReason()),
                "建议动作:" + nz(r.getAction()),
                "免责说明:本意见由 AI 辅助生成,仅供法务参考,不构成正式法律意见。"));
    }

    private byte[] word(String title, List<String> lines) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFParagraph tp = doc.createParagraph();
            tp.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            XWPFRun tr = tp.createRun();
            tr.setBold(true);
            tr.setFontSize(16);
            tr.setText(title);
            for (String line : lines) {
                XWPFRun run = doc.createParagraph().createRun();
                run.setFontSize(11);
                run.setText(line);
            }
            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new BizException("生成文档失败:" + ex.getMessage());
        }
    }

    private static Map<String, Object> ruleHit(String rule, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rule", rule);
        m.put("detail", detail);
        return m;
    }

    private static String authLevel(String grade) {
        return switch (grade == null ? "" : grade) {
            case "核心" -> "一级(严格授权·逐项审批)";
            case "敏感" -> "二级(受限授权)";
            case "公开" -> "四级(开放授权)";
            default -> "三级(常规授权)";
        };
    }

    private static String cap(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    public AitAuditResult getByApply(String applyId) {
        AitAuditResult r = auditMapper.selectOne(new LambdaQueryWrapper<AitAuditResult>()
                .eq(AitAuditResult::getApplyId, applyId).orderByDesc(AitAuditResult::getCreateTime).last("LIMIT 1"));
        if (r == null) {
            throw new BizException("尚未生成 Agent 审核结果,请先发起审核");
        }
        return r;
    }

    // ---- 辅助 ----

    private static boolean isStandardRight(String rt) {
        return rt != null && (rt.contains("数据持有权") || rt.contains("数据加工使用权") || rt.contains("数据产品经营权"));
    }

    /** 数据级别:由数据分类映射(核心/敏感/内部/公开)。 */
    private static String grade(String dataClass) {
        if (dataClass == null) {
            return "内部";
        }
        if (dataClass.contains("敏感个人信息") || dataClass.contains("商业秘密")) {
            return "核心";
        }
        if (dataClass.contains("个人信息") || dataClass.contains("监管")) {
            return "敏感";
        }
        if (dataClass.contains("公开")) {
            return "公开";
        }
        return "内部";
    }

    /** 限制条件:由数据级别 + 权利类型推导。 */
    private static String restrictions(String grade, String rightType) {
        List<String> r = new ArrayList<>();
        switch (grade) {
            case "核心" -> {
                r.add("仅限内部使用");
                r.add("对外提供须脱敏并经合规审批");
                r.add("禁止未授权共享");
            }
            case "敏感" -> {
                r.add("最小必要原则");
                r.add("使用前脱敏/去标识");
            }
            default -> r.add("按授权范围与约定用途使用");
        }
        if (rightType != null && rightType.contains("经营")) {
            r.add("对外经营须在开放目录边界内");
        }
        return String.join(";", r);
    }

    private static String actionOf(String advice) {
        if (advice == null) {
            return "人工复核";
        }
        if (advice.contains("通过") && !advice.contains("有条件")) {
            return "进入权益卡片制卡/确权登记";
        }
        if (advice.contains("有条件")) {
            return "附限制条件确权,处置高风险后复核";
        }
        if (advice.contains("驳回")) {
            return "驳回并通知申请人";
        }
        if (advice.contains("补充")) {
            return "退回补正材料后重审";
        }
        return "人工复核";
    }

    private static void stage(List<Map<String, Object>> stages, String stage, String summary, String by) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stage", stage);
        m.put("summary", summary);
        m.put("by", by);
        stages.add(m);
    }

    private static void tool(List<Map<String, Object>> tools, String tool, String model) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tool", tool);
        m.put("model", model);
        tools.add(m);
    }

    private static String toJson(Object o) {
        try {
            return OM.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
