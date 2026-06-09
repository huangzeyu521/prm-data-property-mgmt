package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 授权签章上传记录(可研 3.2.2.1.1.3.3.2):
 * 记录每次签章文件上传行为 + 格式校验 + 签章有效性验证结果。
 * 对应物理表 IM_AUTH_SEAL_UPLOAD_LOG。
 */
@TableName("IM_AUTH_SEAL_UPLOAD_LOG")
public class AuthSealUploadLog extends BaseEntity {

    @TableId(value = "CEC_LOG_ID", type = IdType.ASSIGN_UUID)
    private String logId;

    @TableField("CEC_AGREEMENT_ID")
    private String agreementId;

    /** 上传角色:授权方/被授权方 */
    @TableField("CEC_UPLOADER_ROLE")
    private String uploaderRole;

    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 签章文件 Base64(下载用;列表置空) */
    @TableField("CEC_FILE_DATA")
    private String fileData;

    /** 格式校验是否通过 */
    @TableField("CEC_FORMAT_OK")
    private Boolean formatOk;

    /** 签章有效性是否通过 */
    @TableField("CEC_SEAL_VALID")
    private Boolean sealValid;

    /** 验证结果说明 */
    @TableField("CEC_VERIFY_RESULT")
    private String verifyResult;

    @TableField("CEC_UPLOAD_TIME")
    private LocalDateTime uploadTime;

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }
    public String getAgreementId() { return agreementId; }
    public void setAgreementId(String agreementId) { this.agreementId = agreementId; }
    public String getUploaderRole() { return uploaderRole; }
    public void setUploaderRole(String uploaderRole) { this.uploaderRole = uploaderRole; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }
    public Boolean getFormatOk() { return formatOk; }
    public void setFormatOk(Boolean formatOk) { this.formatOk = formatOk; }
    public Boolean getSealValid() { return sealValid; }
    public void setSealValid(Boolean sealValid) { this.sealValid = sealValid; }
    public String getVerifyResult() { return verifyResult; }
    public void setVerifyResult(String verifyResult) { this.verifyResult = verifyResult; }
    public LocalDateTime getUploadTime() { return uploadTime; }
    public void setUploadTime(LocalDateTime uploadTime) { this.uploadTime = uploadTime; }
}
