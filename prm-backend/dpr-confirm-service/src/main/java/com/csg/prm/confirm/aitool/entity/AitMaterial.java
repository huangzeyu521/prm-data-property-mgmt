package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-上传材料(SW-001/004 多源资料接入)。
 * 支持 PDF/Word/图片/扫描件;文档哈希+批次编号去重与唯一标识。对应物理表 IM_AIT_MATERIAL。
 */
@TableName("IM_AIT_MATERIAL")
public class AitMaterial extends BaseEntity {

    public static final String PARSE_PENDING = "待解析";
    public static final String PARSE_RUNNING = "解析中";
    public static final String PARSE_SUCCESS = "成功";
    public static final String PARSE_FAILED = "失败";

    @TableId(value = "CEC_MATERIAL_ID", type = IdType.ASSIGN_UUID)
    private String materialId;

    /** 批次编号(批量上传同批次共用) */
    @TableField("CEC_BATCH_NO")
    private String batchNo;

    /** 关联确权申请(用于解析后与表单比对) */
    @TableField("CEC_APPLY_ID")
    private String applyId;

    @TableField("CEC_FILE_NAME")
    private String fileName;

    /** 文件类型:PDF/WORD/JPG/PNG/SCAN */
    @TableField("CEC_FILE_TYPE")
    private String fileType;

    /** 文件 SM3 哈希(去重/防篡改) */
    @TableField("CEC_FILE_HASH")
    private String fileHash;

    @TableField("CEC_SIZE_KB")
    private Long sizeKb;

    /** 解析状态:待解析/解析中/成功/失败 */
    @TableField("CEC_PARSE_STATUS")
    private String parseStatus;

    @TableField("CEC_FAIL_REASON")
    private String failReason;

    /** 解析进度 0–100(供前端实时进度条轮询) */
    @TableField("CEC_PROGRESS")
    private Integer progress;

    /** 文件存储相对路径/键(真实上传后由存储网关写入) */
    @TableField("CEC_STORAGE_PATH")
    private String storagePath;

    /** 解析正文(真实上传:由 PDF/Word 抽取;本地桩:模拟正文) */
    @TableField("CEC_CONTENT")
    private String content;

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public Long getSizeKb() {
        return sizeKb;
    }

    public void setSizeKb(Long sizeKb) {
        this.sizeKb = sizeKb;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
