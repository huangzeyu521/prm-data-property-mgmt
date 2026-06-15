package com.csg.prm.confirm.aitool.gateway;

/**
 * 智能确权辅助工具-文档解析网关(统一工具适配层:封装 OCR/版面分析/印章识别/NLP 抽取)。
 * 生产环境由 Feign/SDK 编排大瓦特 OCR + qwen3-max NLP + CV 印章模型,@Primary 覆盖;
 * 本地/测试用 {@link LocalAiToolParseGateway} 确定性桩(基于规则抽取,便于联调与契约测试)。
 */
public interface AiToolParseGateway {

    /** 解析抽取的确权要素 */
    record ParsedElements(String rightSubject, String rightObject, String rightType, String rightTerm,
                          String authScope, String dataSource, String sensitiveType,
                          String sealValid, String sealDesc, double confidence) {
    }

    /** 印章区域(#3):type=公章/合同章/骑缝章/其他;location=位置描述;desc=说明 */
    record Seal(String type, String location, String desc) {
    }

    /** OCR + 版面分析结果(#2/#3):正文 + 标题 + 表格(文本化)+ 印章区域 + 页类型 + 分栏数 + 置信度 */
    record OcrLayout(String text, java.util.List<String> titles, java.util.List<String> tables,
                     java.util.List<Seal> seals, String pageType, int columnCount, double confidence) {
    }

    /** 对材料(文件名+正文)做版面/OCR/NLP 解析,抽取确权要素 */
    ParsedElements parse(String fileName, String content);

    /** 正文中"权利主体:/权属主体:"标注的机构名称(截到首个标点)。 */
    java.util.regex.Pattern SUBJECT_LABEL =
            java.util.regex.Pattern.compile("权[利属]主体\\s*[：:]\\s*([^，,。;；、\\n\\r\\t]+)");

    /**
     * 抽取结果保真归一(#1/#7):
     * 1) 正文显式标注"权利主体:X"时以标注为准 —— 真实确权材料均会写明主体,且对模型偶发的中文输出损坏(乱码/孤立代理对)免疫;
     * 2) 无标注但模型主体输出损坏时,退回安全默认值。
     * 仅订正 rightSubject(自由文本机构名,跨实现/模型最易漂移损坏的字段),其余字段不动。
     */
    static ParsedElements normalize(ParsedElements e, String content) {
        if (e == null) {
            return null;
        }
        String subject = e.rightSubject();
        String labeled = labeledSubject(content);
        if (labeled != null) {
            subject = labeled;
        } else if (isGarbled(subject)) {
            subject = "中国南方电网有限责任公司";
        }
        if (subject != null && subject.equals(e.rightSubject())) {
            return e;
        }
        return new ParsedElements(subject, e.rightObject(), e.rightType(), e.rightTerm(),
                e.authScope(), e.dataSource(), e.sensitiveType(), e.sealValid(), e.sealDesc(), e.confidence());
    }

    private static String labeledSubject(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        java.util.regex.Matcher m = SUBJECT_LABEL.matcher(content);
        if (m.find()) {
            String s = m.group(1).trim();
            return s.isEmpty() ? null : s;
        }
        return null;
    }

    /** 字段损坏判定:空、含替换符 �、孤立代理对或控制字符(模型偶发的中文编码损坏)。 */
    private static boolean isGarbled(String s) {
        if (s == null || s.isBlank()) {
            return true;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '�' || Character.isSurrogate(c)) {
                return true;
            }
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        return false;
    }

    /**
     * 对图片/扫描件做 OCR + 版面分析(#2/#3):提取正文/标题/表格、识别印章区域(公章/合同章/骑缝章)与目录页/正文页。
     * 默认返回 null(调用方据此判定 OCR 不可用);Qwen 网关用多模态(qwen-vl)实现,Local 网关用确定性桩。
     * @param imageBytes 图片字节(PNG/JPG);扫描 PDF 由调用方逐页渲染为图片传入
     * @param mime 形如 image/png、image/jpeg
     * @param hint 提示(文件名/页码),便于桩与模型上下文
     */
    default OcrLayout ocrAndLayout(byte[] imageBytes, String mime, String hint) {
        return null;
    }

    /**
     * 材料类别归集(#4):判定材料属于 元数据/制度附件/授权材料/合同材料/来源说明/确权证明/其他。
     * 默认返回 null(调用方回退规则分类);Qwen 网关用大模型判定。
     */
    default String classifyCategory(String fileName, String content) {
        return null;
    }

    /**
     * 枚举值语义归一(#3 规则+模型混合清洗):当规则/术语库无法匹配时,用模型把脏值映射到候选标准值之一。
     * 默认返回 null(调用方保留规则结果);Qwen 网关用大模型在 candidates 中择一,无法判定返回空串。
     */
    default String normalizeEnum(String field, String value, java.util.List<String> candidates) {
        return null;
    }

    /** 确权要素集(1.3):来源方式 + 数据特征(多) + 关键主体(角色→名称) + 约束(类型→内容)。 */
    record ElementSet(String sourceMethod, java.util.List<String> dataFeatures,
                      java.util.Map<String, String> subjects, java.util.Map<String, String> constraints,
                      double confidence) {
    }

    /**
     * 确权要素识别与特征抽取(1.3 #2/#4/#5 的模型语义层):从语料抽取 来源方式/数据特征/五类主体/五类约束。
     * 默认返回 null(调用方仅用规则);Qwen 网关用大模型语义抽取,与规则结果合并(规则优先,模型补缺)。
     */
    default ElementSet extractElements(String corpus) {
        return null;
    }

    /**
     * 文本向量化(2.1#2 知识库向量化 / #3 语义检索):返回稠密向量。
     * 默认返回 null;Qwen 网关调 text-embedding;Local 网关用确定性 hash 向量(离线可测)。
     */
    default float[] embed(String text) {
        return null;
    }

    /**
     * 基于规则+法规上下文,为权属冲突生成针对性解决方案建议(#16)。
     * 默认返回 null(由调用方回退到规则建议);Qwen 网关用大模型生成,Local 网关用规则桩。
     */
    default String adviseResolution(String context) {
        return null;
    }

    /**
     * 确权申请材料 AI 校验:输入 申请要素+逐份材料正文,输出严格 JSON
     * {"overall":"通过|不通过|存疑","overallDesc":"…","items":[{"materialName","verdict","issues","suggestion"}]}。
     * 默认返回 null(调用方回退规则结论);Qwen 网关用 qwen3-max 真实校验,Local 网关用确定性规则桩。
     */
    default String reviewMaterials(String context) {
        return null;
    }
}
