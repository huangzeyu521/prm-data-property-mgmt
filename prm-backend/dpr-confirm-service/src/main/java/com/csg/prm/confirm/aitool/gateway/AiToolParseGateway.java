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

    /** 对材料(文件名+正文)做版面/OCR/NLP 解析,抽取确权要素 */
    ParsedElements parse(String fileName, String content);
}
