package com.csg.prm.confirm.aitool.term;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 内置南网/电力确权专属术语库(可研 M1-#4)。
 * 对可枚举要素维护「标准术语集」+「别名/非标 → 标准术语」映射,供要素自动匹配与标准建议。
 * 智能确权辅助工具为独立工具,术语库自包含于此,不依赖外部词典服务。
 */
public final class AitTermLibrary {

    public static final String F_RIGHT_TYPE = "权利类型";
    public static final String F_AUTH_SCOPE = "授权范围";
    public static final String F_DATA_SOURCE = "数据来源";
    public static final String F_SENSITIVE = "敏感类型";
    public static final String F_SECRET_LEVEL = "密级";

    /** 字段 → 标准术语集(有序,便于展示)。 */
    private static final Map<String, Set<String>> STANDARD = new LinkedHashMap<>();
    /** 字段 → (别名/非标 → 标准术语)。 */
    private static final Map<String, Map<String, String>> ALIAS = new LinkedHashMap<>();

    static {
        STANDARD.put(F_RIGHT_TYPE, new LinkedHashSet<>(Set.of("数据持有权", "数据加工使用权", "数据产品经营权")));
        ALIAS.put(F_RIGHT_TYPE, ofPairs(
                "持有权", "数据持有权", "数据所有权", "数据持有权", "所有权", "数据持有权",
                "加工使用权", "数据加工使用权", "使用权", "数据加工使用权", "授权使用权", "数据加工使用权",
                "经营权", "数据产品经营权", "产品经营权", "数据产品经营权", "运营权", "数据产品经营权"));

        STANDARD.put(F_AUTH_SCOPE, new LinkedHashSet<>(Set.of("全字段", "约定字段")));
        ALIAS.put(F_AUTH_SCOPE, ofPairs(
                "全网", "全字段", "全部", "全字段", "全部字段", "全字段", "所有字段", "全字段",
                "部分字段", "约定字段", "限定字段", "约定字段", "指定字段", "约定字段", "约定范围", "约定字段"));

        STANDARD.put(F_DATA_SOURCE, new LinkedHashSet<>(Set.of(
                "自行生产", "公开采集", "公共数据授权", "共同生产", "交易采购", "其他")));
        ALIAS.put(F_DATA_SOURCE, ofPairs(
                "自产", "自行生产", "自有", "自行生产", "采购", "交易采购", "交易", "交易采购", "购买", "交易采购",
                "爬取", "公开采集", "网络采集", "公开采集", "公开", "公开采集",
                "政务数据", "公共数据授权", "公共", "公共数据授权", "合作生产", "共同生产", "联合生产", "共同生产"));

        STANDARD.put(F_SENSITIVE, new LinkedHashSet<>(Set.of(
                "个人信息", "敏感个人信息", "商业秘密", "监管数据", "电网生产数据", "内部运营数据")));
        ALIAS.put(F_SENSITIVE, ofPairs(
                "隐私", "个人信息", "个人隐私", "个人信息", "高敏感", "敏感个人信息",
                "商密", "商业秘密", "商业机密", "商业秘密", "机密", "商业秘密",
                "监管", "监管数据", "运营数据", "内部运营数据", "生产数据", "电网生产数据"));

        // 密级标准值域(《数据确权信息汇总表》官方口径)
        STANDARD.put(F_SECRET_LEVEL, new LinkedHashSet<>(Set.of(
                "不涉密", "核心商密", "普通商密", "工作秘密", "敏感信息")));
        ALIAS.put(F_SECRET_LEVEL, ofPairs(
                "无密级", "不涉密", "非密", "不涉密", "公开", "不涉密",
                "核心商业秘密", "核心商密", "普通商业秘密", "普通商密",
                "工作密", "工作秘密", "内部", "工作秘密", "敏感", "敏感信息", "涉敏", "敏感信息"));
    }

    private AitTermLibrary() {
    }

    /** 匹配结果:是否标准术语 + 标准术语建议。 */
    public record Match(boolean standard, String standardTerm) {
    }

    public static boolean isMatchable(String field) {
        return STANDARD.containsKey(field);
    }

    public static Set<String> standardTerms(String field) {
        return STANDARD.getOrDefault(field, Set.of());
    }

    /** 对某字段的抽取值做术语匹配:命中标准→standard;命中别名/模糊→给标准建议;未知→待人工确认。 */
    public static Match match(String field, String value) {
        String v = value == null ? "" : value.trim();
        Set<String> std = STANDARD.getOrDefault(field, Set.of());
        if (std.isEmpty()) {
            return new Match(true, v); // 非枚举字段不参与术语校验
        }
        if (std.contains(v)) {
            return new Match(true, v);
        }
        Map<String, String> alias = ALIAS.getOrDefault(field, Map.of());
        if (alias.containsKey(v)) {
            return new Match(false, alias.get(v));
        }
        // 模糊:抽取值包含某标准术语 或 反之
        for (String s : std) {
            if (!v.isEmpty() && (v.contains(s) || s.contains(v))) {
                return new Match(false, s);
            }
        }
        // 模糊:抽取值包含某别名关键词
        for (Map.Entry<String, String> en : alias.entrySet()) {
            if (!v.isEmpty() && v.contains(en.getKey())) {
                return new Match(false, en.getValue());
            }
        }
        return new Match(false, "(待人工确认)");
    }

    private static Map<String, String> ofPairs(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }
}
