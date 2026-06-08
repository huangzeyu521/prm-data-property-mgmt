package com.csg.prm.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体:承载数据中心规范的公共字段(字段前缀 CEC_),供各业务实体继承。
 * provinceCode/bureauCode 同时用于 ABAC 数据范围隔离。
 */
public class BaseEntity implements Serializable {

    @TableField(value = "CEC_PROVINCE_CODE", fill = FieldFill.INSERT)
    private String provinceCode;

    @TableField(value = "CEC_BUREAU_CODE", fill = FieldFill.INSERT)
    private String bureauCode;

    @TableField(value = "CEC_CREATOR_ID", fill = FieldFill.INSERT)
    private String creatorId;

    @TableField(value = "CEC_CREATE_TIME", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "CEC_UPDATER_ID", fill = FieldFill.INSERT_UPDATE)
    private String updaterId;

    @TableField(value = "CEC_UPDATE_TIME", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "CEC_DEL_FLAG", fill = FieldFill.INSERT)
    private Integer delFlag;

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getBureauCode() {
        return bureauCode;
    }

    public void setBureauCode(String bureauCode) {
        this.bureauCode = bureauCode;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdaterId() {
        return updaterId;
    }

    public void setUpdaterId(String updaterId) {
        this.updaterId = updaterId;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }
}
