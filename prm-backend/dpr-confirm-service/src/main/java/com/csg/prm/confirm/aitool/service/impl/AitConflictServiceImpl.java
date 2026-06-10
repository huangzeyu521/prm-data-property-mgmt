package com.csg.prm.confirm.aitool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.aitool.dto.KgGraphVO;
import com.csg.prm.confirm.aitool.entity.AitConflict;
import com.csg.prm.confirm.aitool.entity.AitKgClaim;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.mapper.AitConflictMapper;
import com.csg.prm.confirm.aitool.mapper.AitKgClaimMapper;
import com.csg.prm.confirm.aitool.service.AitConflictService;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AitConflictServiceImpl implements AitConflictService {

    private final AitKgClaimMapper claimMapper;
    private final AitConflictMapper conflictMapper;
    private final AitMaterialService materialService;
    private final ConfirmApplyMapper applyMapper;
    private final EquityCardMapper cardMapper;

    public AitConflictServiceImpl(AitKgClaimMapper claimMapper, AitConflictMapper conflictMapper,
                                  AitMaterialService materialService, ConfirmApplyMapper applyMapper,
                                  EquityCardMapper cardMapper) {
        this.claimMapper = claimMapper;
        this.conflictMapper = conflictMapper;
        this.materialService = materialService;
        this.applyMapper = applyMapper;
        this.cardMapper = cardMapper;
    }

    @Override
    @Transactional
    public void updateClaim(AitKgClaim claim) {
        if (claim == null || !StringUtils.hasText(claim.getClaimId())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "主张ID不能为空");
        }
        if (claimMapper.selectById(claim.getClaimId()) == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "权属主张不存在");
        }
        claimMapper.updateById(claim);
    }

    @Override
    @Transactional
    public void deleteClaim(String claimId) {
        if (!StringUtils.hasText(claimId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "主张ID不能为空");
        }
        claimMapper.deleteById(claimId);
    }

    @Override
    @Transactional
    public int syncHistoryClaims(String assetId) {
        if (!StringUtils.hasText(assetId)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产ID不能为空");
        }
        List<EquityCard> cards = cardMapper.selectList(
                new LambdaQueryWrapper<EquityCard>().eq(EquityCard::getAssetId, assetId));
        List<AitKgClaim> existing = claimMapper.selectList(
                new LambdaQueryWrapper<AitKgClaim>().eq(AitKgClaim::getAssetId, assetId)
                        .eq(AitKgClaim::getSourceType, AitKgClaim.SRC_HISTORY));
        int added = 0;
        for (EquityCard card : cards) {
            String subject = card.getRightOwner();
            if (!StringUtils.hasText(subject)) {
                continue;
            }
            // 去重:同资产+主体+权利类型 的历史主张已存在则跳过
            boolean dup = existing.stream().anyMatch(c ->
                    subject.equals(c.getSubject()) && java.util.Objects.equals(card.getRightType(), c.getRightType()));
            if (dup) {
                continue;
            }
            AitKgClaim claim = new AitKgClaim();
            claim.setAssetId(assetId);
            claim.setSubject(subject);
            claim.setRightType(card.getRightType());
            claim.setValidDate(card.getValidDate());
            claim.setSourceType(AitKgClaim.SRC_HISTORY);
            claim.setRemark("历史案例自动同步自权益卡片 " + nz(card.getCardNo())
                    + (StringUtils.hasText(card.getRightSource()) ? "(来源:" + card.getRightSource() + ")" : ""));
            claimMapper.insert(claim);
            existing.add(claim);
            added++;
        }
        return added;
    }

    @Override
    @Transactional
    public String addClaim(AitKgClaim claim) {
        if (claim == null || !StringUtils.hasText(claim.getAssetId()) || !StringUtils.hasText(claim.getSubject())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "资产ID与权利主体不能为空");
        }
        if (!StringUtils.hasText(claim.getSourceType())) {
            claim.setSourceType(AitKgClaim.SRC_CURRENT);
        }
        claimMapper.insert(claim);
        return claim.getClaimId();
    }

    @Override
    @Transactional
    public String buildClaimFromMaterial(String materialId) {
        // 条款内容自动化语义分析:复用材料解析要素(由 AI/规则从正文条款抽取)生成权属主张
        AitMaterial m = materialService.getMaterial(materialId);
        AitParseResult r = materialService.getParse(materialId); // 未解析则抛出
        String assetId = null;
        LocalDateTime validDate = null;
        if (StringUtils.hasText(m.getApplyId())) {
            ConfirmApply apply = applyMapper.selectById(m.getApplyId());
            if (apply != null) {
                assetId = apply.getAssetId();
                validDate = apply.getValidDate();
            }
        }
        if (!StringUtils.hasText(assetId)) {
            assetId = StringUtils.hasText(r.getRightObject()) ? r.getRightObject() : m.getFileName();
        }
        AitKgClaim claim = new AitKgClaim();
        claim.setAssetId(assetId);
        claim.setSubject(StringUtils.hasText(r.getRightSubject()) ? r.getRightSubject() : "未识别主体");
        claim.setRightType(r.getRightType());
        claim.setAuthScope(r.getAuthScope());
        claim.setValidDate(validDate);
        claim.setExclusive(r.getRightType() != null && r.getRightType().contains("独占"));
        claim.setSourceType(AitKgClaim.SRC_MATERIAL);
        claim.setRemark("由材料《" + m.getFileName() + "》条款语义分析自动生成(主体/类型/范围/有效期)");
        return addClaim(claim);
    }

    @Override
    public KgGraphVO graph(String assetId) {
        List<AitKgClaim> claims = claimMapper.selectList(
                new LambdaQueryWrapper<AitKgClaim>().eq(AitKgClaim::getAssetId, assetId));
        List<AitConflict> conflicts = conflictMapper.selectList(
                new LambdaQueryWrapper<AitConflict>().eq(AitConflict::getAssetId, assetId));
        KgGraphVO g = new KgGraphVO();
        g.setAssetId(assetId);
        Map<String, KgGraphVO.Node> nodes = new LinkedHashMap<>();
        List<KgGraphVO.Edge> edges = new ArrayList<>();
        // 客体节点
        String objId = "客体#" + assetId;
        nodes.put(objId, new KgGraphVO.Node(objId, "客体", assetId));
        List<String> subjectIds = new ArrayList<>();
        for (AitKgClaim c : claims) {
            String subjId = "主体#" + c.getSubject();
            if (!nodes.containsKey(subjId)) {
                nodes.put(subjId, new KgGraphVO.Node(subjId, "主体", c.getSubject() + "(" + nz(c.getSourceType()) + ")"));
                subjectIds.add(subjId);
            }
            String matterId = "授权事项#" + c.getClaimId();
            String matterLabel = nz(c.getRightType()) + (StringUtils.hasText(c.getAuthScope()) ? "/" + c.getAuthScope() : "")
                    + (Boolean.TRUE.equals(c.getExclusive()) ? "(排他)" : "");
            nodes.put(matterId, new KgGraphVO.Node(matterId, "授权事项", matterLabel));
            edges.add(new KgGraphVO.Edge(subjId, matterId, "授权", "主张"));
            edges.add(new KgGraphVO.Edge(matterId, objId, "归属", nz(c.getRightType())));
            if (c.getValidDate() != null) {
                String vId = "有效期#" + c.getClaimId();
                nodes.put(vId, new KgGraphVO.Node(vId, "有效期", c.getValidDate().toLocalDate().toString()));
                edges.add(new KgGraphVO.Edge(matterId, vId, "有效期", "至"));
            }
        }
        // 冲突关系:涉及的不同主体之间标"冲突"
        if (!conflicts.isEmpty() && subjectIds.size() >= 2) {
            String ctype = nz(conflicts.get(0).getConflictType());
            for (int i = 0; i < subjectIds.size(); i++) {
                for (int j = i + 1; j < subjectIds.size(); j++) {
                    edges.add(new KgGraphVO.Edge(subjectIds.get(i), subjectIds.get(j), "冲突", ctype));
                }
            }
        }
        g.setNodes(new ArrayList<>(nodes.values()));
        g.setEdges(edges);
        g.setConflicts(conflicts);
        return g;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    @Override
    @Transactional
    public List<AitConflict> detect(AitKgClaim cur) {
        List<AitKgClaim> existing = claimMapper.selectList(new LambdaQueryWrapper<AitKgClaim>()
                .eq(AitKgClaim::getAssetId, cur.getAssetId())
                .ne(cur.getClaimId() != null, AitKgClaim::getClaimId, cur.getClaimId()));
        List<AitConflict> found = new ArrayList<>();
        for (AitKgClaim ex : existing) {
            if (AitKgClaim.SRC_CURRENT.equals(ex.getSourceType())) {
                continue;
            }
            boolean sameSubject = eq(cur.getSubject(), ex.getSubject());
            boolean sameRight = rightEq(cur.getRightType(), ex.getRightType());

            // 主体冲突:同一客体被多主体声明同类权属。
            // 触发:不同主体 + 同类权利 + 该权利天然单一主体(持有权/所有权)或经营权或排他主张。
            if (!sameSubject && sameRight && (isHolding(cur.getRightType()) || isOperation(cur.getRightType())
                    || Boolean.TRUE.equals(ex.getExclusive()) || Boolean.TRUE.equals(cur.getExclusive()))) {
                found.add(emit(cur, AitConflict.TYPE_SUBJECT, "同一客体被多主体声明同类权属",
                        "客体「" + cur.getAssetId() + "」被多主体声明「" + cur.getRightType() + "」 —— 主体「"
                                + ex.getSubject() + "」(" + claimContent(ex) + ") 与 主体「"
                                + cur.getSubject() + "」(" + claimContent(cur) + ")",
                        "客体:" + cur.getAssetId() + ";冲突主体:" + ex.getSubject() + "、" + cur.getSubject(), "高",
                        "建议补充权属证明,协商划分权利主体后再确权(数据持有权应归属单一主体)"));
            }
            // 范围冲突:与历史排他授权范围覆盖/重叠
            if (!sameSubject && (Boolean.TRUE.equals(ex.getExclusive()) || Boolean.TRUE.equals(cur.getExclusive()))
                    && scopeOverlap(cur.getAuthScope(), ex.getAuthScope())) {
                found.add(emit(cur, AitConflict.TYPE_SCOPE, "历史排他性授权未注销",
                        "新授权范围「" + cur.getAuthScope() + "」与排他授权「" + ex.getAuthScope() + "」重叠",
                        "重叠范围:" + cur.getAuthScope(), "中",
                        "建议修订/补充授权范围,避免与排他授权重叠"));
            }
            // 历史记录冲突:与历史确权矛盾(权利类型不同)或重复(同主体同权利)
            if (AitKgClaim.SRC_HISTORY.equals(ex.getSourceType())) {
                if (!sameRight) {
                    found.add(emit(cur, AitConflict.TYPE_HISTORY, "证明材料与历史确权矛盾",
                            "历史确权为「" + ex.getRightType() + "」,当前申请为「" + cur.getRightType() + "」",
                            "权利类型矛盾", "高", "建议核实历史确权记录,补正申请信息"));
                } else if (sameSubject) {
                    found.add(emit(cur, AitConflict.TYPE_HISTORY, "同一主体重复申请同一权利",
                            "主体「" + cur.getSubject() + "」已有历史确权「" + cur.getRightType() + "」",
                            "重复确权", "中", "建议复核是否需重新确权或走变更流程"));
                }
            }
            // 时效冲突:授权有效期超出历史确权/数据生命周期
            if (cur.getValidDate() != null && ex.getValidDate() != null && cur.getValidDate().isAfter(ex.getValidDate())) {
                found.add(emit(cur, AitConflict.TYPE_VALIDITY, "授权有效期超出数据生命周期",
                        "当前有效期晚于历史确权有效期(" + ex.getValidDate().toLocalDate() + ")",
                        "时效越界", "中", "建议调整授权有效期,不超过数据生命周期"));
            }
        }
        return found;
    }

    private AitConflict emit(AitKgClaim cur, String type, String source, String desc,
                             String impact, String risk, String suggestion) {
        AitConflict c = new AitConflict();
        c.setAssetId(cur.getAssetId());
        c.setConflictType(type);
        c.setConflictSource(source);
        c.setConflictDesc(desc);
        c.setImpactScope(impact);
        c.setRiskLevel(risk);
        c.setSuggestion(suggestion);
        c.setStatus(AitConflict.STATUS_OPEN);
        conflictMapper.insert(c);
        return c;
    }

    @Override
    public List<AitKgClaim> claims(String assetId) {
        return claimMapper.selectList(new LambdaQueryWrapper<AitKgClaim>()
                .eq(AitKgClaim::getAssetId, assetId).orderByDesc(AitKgClaim::getCreateTime));
    }

    @Override
    public List<AitConflict> conflicts(String assetId) {
        return conflicts(assetId, null, null, null, null);
    }

    @Override
    public List<AitConflict> conflicts(String assetId, String conflictType, String riskLevel,
                                       String startTime, String endTime) {
        LambdaQueryWrapper<AitConflict> w = new LambdaQueryWrapper<AitConflict>()
                .eq(StringUtils.hasText(assetId), AitConflict::getAssetId, assetId)
                .eq(StringUtils.hasText(conflictType), AitConflict::getConflictType, conflictType)
                .eq(StringUtils.hasText(riskLevel), AitConflict::getRiskLevel, riskLevel);
        if (StringUtils.hasText(startTime)) {
            w.ge(AitConflict::getCreateTime, LocalDate.parse(startTime).atStartOfDay());
        }
        if (StringUtils.hasText(endTime)) {
            w.le(AitConflict::getCreateTime, LocalDate.parse(endTime).atTime(23, 59, 59));
        }
        w.orderByDesc(AitConflict::getCreateTime);
        return conflictMapper.selectList(w);
    }

    @Override
    @Transactional
    public void resolve(String conflictId, String feedback) {
        AitConflict c = conflictMapper.selectById(conflictId);
        if (c == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "冲突记录不存在");
        }
        AitConflict upd = new AitConflict();
        upd.setConflictId(conflictId);
        upd.setStatus(AitConflict.STATUS_RESOLVED);
        upd.setSuggestion(StringUtils.hasText(feedback) ? feedback : c.getSuggestion());
        conflictMapper.updateById(upd);
    }

    @Override
    public Map<String, Object> report(String assetId) {
        return report(assetId, null, null, null, null);
    }

    @Override
    public Map<String, Object> report(String assetId, String conflictType, String riskLevel,
                                      String startTime, String endTime) {
        List<AitConflict> list = conflicts(assetId, conflictType, riskLevel, startTime, endTime);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("assetId", assetId);
        report.put("total", list.size());
        report.put("byType", list.stream().collect(Collectors.groupingBy(AitConflict::getConflictType, Collectors.counting())));
        report.put("byRisk", list.stream().collect(Collectors.groupingBy(AitConflict::getRiskLevel, Collectors.counting())));
        report.put("details", list);
        report.put("conclusion", list.isEmpty() ? "未发现权属冲突,可继续确权"
                : "发现 " + list.size() + " 项权属冲突,建议处置后再确权");
        return report;
    }

    @Override
    public byte[] exportReportWord(String assetId, String conflictType, String riskLevel,
                                   String startTime, String endTime) {
        List<AitConflict> list = conflicts(assetId, conflictType, riskLevel, startTime, endTime);
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
            XWPFRun tr = title.createRun();
            tr.setBold(true);
            tr.setFontSize(16);
            tr.setText("权属冲突分析报告");
            addLine(doc, "数据资产:" + (StringUtils.hasText(assetId) ? assetId : "全部"));
            addLine(doc, "筛选条件:冲突类型=" + orAll(conflictType) + " 风险等级=" + orAll(riskLevel)
                    + " 时间=" + orAll(startTime) + "~" + orAll(endTime));
            addLine(doc, "冲突总数:" + list.size());
            addLine(doc, "结论:" + (list.isEmpty() ? "未发现权属冲突,可继续确权"
                    : "发现 " + list.size() + " 项权属冲突,建议处置后再确权"));

            XWPFTable table = doc.createTable(1, 6);
            String[] heads = {"冲突类型", "冲突来源", "冲突描述", "影响范围", "风险等级", "处置建议"};
            XWPFTableRow h = table.getRow(0);
            for (int i = 0; i < heads.length; i++) {
                setCell(h.getCell(i), heads[i], true);
            }
            for (AitConflict c : list) {
                XWPFTableRow row = table.createRow();
                setCell(row.getCell(0), c.getConflictType(), false);
                setCell(row.getCell(1), c.getConflictSource(), false);
                setCell(row.getCell(2), c.getConflictDesc(), false);
                setCell(row.getCell(3), c.getImpactScope(), false);
                setCell(row.getCell(4), c.getRiskLevel(), false);
                setCell(row.getCell(5), c.getSuggestion(), false);
            }
            doc.write(bos);
            return bos.toByteArray();
        } catch (Exception ex) {
            throw new BizException("导出冲突报告失败:" + ex.getMessage());
        }
    }

    private String orAll(String s) {
        return StringUtils.hasText(s) ? s : "全部";
    }

    private void addLine(XWPFDocument doc, String text) {
        XWPFRun run = doc.createParagraph().createRun();
        run.setFontSize(11);
        run.setText(text);
    }

    private void setCell(org.apache.poi.xwpf.usermodel.XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFRun run = cell.addParagraph().createRun();
        run.setBold(bold);
        run.setFontSize(10);
        run.setText(text == null ? "" : text);
    }

    private boolean eq(String a, String b) {
        return a != null && b != null && (a.contains(b) || b.contains(a));
    }

    private boolean rightEq(String a, String b) {
        return norm(a).equals(norm(b));
    }

    private String norm(String rt) {
        if (rt == null) {
            return "";
        }
        if (rt.contains("经营")) {
            return "经营";
        }
        if (rt.contains("使用") || rt.contains("加工")) {
            return "使用";
        }
        if (rt.contains("持有") || rt.contains("所有")) {
            return "持有";
        }
        return rt;
    }

    private boolean isOperation(String rt) {
        return rt != null && rt.contains("经营");
    }

    /** 天然单一主体的权利:数据持有权/所有权(同一客体不应被多主体声明)。 */
    private boolean isHolding(String rt) {
        return rt != null && (rt.contains("持有") || rt.contains("所有"));
    }

    /** 主张内容摘要:范围/排他/来源,供冲突详情明确各方主张。 */
    private String claimContent(AitKgClaim c) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(c.getAuthScope())) {
            sb.append("范围:").append(c.getAuthScope());
        }
        if (Boolean.TRUE.equals(c.getExclusive())) {
            sb.append(sb.length() > 0 ? "," : "").append("排他");
        }
        if (StringUtils.hasText(c.getSourceType())) {
            sb.append(sb.length() > 0 ? "," : "").append("来源:").append(c.getSourceType());
        }
        return sb.length() > 0 ? sb.toString() : "未注明";
    }

    private boolean scopeOverlap(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.contains("全") || b.contains("全")) {
            return true;
        }
        return eq(a, b);
    }
}
