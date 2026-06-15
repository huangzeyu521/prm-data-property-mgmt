package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-多粒度解析片段(#5:按页/段/表格单元生成标准化切片)。
 * 统一文档对象 = AitMaterial,其下挂多粒度片段,为后续清洗/审核/追溯提供标准输入。对应 IM_AIT_DOC_SEGMENT。
 */
@TableName("IM_AIT_DOC_SEGMENT")
public class AitDocSegment extends BaseEntity {

    public static final String G_PAGE = "PAGE";
    public static final String G_PARAGRAPH = "PARAGRAPH";
    public static final String G_CELL = "CELL";
    public static final String G_TABLE = "TABLE";
    public static final String G_TITLE = "TITLE";

    @TableId(value = "CEC_SEGMENT_ID", type = IdType.ASSIGN_UUID)
    private String segmentId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    /** 粒度:PAGE/PARAGRAPH/CELL/TABLE/TITLE */
    @TableField("CEC_GRANULARITY")
    private String granularity;

    @TableField("CEC_PAGE_NO")
    private Integer pageNo;

    @TableField("CEC_SEG_INDEX")
    private Integer segIndex;

    @TableField("CEC_SHEET_NAME")
    private String sheetName;

    @TableField("CEC_ROW_NUM")
    private Integer rowIdx;

    @TableField("CEC_COL_NUM")
    private Integer colIdx;

    @TableField("CEC_CONTENT")
    private String content;

    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getGranularity() { return granularity; }
    public void setGranularity(String granularity) { this.granularity = granularity; }
    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }
    public Integer getSegIndex() { return segIndex; }
    public void setSegIndex(Integer segIndex) { this.segIndex = segIndex; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public Integer getRowIdx() { return rowIdx; }
    public void setRowIdx(Integer rowIdx) { this.rowIdx = rowIdx; }
    public Integer getColIdx() { return colIdx; }
    public void setColIdx(Integer colIdx) { this.colIdx = colIdx; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
