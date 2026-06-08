package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 表6《数据批量授权清单》(附录C 表6)。状态:草案 -> 申报稿 -> 批准(领导小组办公室)。
 * 对应物理表 IM_BATCH_AUTH_LIST。
 */
@TableName("IM_BATCH_AUTH_LIST")
public class BatchAuthList extends BaseEntity {

    public static final String STATUS_DRAFT = "草案";
    public static final String STATUS_SUBMITTED = "申报稿";
    public static final String STATUS_APPROVED = "批准";

    @TableId(value = "CEC_BATCH_LIST_ID", type = IdType.ASSIGN_UUID)
    private String batchListId;

    @TableField("CEC_LIST_NO")
    private String listNo;

    /** 授权年度 */
    @TableField("CEC_LIST_YEAR")
    private String listYear;

    /** 状态:草案/申报稿/批准 */
    @TableField("CEC_LIST_STATUS")
    private String listStatus;

    /** 清单条目数(归集的表5 数量) */
    @TableField("CEC_ITEM_COUNT")
    private Integer itemCount;

    @TableField("CEC_REMARK")
    private String remark;

    public String getBatchListId() {
        return batchListId;
    }

    public void setBatchListId(String batchListId) {
        this.batchListId = batchListId;
    }

    public String getListNo() {
        return listNo;
    }

    public void setListNo(String listNo) {
        this.listNo = listNo;
    }

    public String getListYear() {
        return listYear;
    }

    public void setListYear(String listYear) {
        this.listYear = listYear;
    }

    public String getListStatus() {
        return listStatus;
    }

    public void setListStatus(String listStatus) {
        this.listStatus = listStatus;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
