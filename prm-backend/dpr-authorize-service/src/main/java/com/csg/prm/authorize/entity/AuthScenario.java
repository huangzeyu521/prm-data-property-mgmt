package com.csg.prm.authorize.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.csg.prm.common.entity.BaseEntity;

/**
 * 授权应用场景配置(可研 3.2.2.1.1.3.1.2/3)。
 * 管理授权申请的用途场景(分类/描述)与申请原因模板;申请时可选择或搜索。
 * 对应物理表 IM_AUTH_SCENARIO。
 */
@TableName("IM_AUTH_SCENARIO")
public class AuthScenario extends BaseEntity {

    public static final String STATUS_ACTIVE = "生效中";
    public static final String STATUS_DISABLED = "停用";

    @TableId(value = "CEC_SCENARIO_ID", type = IdType.ASSIGN_UUID)
    private String scenarioId;

    @TableField("CEC_SCENARIO_NAME")
    private String scenarioName;

    /** 场景分类:内部分析/对外服务/联合建模/监管报送... */
    @TableField("CEC_CATEGORY")
    private String category;

    /** 适用授权权益类型:数据加工使用权/数据产品经营权/通用(供向导按权益类型过滤场景) */
    @TableField("CEC_RIGHT_TYPE")
    private String rightType;

    @TableField("CEC_DESCRIPTION")
    private String description;

    /** 申请原因模板(选中场景时带出) */
    @TableField("CEC_REASON_TEMPLATE")
    private String reasonTemplate;

    @TableField("CEC_SCENARIO_STATUS")
    private String scenarioStatus;

    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRightType() { return rightType; }
    public void setRightType(String rightType) { this.rightType = rightType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReasonTemplate() { return reasonTemplate; }
    public void setReasonTemplate(String reasonTemplate) { this.reasonTemplate = reasonTemplate; }
    public String getScenarioStatus() { return scenarioStatus; }
    public void setScenarioStatus(String scenarioStatus) { this.scenarioStatus = scenarioStatus; }
}
