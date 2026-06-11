package com.csg.prm.confirm.aitool.gateway;

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
        return new ParsedElements(subject, object, rightType, term, scope, dataSource, sensitive,
                sealValid, sealDesc, 0.92);
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
