package com.csg.prm.ledger.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 产权变更记录(原则上由确权/授权审批流事件自动生成,形成防篡改证据链)。
 * 对应物理表 IM_PROPERTY_CHANGE_RECORD。
 */
@TableName("IM_PROPERTY_CHANGE_RECORD")
public class PropertyChangeRecord extends BaseEntity {

    @TableId(value = "CEC_CHANGE_ID", type = IdType.ASSIGN_UUID)
    private String changeId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 变更类型:确权/授权/续期/冻结/注销… */
    @TableField("CEC_CHANGE_TYPE")
    private String changeType;

    @TableField("CEC_FIELD_NAME")
    private String fieldName;

    @TableField("CEC_BEFORE_VALUE")
    private String beforeValue;

    @TableField("CEC_AFTER_VALUE")
    private String afterValue;

    @TableField("CEC_CHANGE_REASON")
    private String changeReason;

    /** 来源流程 */
    @TableField("CEC_SOURCE_FLOW")
    private String sourceFlow;

    /** 关联工单号 */
    @TableField("CEC_SOURCE_TICKET")
    private String sourceTicket;

    @TableField("CEC_OPERATOR_ID")
    private String operatorId;

    @TableField("CEC_CHANGE_TIME")
    private LocalDateTime changeTime;

    /** 区块链存证回执(SM3) */
    @TableField("CEC_CHAIN_HASH")
    private String chainHash;

    public String getChangeId() {
        return changeId;
    }

    public void setChangeId(String changeId) {
        this.changeId = changeId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getSourceFlow() {
        return sourceFlow;
    }

    public void setSourceFlow(String sourceFlow) {
        this.sourceFlow = sourceFlow;
    }

    public String getSourceTicket() {
        return sourceTicket;
    }

    public void setSourceTicket(String sourceTicket) {
        this.sourceTicket = sourceTicket;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getChangeTime() {
        return changeTime;
    }

    public void setChangeTime(LocalDateTime changeTime) {
        this.changeTime = changeTime;
    }

    public String getChainHash() {
        return chainHash;
    }

    public void setChainHash(String chainHash) {
        this.chainHash = chainHash;
    }
}
