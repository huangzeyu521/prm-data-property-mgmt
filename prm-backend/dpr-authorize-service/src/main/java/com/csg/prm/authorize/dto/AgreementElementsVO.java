package com.csg.prm.authorize.dto;

import com.csg.prm.authorize.entity.AuthAgreement;
import com.csg.prm.authorize.entity.AuthApply;

import java.io.Serializable;

/**
 * 协议要素核对视图(附录D §3.4.4):协议 + join 其来源授权申请单(applyId)的协议要素,
 * 供「协议工作台 / ②协议审核」核对"协议内容与申请单一致"(防篡改、防阴阳合同)。
 *
 * §3.4.4 五要素:数据范围(scope)/使用场景及目的(scenario)/利益分配(benefitAllocation)/安全保障(securityReq);
 * 另带 表5/表6 上下文(系统/数据表/模式/权益/被授权方/涉第三方/涉隐私/跨域)。
 */
public record AgreementElementsVO(
        String agreementId,
        String agreementNo,
        String applyId,
        String authMode,
        String granteeOrg,
        String sysName,        // 所属系统(assetId 去 SYS: 前缀)
        String dataTable,      // 数据表(库表名 = assetName)
        String schemaName,     // 模式名称(表5/表6)
        String rightType,      // 权益类型
        String businessDomain, // 业务域
        String scenario,       // 使用场景及目的(§3.4.4)
        String scope,          // 数据范围(§3.4.4)
        String benefitAllocation, // 利益分配约定(§3.4.4)
        String securityReq,    // 安全保障要求(§3.4.4)
        String thirdPartySource, // 涉第三方来源
        String sensitiveType,  // 涉个人隐私/商业秘密
        Boolean crossRegion,   // 是否跨域
        String validDate       // 授权时效
) implements Serializable {

    private static String sysOf(String assetId) {
        if (assetId == null) {
            return "";
        }
        return assetId.startsWith("SYS:") ? assetId.substring(4) : assetId;
    }

    /** 协议 + 申请单(可能为空,容错)→ 核对视图。 */
    public static AgreementElementsVO of(AuthAgreement ag, AuthApply apply) {
        String validDate = apply != null && apply.getValidDate() != null
                ? apply.getValidDate().toString().substring(0, 10) : null;
        return new AgreementElementsVO(
                ag.getAgreementId(), ag.getAgreementNo(), ag.getApplyId(),
                apply != null ? apply.getAuthMode() : null,
                ag.getGranteeOrg(),
                apply != null ? sysOf(apply.getAssetId()) : null,
                apply != null ? apply.getAssetName() : null,
                apply != null ? apply.getSchemaName() : null,
                apply != null ? apply.getRightType() : null,
                apply != null ? apply.getBusinessDomain() : null,
                apply != null ? apply.getScenario() : null,
                apply != null ? apply.getScope() : null,
                apply != null ? apply.getBenefitAllocation() : null,
                apply != null ? apply.getSecurityReq() : null,
                apply != null ? apply.getThirdPartySource() : null,
                apply != null ? apply.getSensitiveType() : null,
                apply != null ? apply.getCrossRegion() : null,
                validDate);
    }
}
