package com.csg.prm.authorize.dto;

import java.util.ArrayList;
import java.util.List;

/** 授权合规校验报告(三维:材料完整性/权限合理性/合规性)。 */
public class AuthComplianceReport {

    private String checkId;
    private String applyId;
    private String riskLevel;     // 绿/黄/红
    private String checkResult;   // 通过/警告/不通过
    private String problemDesc;   // 不符合项汇总
    private final List<Item> items = new ArrayList<>();

    public void add(String dimension, String item, boolean pass, String message) {
        items.add(new Item(dimension, item, pass, message));
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getCheckResult() {
        return checkResult;
    }

    public void setCheckResult(String checkResult) {
        this.checkResult = checkResult;
    }

    public String getProblemDesc() {
        return problemDesc;
    }

    public void setProblemDesc(String problemDesc) {
        this.problemDesc = problemDesc;
    }

    public List<Item> getItems() {
        return items;
    }

    public static class Item {
        private final String dimension;
        private final String item;
        private final boolean pass;
        private final String message;

        public Item(String dimension, String item, boolean pass, String message) {
            this.dimension = dimension;
            this.item = item;
            this.pass = pass;
            this.message = message;
        }

        public String getDimension() {
            return dimension;
        }

        public String getItem() {
            return item;
        }

        public boolean isPass() {
            return pass;
        }

        public String getMessage() {
            return message;
        }
    }
}
