package com.csg.prm.confirm.aitool.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 智能确权辅助工具-解析要素结果(SW-003 确权要素识别与特征抽取)。
 * 抽取权利主体/客体/类型/期限/授权范围、数据来源、敏感类型、印章真伪等。对应物理表 IM_AIT_PARSE_RESULT。
 */
@TableName("IM_AIT_PARSE_RESULT")
public class AitParseResult extends BaseEntity {

    @TableId(value = "CEC_PARSE_ID", type = IdType.ASSIGN_UUID)
    private String parseId;

    @TableField("CEC_MATERIAL_ID")
    private String materialId;

    /** 权利主体 */
    @TableField("CEC_RIGHT_SUBJECT")
    private String rightSubject;

    /** 权利客体 */
    @TableField("CEC_RIGHT_OBJECT")
    private String rightObject;

    /** 权利类型:所有权/使用权/授权使用权/持有权/经营权 */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    /** 权利期限/有效期 */
    @TableField("CEC_RIGHT_TERM")
    private String rightTerm;

    /** 授权范围 */
    @TableField("CEC_AUTH_SCOPE")
    private String authScope;

    /** 数据来源:自行生产/公开采集/公共数据授权/共同生产/交易采购/其他 */
    @TableField("CEC_DATA_SOURCE")
    private String dataSource;

    /** 敏感类型:个人信息/敏感个人信息/商业秘密/监管数据/电网生产数据/内部运营数据 */
    @TableField("CEC_SENSITIVE_TYPE")
    private String sensitiveType;

    /** 印章真伪:有效/可疑/未检出 */
    @TableField("CEC_SEAL_VALID")
    private String sealValid;

    @TableField("CEC_SEAL_DESC")
    private String sealDesc;

    /** 抽取置信度(0-1) */
    @TableField("CEC_CONFIDENCE")
    private Double confidence;

    /** 复核标记:置信度 ≥ 阈值=自动通过,否则=需人工复核(落"准确率≥95%"为可操作行为) */
    @TableField("CEC_REVIEW_STATUS")
    private String reviewStatus;

    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }

    public String getParseId() {
        return parseId;
    }

    public void setParseId(String parseId) {
        this.parseId = parseId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public String getRightSubject() {
        return rightSubject;
    }

    public void setRightSubject(String rightSubject) {
        this.rightSubject = rightSubject;
    }

    public String getRightObject() {
        return rightObject;
    }

    public void setRightObject(String rightObject) {
        this.rightObject = rightObject;
    }

    public String getRightType() {
        return rightType;
    }

    public void setRightType(String rightType) {
        this.rightType = rightType;
    }

    public String getRightTerm() {
        return rightTerm;
    }

    public void setRightTerm(String rightTerm) {
        this.rightTerm = rightTerm;
    }

    public String getAuthScope() {
        return authScope;
    }

    public void setAuthScope(String authScope) {
        this.authScope = authScope;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getSensitiveType() {
        return sensitiveType;
    }

    public void setSensitiveType(String sensitiveType) {
        this.sensitiveType = sensitiveType;
    }

    public String getSealValid() {
        return sealValid;
    }

    public void setSealValid(String sealValid) {
        this.sealValid = sealValid;
    }

    public String getSealDesc() {
        return sealDesc;
    }

    public void setSealDesc(String sealDesc) {
        this.sealDesc = sealDesc;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
