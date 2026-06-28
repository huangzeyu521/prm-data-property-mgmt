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

    public RightsFactsVO() {
    }

    public RightsFactsVO(String assetId) {
        this.assetId = assetId;
        this.thirdPartySource = "";
        this.sensitiveType = "无";
        this.businessDomain = "";
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
