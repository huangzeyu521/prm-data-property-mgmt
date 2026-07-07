package com.csg.prm.authorize.dto;

import com.csg.prm.authorize.entity.AuthAgreement;

import java.io.Serializable;

/**
 * 协议要素落定视图/入参(附录D 协商项)。
 *
 * 协议草稿(附录D)中留白待协商的要素集:有效期止日(表1)/地理范围(表1)/安全要求三行(表2)/
 * 收益分配补充(第六章)/违约金(第九章)/争议方式·送达信息(第十章)/正本份数(第十一章)。
 * 「只填空、不改条款」——可变面收敛到本要素集,正式稿锁定后不可再改(须先退回草案)。
 */
public record AgreementNegotiationVO(
        String agreementId,
        String agreementNo,
        String docStatus,          // 草案/正式稿
        String validUntil,         // 授权有效期止日(yyyy-MM-dd)
        String geoScope,           // 数据使用地理范围
        String securityEncrypt,    // 表2:数据加密
        String securityAccess,     // 表2:访问控制
        String securityAudit,      // 表2:操作审计
        String benefitAllocation,  // 第六章 收益分配补充约定(可选)
        String penaltyAmount,      // 第九章 违约金(万元)
        String disputeMethod,      // 第十章 争议解决方式
        String serviceDelivery,    // 第十章(二) 乙方送达信息
        Integer copiesCount,       // 第十一章 正本份数
        Boolean confidentialityUploaded, // 保密承诺函(附录E)是否已收口(协议级或一事一议申请级)
        Boolean grantorSigned,
        Boolean granteeSigned,
        Boolean terminated
) implements Serializable {

    public static AgreementNegotiationVO of(AuthAgreement a, boolean confidentialityOk) {
        return new AgreementNegotiationVO(
                a.getAgreementId(), a.getAgreementNo(),
                a.getDocStatus() == null ? AuthAgreement.DOC_DRAFT : a.getDocStatus(),
                a.getValidUntil() == null ? null : a.getValidUntil().toLocalDate().toString(),
                a.getGeoScope(), a.getSecurityEncrypt(), a.getSecurityAccess(), a.getSecurityAudit(),
                a.getBenefitAllocation(), a.getPenaltyAmount(), a.getDisputeMethod(),
                a.getServiceDelivery(), a.getCopiesCount(), confidentialityOk,
                a.getGrantorSigned(), a.getGranteeSigned(), a.getTerminated());
    }
}
