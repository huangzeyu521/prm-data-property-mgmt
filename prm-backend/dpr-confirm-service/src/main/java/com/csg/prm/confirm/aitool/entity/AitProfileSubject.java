package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-关键主体(#2):来源主体/授权主体/使用主体/加工主体/共享对象。对应 IM_AIT_PROFILE_SUBJECT。
 */
@TableName("IM_AIT_PROFILE_SUBJECT")
public class AitProfileSubject extends BaseEntity {

    public static final String R_SOURCE = "来源主体";
    public static final String R_AUTHORIZER = "授权主体";
    public static final String R_USER = "使用主体";
    public static final String R_PROCESSOR = "加工主体";
    public static final String R_SHARED = "共享对象";

    @TableId(value = "CEC_SUBJECT_ID", type = IdType.ASSIGN_UUID)
    private String subjectId;

    @TableField("CEC_PROFILE_ID")
    private String profileId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    @TableField("CEC_SUBJECT_ROLE")
    private String subjectRole;

    @TableField("CEC_SUBJECT_NAME")
    private String subjectName;

    @TableField("CEC_METHOD")
    private String method;

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
    public String getProfileId() { return profileId; }
    public void setProfileId(String profileId) { this.profileId = profileId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getSubjectRole() { return subjectRole; }
    public void setSubjectRole(String subjectRole) { this.subjectRole = subjectRole; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}
