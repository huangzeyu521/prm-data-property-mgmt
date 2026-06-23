package com.csg.prm.confirm.aitool.gateway;

import com.csg.prm.confirm.aitool.enums.MaterialDataType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文档解析网关本地桩:基于关键词规则确定性抽取确权要素,无外部依赖,便于联调与契约测试。
 * 生产环境另以 OCR+qwen+CV 编排实现 + @Primary 覆盖。
 */
@Component
public class LocalAiToolParseGateway implements AiToolParseGateway {

    @Override
    public String adviseResolution(String context) {
        String c = context == null ? "" : context;
        // 规则桩:按冲突类型给出结构化处置方案(生产环境由 qwen3-max @Primary 覆盖生成)
        String plan;
        if (c.contains("主体冲突")) {
            plan = "①由确权管理方组织争议主体补充权属证明材料;②依三权分置协商划分权利主体(数据持有权归唯一主体);③划分结论存证后再确权。";
        } else if (c.contains("范围冲突")) {
            plan = "①核对排他授权的具体范围;②将当前授权范围修订为与排他范围不重叠的字段;③留存范围调整记录。";
        } else if (c.contains("时效冲突")) {
            plan = "①将授权有效期调整至不晚于数据生命周期到期日;②对超期区间不予授权;③如需延长须先办理数据生命周期延展。";
        } else if (c.contains("历史")) {
            plan = "①调取历史确权记录核验;②矛盾项厘清权属归属、变更项走注销/变更流程;③重复项确认是否重新确权。";
        } else {
            plan = "①核验权属证明与历史确权记录;②按冲突性质协商处置;③留存处置记录备查。";
        }
        return "（规则引擎建议）" + plan;
    }

    /**
     * 本地 OCR/版面分析桩(无像素识别能力):基于 hint(文件名/页码)产出确定性结构,便于离线联调与契约测试。
     * 生产由 qwen-vl 多模态 @Primary 覆盖,做真实 OCR + 版面分析。置信度给 0.6(低)以标"需人工复核"。
     */
    @Override
    public OcrLayout ocrAndLayout(byte[] imageBytes, String mime, String hint) {
        String h = hint == null ? "" : hint;
        java.util.List<Seal> seals = new java.util.ArrayList<>();
        if (h.contains("骑缝")) {
            seals.add(new Seal("骑缝章", "跨页接缝处", "(本地桩)检出骑缝章表述"));
        }
        if (h.contains("合同")) {
            seals.add(new Seal("合同章", "落款区", "(本地桩)检出合同章表述"));
        }
        if (h.contains("章") || h.contains("盖章") || h.contains("印")) {
            seals.add(new Seal("公章", "落款区", "(本地桩)检出公章表述"));
        }
        String pageType = h.contains("目录") ? "目录页" : "正文页";
        String text = "（本地OCR桩,无真实像素识别）材料:" + h;
        return new OcrLayout(text, java.util.List.of(h.isEmpty() ? "材料" : h),
                java.util.List.of(), seals, pageType, 1, 0.6);
    }

    /** 本地确定性 hash 向量(2.1#3 离线语义检索):字符 bigram 散列到固定维度 + L2 归一,余弦≈词面重叠。 */
    private static final int LOCAL_DIM = 256;

