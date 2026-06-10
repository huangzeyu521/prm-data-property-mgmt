package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-解析结果与确权申请表单比对差异(SW-001 第7项/解析结果自动标注)。
 * 对应物理表 IM_AIT_COMPARE。
 */
@TableName("IM_AIT_COMPARE")
public class AitCompare extends BaseEntity {

    public static final String DIFF_MATCH = "一致";
    public static final String DIFF_MISMATCH = "不一致";
    public static final String DIFF_MISSING = "缺失";
    /** 确权表单本就不含该字段(如授权范围),区别于"缺失"(应有却没填) */
    public static final String DIFF_NA = "表单未含此项";

    @TableId(value = "CEC_COMPARE_ID", type = IdType.ASSIGN_UUID)
    private String compareId;

    @TableField("CEC_PARSE_ID")
    private String parseId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 比对字段:权利主体/权利类型/权利期限/授权范围 */
    @TableField("CEC_FIELD")
    private String field;

    /** 材料解析值 */
    @TableField("CEC_MATERIAL_VALUE")
    private String materialValue;

    /** 表单填写值 */
    @TableField("CEC_FORM_VALUE")
    private String formValue;

    /** 差异类型:一致/不一致/缺失 */
    @TableField("CEC_DIFF_TYPE")
    private String diffType;

    /** 定位锚点:材料值在原始正文中的字符偏移(-1=未在正文定位到,如图片/扫描件)(#6) */
    @TableField("CEC_SOURCE_OFFSET")
    private Integer sourceOffset;

    /** 定位上下文:原始正文中该值附近的片段(供定位预览)(#6) */
    @TableField("CEC_SOURCE_SNIPPET")
    private String sourceSnippet;

    public Integer getSourceOffset() { return sourceOffset; }
    public void setSourceOffset(Integer sourceOffset) { this.sourceOffset = sourceOffset; }
    public String getSourceSnippet() { return sourceSnippet; }
    public void setSourceSnippet(String sourceSnippet) { this.sourceSnippet = sourceSnippet; }

    public String getCompareId() {
        return compareId;
    }

    public void setCompareId(String compareId) {
        this.compareId = compareId;
    }

    public String getParseId() {
        return parseId;
    }

    public void setParseId(String parseId) {
        this.parseId = parseId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMaterialValue() {
        return materialValue;
    }

    public void setMaterialValue(String materialValue) {
        this.materialValue = materialValue;
    }

    public String getFormValue() {
        return formValue;
    }

    public void setFormValue(String formValue) {
        this.formValue = formValue;
    }

    public String getDiffType() {
        return diffType;
    }

    public void setDiffType(String diffType) {
        this.diffType = diffType;
    }
}
