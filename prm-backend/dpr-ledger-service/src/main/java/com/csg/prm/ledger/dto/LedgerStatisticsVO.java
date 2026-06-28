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
    /** 按子公司分布(基于资产) */
    private Map<String, Long> bySubsidiary;
    /** 按系统部署单位分布(南网"打√"口径:总部/超高压/双调/五省网/广州/深圳,固定10桶恒显;基于资产归属派生) */
    private Map<String, Long> byDeploymentUnit;
    /** 按产权类型分布(基于档案) */
    private Map<String, Long> byRightType;
    /** 按确权状态分布(基于档案) */
    private Map<String, Long> byConfirmStatus;
    /** 按授权状态分布(基于档案) */
    private Map<String, Long> byAuthStatus;
    /** 同比/环比趋势(按月新增档案) */
    private List<TrendPoint> trend;

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

    public Map<String, Long> getBySubsidiary() {
        return bySubsidiary;
    }

    public void setBySubsidiary(Map<String, Long> bySubsidiary) {
        this.bySubsidiary = bySubsidiary;
    }

    public Map<String, Long> getByDeploymentUnit() {
        return byDeploymentUnit;
    }

    public void setByDeploymentUnit(Map<String, Long> byDeploymentUnit) {
        this.byDeploymentUnit = byDeploymentUnit;
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
