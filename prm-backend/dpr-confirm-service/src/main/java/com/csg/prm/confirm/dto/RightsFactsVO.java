package com.csg.prm.confirm.dto;

import java.io.Serializable;

/**
 * 权益事实(供授权侧"确权信息带出"):按资产取最新已完成确权,推导其第三方来源/隐私商密事实。
 * 对齐工作指引 表5「涉及第三方来源方式(确权信息带出)」「涉及个人隐私/商业秘密(确权信息带出)」。
 */
public class RightsFactsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 资产 ID */
    private String assetId;
    /** 是否存在已完成确权(无则下列为缺省) */
    private boolean confirmed;
    /** 涉第三方来源(空=不涉及;非空=来源主体/说明) */
    private String thirdPartySource;
    /** 隐私/商密:个人隐私 / 商业秘密 / 个人隐私、商业秘密 / 无 */
    private String sensitiveType;
    /** 所属业务域(表5/表6,按系统所属业务域带出;系统级确权可解析,资产级由前端目录树系统名解析) */
    private String businessDomain;
    /** 第三方许可凭证/说明(确权侧表2带出,供授权逐表引用·先确后授免重传;空=确权侧未留存,需授权侧补传) */
    private String thirdPartyLicense;
    /** 信息授权协议/隐私说明(确权侧带出,涉个人隐私/商业秘密时逐表引用·免重传;空=确权侧未留存,需授权侧补传) */
    private String infoAuthAgreement;
    /** 数据归属主体(确权 表1「公司主体」/权利主体 rightHolder;供授权侧「是否跨地域」判定:被授权方省 vs 归属主体省) */
    private String ownerOrg;

    public RightsFactsVO() {
    }

    public RightsFactsVO(String assetId) {
        this.assetId = assetId;
        this.thirdPartySource = "";
        this.sensitiveType = "无";
        this.businessDomain = "";
        this.thirdPartyLicense = "";
        this.infoAuthAgreement = "";
        this.ownerOrg = "";
    }

    public String getOwnerOrg() {
        return ownerOrg;
    }

    public void setOwnerOrg(String ownerOrg) {
        this.ownerOrg = ownerOrg;
    }

    public String getThirdPartyLicense() {
        return thirdPartyLicense;
    }

    public void setThirdPartyLicense(String thirdPartyLicense) {
        this.thirdPartyLicense = thirdPartyLicense;
    }

    public String getInfoAuthAgreement() {
        return infoAuthAgreement;
    }

    public void setInfoAuthAgreement(String infoAuthAgreement) {
        this.infoAuthAgreement = infoAuthAgreement;
    }

    public String getBusinessDomain() {
        return businessDomain;
    }

    public void setBusinessDomain(String businessDomain) {
        this.businessDomain = businessDomain;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getThirdPartySource() {
        return thirdPartySource;
    }

    public void setThirdPartySource(String thirdPartySource) {
        this.thirdPartySource = thirdPartySource;
    }

    public String getSensitiveType() {
        return sensitiveType;
    }

    public void setSensitiveType(String sensitiveType) {
        this.sensitiveType = sensitiveType;
    }
}
