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
}
