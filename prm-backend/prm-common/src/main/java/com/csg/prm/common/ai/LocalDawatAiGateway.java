package com.csg.prm.common.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 大瓦特 AI 网关本地桩:基于规则/关键词的确定性实现,无外部 AI 依赖,便于开发联调与契约测试。
 * 生产环境另以 Feign 实现 + @Primary 覆盖,调用真实大瓦特 AI 平台(OCR/NLP/RAG)。
 */
@Component
public class LocalDawatAiGateway implements DawatAiGateway {

    private static final String HOLD = "数据持有权";
    private static final String USE = "数据加工使用权";
    private static final String OPERATE = "数据产品经营权";

    @Override
    public OcrOwnership recognizeOwnership(String fileUrl) {
        String src = fileUrl == null ? "" : fileUrl;
        String rightType = inferRightType(src);
        String assetName = src.isEmpty() ? "未识别资产" : src.replaceAll(".*/", "").replaceAll("\\.[a-zA-Z]+$", "");
        return new OcrOwnership(
                assetName,
                "中国南方电网有限责任公司",
                rightType,
                "数字化部",
                0.86,
                "[OCR本地桩] 已从 " + src + " 抽取权属要素(生产由大瓦特OCR返回)");
    }

    @Override
    public ConflictResult detectConflict(String assetId, String rightHolder, String rightType) {
        String id = assetId == null ? "" : assetId;
        List<String> conflicts = new ArrayList<>();
        // 约定:资产ID 含 CONFLICT 视为存在冲突(便于契约测试与演示)
        boolean conflict = id.toUpperCase().contains("CONFLICT");
        if (conflict) {
            conflicts.add("资产 " + id + " 已存在另一权属主体的确权主张,权属边界重叠");
            conflicts.add("权属类型 " + rightType + " 与既有登记不一致");
            return new ConflictResult(true, "高", conflicts,
                    "建议提交合规管控小组裁定,补充权属证明材料后重新确权");
        }
        return new ConflictResult(false, "低", conflicts, "未发现权属冲突,可继续确权流程");
    }

    @Override
    public AuthIntent recognizeAuthIntent(String text) {
        String t = text == null ? "" : text;
        String rightType = t.contains("经营") || t.contains("对外") ? OPERATE
                : (t.contains("加工") || t.contains("使用") || t.contains("分析") ? USE : HOLD);
        String mode = t.contains("批量") ? "批量" : "一事一议";
        String scenario = t.contains("征信") ? "电力金融征信"
                : (t.contains("营销") ? "精准营销" : (t.contains("风控") ? "风险防控" : "数据应用"));
        String scope = t.contains("全字段") ? "全字段" : "约定字段";
        String grantee = extractGrantee(t);
        return new AuthIntent(grantee, rightType, scenario, scope, mode,
                "建议按" + mode + "授权流程发起,授权权益为" + rightType
                        + (OPERATE.equals(rightType) ? ";经营权须校验对外开放目录" : ""),
                0.82);
    }

    @Override
    public RagAnswer ask(String question) {
        String q = question == null ? "" : question;
        if (q.contains("确权") && (q.contains("流程") || q.contains("节点"))) {
            return new RagAnswer(
                    "数据确权按附录F 4.1 的八节点流程办理:发起→收集编制(表1/表2)→配合提供→归集审查→合规小组审核(生成表3/表4 及认定意见)→主管审核→经理/高级经理审批→制卡归集。",
                    List.of("附录F 4.1 数据确权流程", "附录C 表1-表4"), 0.9);
        }
        if (q.contains("先确后授")) {
            return new RagAnswer(
                    "先确后授:数据须先完成确权并生成有效权益卡片,授权方可发起;权益卡片冻结/失效将熔断在途授权。",
                    List.of("附录F 3.2 六大原则", "附录F 4.2/4.3 授权流程"), 0.92);
        }
        if (q.contains("三权分置") || q.contains("权属")) {
            return new RagAnswer(
                    "数据三权分置:数据持有权、数据加工使用权、数据产品经营权分置确权;产品经营权对外授权范围仅限对外开放目录。",
                    List.of("附录F 3.1 三权分置", "附录F 3.4.3 经营权范围"), 0.9);
        }
        if (q.contains("授权") && (q.contains("流程") || q.contains("审批"))) {
            return new RagAnswer(
                    "授权分批量与专项一事一议:专项经合规→业务→主管→经理→副总逐级审批;批量经合规→数字化部认定→领导小组办公室批准。",
                    List.of("附录F 4.2 批量授权", "附录F 4.3 专项授权"), 0.88);
        }
        return new RagAnswer(
                "[RAG本地桩] 未命中知识条目,建议细化问题(如确权流程/先确后授/三权分置/授权审批)。生产由大瓦特RAG基于全量指导书检索作答。",
                List.of("附录F 数据确权授权业务指导书"), 0.5);
    }

    private String inferRightType(String s) {
        if (s.contains("经营") || s.contains("operate")) {
            return OPERATE;
        }
        if (s.contains("加工") || s.contains("使用") || s.contains("use")) {
            return USE;
        }
        return HOLD;
    }

    private String extractGrantee(String t) {
        for (String kw : new String[]{"供电局", "公司", "银行", "机构", "中心"}) {
            int idx = t.indexOf(kw);
            if (idx >= 0) {
                int start = Math.max(0, idx - 6);
                return t.substring(start, idx + kw.length()).replaceAll("^[,，。:：\\s]+", "");
            }
        }
        return "待明确被授权方";
    }
}
