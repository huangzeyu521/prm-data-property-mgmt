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
}
