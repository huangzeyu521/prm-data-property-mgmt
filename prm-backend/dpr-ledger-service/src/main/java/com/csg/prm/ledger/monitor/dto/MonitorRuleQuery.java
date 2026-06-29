package com.csg.prm.ledger.monitor.dto;

import com.csg.prm.common.query.PageRequest;

public class MonitorRuleQuery extends PageRequest {

    private String ruleName;
    private String ruleCategory;
    private String effectStatus;

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getRuleCategory() {
        return ruleCategory;
    }

    public void setRuleCategory(String ruleCategory) {
        this.ruleCategory = ruleCategory;
    }

    public String getEffectStatus() {
        return effectStatus;
    }

    public void setEffectStatus(String effectStatus) {
        this.effectStatus = effectStatus;
    }
}
