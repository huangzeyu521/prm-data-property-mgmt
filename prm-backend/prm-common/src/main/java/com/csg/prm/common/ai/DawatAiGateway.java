package com.csg.prm.common.ai;

import java.util.List;

/**
 * 大瓦特 AI 平台网关(数据产权管理 AI 赋能)。
 * 四项能力:OCR 权属识别 / 权属冲突检测 / 授权意图识别 / RAG 知识问答。
 * 生产环境由 Feign 调用大瓦特 AI 平台 + @Primary 覆盖;本地/测试用 {@link LocalDawatAiGateway} 确定性桩。
 */
public interface DawatAiGateway {

    /** OCR 权属识别结果(从权属证明材料抽取结构化权属要素) */
    record OcrOwnership(String assetName, String rightHolder, String rightType,
                        String respDept, double confidence, String rawText) {
    }

    /** 权属冲突检测结果 */
    record ConflictResult(boolean hasConflict, String riskLevel,
                          List<String> conflicts, String suggestion) {
    }

    /** 授权意图识别结果(从自然语言申请抽取授权要素) */
    record AuthIntent(String granteeOrg, String rightType, String scenario, String scope,
                      String mode, String suggestion, double confidence) {
    }

    /** RAG 知识问答结果(基于确权授权指导书/附录F 知识库) */
    record RagAnswer(String answer, List<String> citations, double confidence) {
    }

    /** OCR 权属识别:从权属证明材料(文件URL)抽取权属要素 */
    OcrOwnership recognizeOwnership(String fileUrl);

    /** 权属冲突检测:对资产的权属主张进行冲突/重复确权排查 */
    ConflictResult detectConflict(String assetId, String rightHolder, String rightType);

    /** 授权意图识别:从自然语言授权申请抽取被授权方/权益/场景/范围/模式 */
    AuthIntent recognizeAuthIntent(String text);

    /** RAG 知识问答:基于确权授权业务指导书与附录F 回答 */
    RagAnswer ask(String question);

    /**
     * 授权申请材料 AI 校验:输入 申请要素+逐份材料正文(段格式 【材料】名称=..;正文=..),
     * 输出严格 JSON {"overall":"通过|不通过|存疑","overallDesc":"…","items":[{materialName,verdict,issues,suggestion}]}。
     * 默认 null(调用方报"暂不可用");Qwen 用 qwen3-max 真实校验,Local 用确定性规则桩。
     */
    default String reviewAuthMaterials(String context) {
        return null;
    }

    /** 授权合规 AI 预审:输入 规则校验结果+申请上下文,输出预审意见文本(规则门禁的补充意见,非门禁) */
    default String preReviewAuth(String context) {
        return null;
    }

    /**
     * 批量授权意图解析:自然语言→严格 JSON
     * {"granteeOrg":"…","rightType":"…","scenario":"…","items":[{"assetName":"…"}]}(一段话解析出多条明细)。
     */
    default String parseBatchIntent(String text) {
        return null;
    }
}
