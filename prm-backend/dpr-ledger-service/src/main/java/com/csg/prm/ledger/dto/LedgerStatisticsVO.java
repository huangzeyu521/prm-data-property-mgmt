package com.csg.prm.ledger.dto;

import java.util.List;
import java.util.Map;

/**
 * 产权台账统计分析(对应界面 IM-DAM-DPR-01-001-001-007)。
 * 多维:按子公司、系统部署单位(南网打√口径)、产权类型、确权/授权状态分布 + 同比/环比趋势。
 * 注:不按地理省域/地市统计——南网习惯按"系统部署单位"切分(byDeploymentUnit)。
 */
public class LedgerStatisticsVO {

    private long totalArchive;
    /** 各系统部署单位「确权覆盖率/授权率」(南网打√口径10桶恒显;率=产权进度,非资产库存) */
    private List<Coverage> coverageByDeploymentUnit;
    /** 各子公司「确权覆盖率/授权率」(率视角,非资产库存) */
    private List<Coverage> coverageBySubsidiary;
    /** 按产权类型分布(已确权资产·三权分置结构) */
    private Map<String, Long> byRightType;
    /** 按确权状态分布(基于档案) */
    private Map<String, Long> byConfirmStatus;
    /** 按授权状态分布(基于档案) */
    private Map<String, Long> byAuthStatus;
    /** 同比/环比趋势(按月新增确权登记) */
    private List<TrendPoint> trend;

    /** 维度确权/授权覆盖率:名称 + 资产总数(分母) + 已确权/已授权数 + 覆盖率%。 */
    public static class Coverage {
        private String name;
        private long total;
        private long confirmed;
        private long authorized;
        private Double confirmRate;
        private Double authRate;

        public Coverage() {
        }

        public Coverage(String name, long total, long confirmed, long authorized, Double confirmRate, Double authRate) {
            this.name = name;
            this.total = total;
            this.confirmed = confirmed;
            this.authorized = authorized;
            this.confirmRate = confirmRate;
            this.authRate = authRate;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public long getConfirmed() { return confirmed; }
        public void setConfirmed(long confirmed) { this.confirmed = confirmed; }
        public long getAuthorized() { return authorized; }
        public void setAuthorized(long authorized) { this.authorized = authorized; }
        public Double getConfirmRate() { return confirmRate; }
        public void setConfirmRate(Double confirmRate) { this.confirmRate = confirmRate; }
        public Double getAuthRate() { return authRate; }
        public void setAuthRate(Double authRate) { this.authRate = authRate; }
    }

    /** 趋势点:月份 + 新增数 + 环比% + 同比% */
    public static class TrendPoint {
        private String month;
        private long count;
        private Double momRate;
        private Double yoyRate;

        public TrendPoint() {
        }

        public TrendPoint(String month, long count, Double momRate, Double yoyRate) {
            this.month = month;
            this.count = count;
            this.momRate = momRate;
            this.yoyRate = yoyRate;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public Double getMomRate() {
            return momRate;
        }

        public void setMomRate(Double momRate) {
            this.momRate = momRate;
        }

        public Double getYoyRate() {
            return yoyRate;
        }

        public void setYoyRate(Double yoyRate) {
            this.yoyRate = yoyRate;
        }
    }

    public Map<String, Long> getByAuthStatus() {
        return byAuthStatus;
    }

    public void setByAuthStatus(Map<String, Long> byAuthStatus) {
        this.byAuthStatus = byAuthStatus;
    }

    public List<TrendPoint> getTrend() {
        return trend;
    }

    public void setTrend(List<TrendPoint> trend) {
        this.trend = trend;
    }

    public long getTotalArchive() {
        return totalArchive;
    }

    public void setTotalArchive(long totalArchive) {
        this.totalArchive = totalArchive;
    }

    public List<Coverage> getCoverageBySubsidiary() {
        return coverageBySubsidiary;
    }

    public void setCoverageBySubsidiary(List<Coverage> coverageBySubsidiary) {
        this.coverageBySubsidiary = coverageBySubsidiary;
    }

    public List<Coverage> getCoverageByDeploymentUnit() {
        return coverageByDeploymentUnit;
    }

    public void setCoverageByDeploymentUnit(List<Coverage> coverageByDeploymentUnit) {
        this.coverageByDeploymentUnit = coverageByDeploymentUnit;
    }

    public Map<String, Long> getByRightType() {
        return byRightType;
    }

    public void setByRightType(Map<String, Long> byRightType) {
        this.byRightType = byRightType;
    }

    public Map<String, Long> getByConfirmStatus() {
        return byConfirmStatus;
    }

    public void setByConfirmStatus(Map<String, Long> byConfirmStatus) {
        this.byConfirmStatus = byConfirmStatus;
    }
}
