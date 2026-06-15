package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-约束信息(#5):授权范围/使用边界/共享限制/保留期限/脱敏要求。对应 IM_AIT_CONSTRAINT。
 */
@TableName("IM_AIT_CONSTRAINT")
public class AitConstraint extends BaseEntity {

    public static final String T_AUTH_SCOPE = "授权范围";
    public static final String T_USE_BOUNDARY = "使用边界";
    public static final String T_SHARE_LIMIT = "共享限制";
    public static final String T_RETENTION = "保留期限";
    public static final String T_DESENSITIZE = "脱敏要求";

    @TableId(value = "CEC_CONSTRAINT_ID", type = IdType.ASSIGN_UUID)
    private String constraintId;

    @TableField("CEC_PROFILE_ID")
    private String profileId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    @TableField("CEC_CONSTRAINT_TYPE")
    private String constraintType;

    @TableField("CEC_CONSTRAINT_VALUE")
    private String constraintValue;

    @TableField("CEC_METHOD")
    private String method;

    public String getConstraintId() { return constraintId; }
    public void setConstraintId(String constraintId) { this.constraintId = constraintId; }
    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getConstraintType() { return constraintType; }
    public void setConstraintType(String constraintType) { this.constraintType = constraintType; }
    public String getConstraintValue() { return constraintValue; }
    public void setConstraintValue(String constraintValue) { this.constraintValue = constraintValue; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}
