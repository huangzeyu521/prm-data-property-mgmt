package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 数据权益卡片(对应界面 IM-DAM-DPR-02-001-003-001)。确权终审通过后自动生成。
 * 对应物理表 IM_EQUITY_CARD_INFO。冻结卡片不可用于下游授权(风险熔断)。
 */
@TableName("IM_EQUITY_CARD_INFO")
public class EquityCard extends BaseEntity {

    public static final String STATUS_NORMAL = "正常";
    public static final String STATUS_FROZEN = "冻结";
    public static final String STATUS_INVALID = "失效";

    @TableId(value = "CEC_CARD_ID", type = IdType.ASSIGN_UUID)
    private String cardId;

    /** 全局唯一权益资产编码 */
    @TableField("CEC_CARD_NO")
    private String cardNo;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    @TableField("CEC_ASSET_NAME")
    private String assetName;

    /** 权益类型(三权分置) */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    /** 权益所有者 */
    @TableField("CEC_RIGHT_OWNER")
    private String rightOwner;

    /** 权益来源 */
    @TableField("CEC_RIGHT_SOURCE")
    private String rightSource;

    /** 确权范围(授权边界依据):全字段 / 约定字段(确权列了表级清单时);先确后授时授权范围不得超此边界 */
    @TableField("CEC_SCOPE")
    private String scope;

    @TableField("CEC_VALID_DATE")
    private LocalDateTime validDate;

    /** 权益状态:正常/冻结/失效 */
    @TableField("CEC_CARD_STATUS")
    private String cardStatus;

    /** 权益归口单位(权益归集原则:分省确权通过后统一归口网级/中国南方电网有限责任公司) */
    @TableField("CEC_CONSOLIDATED_UNIT")
    private String consolidatedUnit;

    /** 卡片版本号(确权变更每取代一次 +1,初始确权为 1)。 */
    @TableField("CEC_VERSION")
    private Integer version;

    /** 前序被取代卡片号(确权变更时指向上一版"正常"卡;初始确权为空)——形成版本链。 */
    @TableField("CEC_SUPERSEDED_NO")
    private String supersededCardNo;

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getRightOwner() {
        return rightOwner;
    }

    public void setRightOwner(String rightOwner) {
        this.rightOwner = rightOwner;
    }

    public String getRightSource() {
        return rightSource;
    }

    public void setRightSource(String rightSource) {
        this.rightSource = rightSource;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public LocalDateTime getValidDate() {
        return validDate;
    }

    public void setValidDate(LocalDateTime validDate) {
        this.validDate = validDate;
    }

    public String getCardStatus() {
        return cardStatus;
    }

    public void setCardStatus(String cardStatus) {
        this.cardStatus = cardStatus;
    }

    public String getConsolidatedUnit() {
        return consolidatedUnit;
    }

    public void setConsolidatedUnit(String consolidatedUnit) {
        this.consolidatedUnit = consolidatedUnit;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getSupersededCardNo() {
        return supersededCardNo;
    }

    public void setSupersededCardNo(String supersededCardNo) {
        this.supersededCardNo = supersededCardNo;
    }
}