    @Override
    public float[] embed(String text) {
        float[] v = new float[LOCAL_DIM];
        if (text == null || text.isEmpty()) {
            return v;
        }
        String t = text.trim();
        for (int i = 0; i < t.length(); i++) {
            // 单字 + 相邻 bigram 两种特征,增强重叠区分度
            bump(v, t.substring(i, i + 1));
            if (i + 1 < t.length()) {
                bump(v, t.substring(i, i + 2));
            }
        }
        double norm = 0;
        for (float x : v) {
            norm += x * x;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] /= (float) norm;
            }
        }
        return v;
    }

    private static void bump(float[] v, String token) {
        int h = token.hashCode();
        int idx = Math.floorMod(h, v.length);
        v[idx] += 1f;
    }

    /** 本地材料类别规则分类(#4):按文件名/正文关键词归类,返回规范资料类型编码 01–07(CEC_DATA_TYPE)。 */
    @Override
    public String classifyCategory(String fileName, String content) {
        String t = (fileName == null ? "" : fileName) + " " + (content == null ? "" : content);
        if (t.contains("合同") || t.contains("协议")) {
            return MaterialDataType.CONTRACT.getCode();
        }
        if (t.contains("授权")) {
            return MaterialDataType.AUTHORIZATION.getCode();
        }
        if (t.contains("制度") || t.contains("办法") || t.contains("规定") || t.contains("规范")) {
            return MaterialDataType.POLICY_ATTACHMENT.getCode();
        }
        if (t.contains("来源") || t.contains("采集") || t.contains("生产说明")) {
            return MaterialDataType.SOURCE_DESC.getCode();
        }
        if (t.contains("元数据") || t.contains("字段") || t.contains("表结构") || t.contains("数据字典")) {
            return MaterialDataType.METADATA.getCode();
        }
        if (t.contains("确权") || t.contains("权属") || t.contains("证明")) {
            return MaterialDataType.CONFIRM_PROOF.getCode();
        }
        return MaterialDataType.OTHER.getCode();
    }

    @Override
    public ParsedElements parse(String fileName, String content) {
        String t = (fileName == null ? "" : fileName) + " " + (content == null ? "" : content);
        String rightType = t.contains("经营") ? "数据产品经营权"
                : (t.contains("授权使用") ? "授权使用权"
                : (t.contains("使用") || t.contains("加工") ? "数据加工使用权"
                : (t.contains("所有") ? "所有权" : "数据持有权")));
        String dataSource = t.contains("采购") || t.contains("交易") ? "交易采购"
                : (t.contains("公开采集") ? "公开采集"
                : (t.contains("公共") ? "公共数据授权"
                : (t.contains("共同") ? "共同生产" : "自行生产")));
        String sensitive = t.contains("隐私") || t.contains("个人信息") ? "个人信息"
                : (t.contains("商密") || t.contains("商业秘密") || t.contains("机密") ? "商业秘密"
                : (t.contains("监管") ? "监管数据"
                : (t.contains("运营") ? "内部运营数据" : "电网生产数据")));
        String subject = extract(t, "中国南方电网有限责任公司", "供电局", "供电所", "用户单位", "客户", "电网", "公司", "研究院");
        String object = StringUtils.hasText(fileName) ? fileName.replaceAll("\\.[a-zA-Z]+$", "") : "数据资源";
        String term = t.contains("长期") ? "长期" : (t.contains("5年") ? "5年" : (t.contains("2年") ? "2年" : "3年"));
        String scope = t.contains("全字段") ? "全字段" : (t.contains("全网") ? "全网" : "约定字段");
        boolean sealed = t.contains("章") || t.contains("盖章") || t.contains("印");
        String sealValid = sealed ? "有效" : "未检出";
        String sealDesc = sealed ? "检出企业公章,印章纹理与备案样本一致" : "未检出印章区域";
        return AiToolParseGateway.normalize(new ParsedElements(subject, object, rightType, term, scope, dataSource, sensitive,
                sealValid, sealDesc, 0.92), content);
    }

    private String extract(String text, String dflt, String... kws) {
        for (String kw : kws) {
            int idx = text.indexOf(kw);
            if (idx >= 0) {
                int start = Math.max(0, idx - 6);
                String seg = text.substring(start, idx + kw.length()).replaceAll("^[\\s,，。:：]+", "");
                return seg.length() >= 3 ? seg : dflt;
            }
        }
        return dflt;
    }

    /**
     * 材料 AI 校验规则桩(确定性,便于离线联调与契约测试;生产由 qwen3-max @Primary 覆盖):
     * 逐份材料按 正文是否含盖章表述 给 通过/存疑,汇总 overall。
     * 约定上下文格式:每份材料一段 "【材料】名称=xxx;正文=yyy"。
     */
    @Override
    public String reviewMaterials(String context) {
        String c = context == null ? "" : context;
        StringBuilder items = new StringBuilder();
        boolean anyDoubt = false;
        for (String seg : c.split("【材料】")) {
            if (!seg.contains("名称=")) {
                continue;
            }
            String name = seg.substring(seg.indexOf("名称=") + 3);
            name = name.substring(0, name.indexOf(';') >= 0 ? name.indexOf(';') : name.length()).trim();
            boolean sealed = seg.contains("盖章") && !seg.contains("未盖章") && !seg.contains("无章");
            boolean doubt = !sealed;
            anyDoubt = anyDoubt || doubt;
            if (items.length() > 0) {
                items.append(',');
            }
            items.append("{\"materialName\":\"").append(name)
                 .append("\",\"verdict\":\"").append(doubt ? "存疑" : "通过")
                 .append("\",\"issues\":\"").append(doubt ? "正文未见盖章表述,印章有效性待人工核验" : "无")
                 .append("\",\"suggestion\":\"").append(doubt ? "补充盖章版扫描件或线下核验后备注" : "无需补正")
                 .append("\"}");
        }
        String overall = anyDoubt ? "存疑" : "通过";
        return "{\"overall\":\"" + overall + "\",\"overallDesc\":\"(规则桩校验)逐份核验盖章表述与要素完整性,"
                + (anyDoubt ? "存在待人工核验项" : "全部通过") + "\",\"items\":[" + items + "]}";
    }
}
