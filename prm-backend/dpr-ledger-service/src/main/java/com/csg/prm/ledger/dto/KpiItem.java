package com.csg.prm.ledger.dto;

/**
 * 单项治理 KPI:实际值 / 目标值 / 达标状态(红黄绿)/ 权重。
 */
public class KpiItem {

    public static final String STATUS_PASS = "达标";
    public static final String STATUS_WARN = "预警";
    public static final String STATUS_FAIL = "不达标";

    private String key;
    private String name;
    private double value;
    private double target;
    private String unit;
    private String status;
    private double weight;
    /** 达成度(%) = min(value/target,1)*100 */
    private double achievement;

    public KpiItem() {
    }

    public KpiItem(String key, String name, double value, double target, String unit,
                   String status, double weight, double achievement) {
        this.key = key;
        this.name = name;
        this.value = value;
        this.target = target;
        this.unit = unit;
        this.status = status;
        this.weight = weight;
        this.achievement = achievement;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getTarget() {
        return target;
    }

    public void setTarget(double target) {
        this.target = target;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getAchievement() {
        return achievement;
    }

    public void setAchievement(double achievement) {
        this.achievement = achievement;
    }
}
