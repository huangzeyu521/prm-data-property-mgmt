package com.csg.prm.confirm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 确权汇总表(附录C 表3《数据确权信息汇总表》/ 表4《数据权益内部管理汇总表》)。
 * 在节点50(合规管控小组审核)由系统自动生成。对应物理表 IM_CONFIRM_SUMMARY。
 */
@TableName("IM_CONFIRM_SUMMARY")
public class ConfirmSummary extends BaseEntity {

    public static final String TYPE_T3 = "表3-数据确权信息汇总表";
    public static final String TYPE_T4 = "表4-数据权益内部管理汇总表";

    @TableId(value = "CEC_SUMMARY_ID", type = IdType.ASSIGN_UUID)
    private String summaryId;

    @TableField("CEC_APPLY_ID")
    private String applyId;

    /** 汇总表类型:表3 / 表4 */
    @TableField("CEC_SUMMARY_TYPE")
    private String summaryType;

    /** 汇总内容(结构化/文本) */
    @TableField("CEC_CONTENT")
    private String content;

    @TableField("CEC_GENERATOR_ID")
    private String generatorId;

    @TableField("CEC_GENERATE_TIME")
    private LocalDateTime generateTime;

    public String getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(String summaryId) {
        this.summaryId = summaryId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getGeneratorId() {
        return generatorId;
    }

    public void setGeneratorId(String generatorId) {
        this.generatorId = generatorId;
    }

    public LocalDateTime getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(LocalDateTime generateTime) {
        this.generateTime = generateTime;
    }
}
