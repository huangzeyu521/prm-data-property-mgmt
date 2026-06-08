package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 确权申请材料(对应界面 IM-DAM-DPR-02-001-001-004 材料上传与补充 / -005 材料校验)。
 * 合并材料与校验结果于一表。对应物理表 IM_CONFIRM_MATERIAL。
 */
@TableName("IM_CONFIRM_MATERIAL")
public class ConfirmMaterial extends BaseEntity {

    public static final String CHECK_PENDING = "待校验";
    public static final String CHECK_PASS = "通过";
    public static final String CHECK_FAIL = "不通过";

    @TableId(value = "CEC_MATERIAL_ID", type = IdType.ASSIGN_UUID)
    private String materialId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_MATERIAL_NAME")
    private String materialName;

    /** 资料类型:确权书/授权函/采购协议… */
    @TableField("CEC_MATERIAL_TYPE")
    private String materialType;

    @TableField("CEC_FILE_URL")
    private String fileUrl;

    @TableField("CEC_OWNER")
    private String owner;

    @TableField("CEC_UPLOAD_TIME")
    private LocalDateTime uploadTime;

    /** 校验结果:待校验/通过/不通过 */
    @TableField("CEC_CHECK_RESULT")
    private String checkResult;

    @TableField("CEC_ABNORMAL_DESC")
    private String abnormalDesc;

    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 原件二进制(Base64);列表响应置空,预览/下载时取用 */
    @TableField("CEC_FILE_DATA")
    private String fileData;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }

    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getApplyId() { return applyId; }
    public void setApplyId(String applyId) { this.applyId = applyId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
    public String getCheckResult() { return checkResult; }
    public void setCheckResult(String checkResult) { this.checkResult = checkResult; }
    public String getAbnormalDesc() { return abnormalDesc; }
    public void setAbnormalDesc(String abnormalDesc) { this.abnormalDesc = abnormalDesc; }
}
