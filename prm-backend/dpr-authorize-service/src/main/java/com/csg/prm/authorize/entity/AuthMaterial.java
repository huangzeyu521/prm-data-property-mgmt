package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 授权申请材料(可研 3.2.2.1.1.3.1.4:上传相关材料 + 申请材料信息增删改查)。
 * 文件 Base64 入库(容器无依赖)。对应物理表 IM_AUTH_MATERIAL。
 */
@TableName("IM_AUTH_MATERIAL")
public class AuthMaterial extends BaseEntity {

    @TableId(value = "CEC_MATERIAL_ID", type = IdType.ASSIGN_UUID)
    private String materialId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_MATERIAL_NAME")
    private String materialName;

    @TableField("CEC_MATERIAL_TYPE")
    private String materialType;

    @TableField("CEC_OWNER")
    private String owner;

    @TableField("CEC_UPLOAD_TIME")
    private LocalDateTime uploadTime;

    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 文件二进制 Base64;列表响应置空减负 */
    @TableField("CEC_FILE_DATA")
    private String fileData;

    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
}
