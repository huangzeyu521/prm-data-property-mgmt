package com.csg.prm.confirm.integration.dto;

import com.csg.prm.confirm.entity.ConfirmApply;

import java.time.LocalDateTime;

/**
 * 数据资产卡片「产权信息」子Tab 只读契约(语义字段,源:确权登记 ConfirmApply)。
 * 对齐平台产权元数据表 AU_TABLE_META_DATA 的 G/H/I/J 四类权益约束维度(是否+说明);
 * 四维"是否"由 ConfirmApply.relationIdentification(含 G/H/I/J 标记)+ regulated/涉第三方派生。
 */
public record AssetPropertyVO(
        String assetId,
        String assetName,
        String state,                       // 待确权 / 确权中 / 已确权 / 已驳回
        String registerType,                // 初始确权 / 确权变更
        String rightType,                   // 持有权 / 使用权 / 经营权
        String rightHolder,                 // 权利主体
        String respDept,                    // 数据责任部门
        String sourceMethod,                // 来源方式 A自行生产…F其他
        String sourceSubject,               // 来源主体
        String sourceLimit,                 // 来源说明 / 约束
        Boolean involvesRegulation,         // G 是否涉及行政监管要求
        String regulated,                   // G 说明(监管机构/主体)
        Boolean involvesPrivacy,            // H 是否涉及用户个人/家庭隐私
        Boolean involvesTradeSecret,        // I 是否涉及第三方商业机密
        String thirdPartyInfo,              // I 说明
        Boolean involvesThirdPartyAgreement,// J 是否存在其他数据权益约束协议
        String relationSubject,             // J 说明(关联主体)
        String recognitionOpinion,          // 认定意见 / 确权结论
        String equityRisk,                  // 权益风险
        LocalDateTime validDate,            // 有效期
        LocalDateTime confirmTime,          // 确权时间(终审通过时点)
        String evidenceRef,                 // 确权凭证(区块链存证号 / sourceRef)
        String applyNo,                     // 关联确权登记号
        String message                      // 边界态提示(未确权/确权中/驳回原因)
) {
    /** 由一条确权登记装配;G–J 四维由 relationIdentification 标记 + regulated/涉第三方派生。 */
    public static AssetPropertyVO of(ConfirmApply a, String state, String message) {
        String rel = a.getRelationIdentification() == null ? "" : a.getRelationIdentification().toUpperCase();
        boolean g = rel.contains("G") || hasText(a.getRegulated());
        boolean h = rel.contains("H");
        boolean i = rel.contains("I") || Boolean.TRUE.equals(a.getInvolvesThirdParty());
        boolean j = rel.contains("J");
        return new AssetPropertyVO(
                a.getAssetId(), a.getAssetName(), state, a.getRegisterType(),
                a.getRightType(), a.getRightHolder(), a.getRespDept(),
                a.getSourceIdentification(), a.getSourceSubject(), a.getSourceLimit(),
                g, a.getRegulated(), h, i, a.getThirdPartyInfo(), j, a.getRelationSubject(),
                a.getRecognitionOpinion(), a.getEquityRisk(),
                a.getValidDate(), a.getUpdateTime(), a.getSourceRef(), a.getApplyNo(), message);
    }

    /** 无确权记录时的占位契约(卡片不空白)。 */
    public static AssetPropertyVO placeholder(String assetId, String state, String message) {
        return new AssetPropertyVO(assetId, null, state, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, message);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
