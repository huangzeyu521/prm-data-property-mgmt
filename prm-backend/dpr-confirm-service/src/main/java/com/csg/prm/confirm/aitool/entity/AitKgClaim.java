package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 智能确权辅助工具-权属主张(知识图谱边,M2 / SW-005)。
 * 节点=权利主体/客体;边=某主体对某资产主张的权属(类型/范围/有效期/排他性/来源)。
 * 用于主体/范围/时效/历史冲突检测。对应物理表 IM_AIT_KG_CLAIM。
 */
@TableName("IM_AIT_KG_CLAIM")
public class AitKgClaim extends BaseEntity {

    public static final String SRC_HISTORY = "历史确权";
    public static final String SRC_CURRENT = "当前申请";
    public static final String SRC_MATERIAL = "证明材料";

    @TableId(value = "CEC_CLAIM_ID", type = IdType.ASSIGN_UUID)
    private String claimId;

    @TableField("CEC_ASSET_ID")
    private String assetId;

    /** 权利主体 */
    @TableField("CEC_SUBJECT")
    private String subject;

    /** 权利类型 */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    /** 授权/使用范围 */
    @TableField("CEC_AUTH_SCOPE")
    private String authScope;

    @TableField("CEC_VALID_DATE")
    private LocalDateTime validDate;

    /** 是否排他性主张 */
    @TableField("CEC_EXCLUSIVE")
    private Boolean exclusive;

    /** 来源:历史确权/当前申请/证明材料 */
    @TableField("CEC_SOURCE_TYPE")
    private String sourceType;

    @TableField("CEC_REMARK")
    private String remark;

    public String getClaimId() {
        return claimId;
    }

    public void setClaimId(String claimId) {
        this.claimId = claimId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getAuthScope() {
        return authScope;
    }

    public void setAuthScope(String authScope) {
        this.authScope = authScope;
    }

    public LocalDateTime getValidDate() {
        return validDate;
    }

    public void setValidDate(LocalDateTime validDate) {
        this.validDate = validDate;
    }

    public Boolean getExclusive() {
        return exclusive;
    }

    public void setExclusive(Boolean exclusive) {
        this.exclusive = exclusive;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
