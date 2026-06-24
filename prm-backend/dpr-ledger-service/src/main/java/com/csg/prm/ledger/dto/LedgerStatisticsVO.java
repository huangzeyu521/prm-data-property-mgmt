package com.csg.prm.ledger.dto;

import java.util.List;
import java.util.Map;

/**
 * 产权台账统计分析(对应界面 IM-DAM-DPR-01-001-001-007)。
 * 多维:按子公司、省域/地市(组织主数据)、产权类型、确权/授权状态分布 + 同比/环比趋势。
 */
public class LedgerStatisticsVO {

    private long totalArchive;
    /** 按子公司分布(基于资产) */
    private Map<String, Long> bySubsidiary;
    /** 按省域分布(基于组织主数据 SYS_ORGANIZATION:province_code→省名,无码按子公司名兜底解析) */
    private Map<String, Long> byProvince;
    /** 按省→地市嵌套分布(组织主数据;前端点省下钻该省地市,真联动) */
    private Map<String, Map<String, Long>> byBureau;
    /** 各省确权覆盖率对比(基于档案:按省分组算 已确权/总数,跨维决策) */
    private List<ProvinceConfirm> provinceConfirm;
    /** 各省授权状态结构(基于档案:省→授权状态→数,堆叠对比) */
    private Map<String, Map<String, Long>> provinceAuthStatus;
    /** 按产权类型分布(基于档案) */
    private Map<String, Long> byRightType;
    /** 按确权状态分布(基于档案) */
    private Map<String, Long> byConfirmStatus;
    /** 按授权状态分布(基于档案) */
    private Map<String, Long> byAuthStatus;
    /** 同比/环比趋势(按月新增档案) */
    private List<TrendPoint> trend;

    /** 各省确权覆盖率:省 + 档案总数 + 已确权数 + 覆盖率%。 */
    public static class ProvinceConfirm {
        private String province;
        private long total;
        private long confirmed;
        private Double rate;

        public ProvinceConfirm() {
        }

        public ProvinceConfirm(String province, long total, long confirmed, Double rate) {
            this.province = province;
            this.total = total;
            this.confirmed = confirmed;
            this.rate = rate;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getConfirmed() {
            return confirmed;
        }

        public void setConfirmed(long confirmed) {
            this.confirmed = confirmed;
        }

        public Double getRate() {
            return rate;
        }

        public void setRate(Double rate) {
            this.rate = rate;
        }
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

    public Map<String, Long> getByProvince() {
        return byProvince;
    }

    public void setByProvince(Map<String, Long> byProvince) {
        this.byProvince = byProvince;
    }

    public Map<String, Map<String, Long>> getByBureau() {
        return byBureau;
    }

    public void setByBureau(Map<String, Map<String, Long>> byBureau) {
        this.byBureau = byBureau;
    }

    public List<ProvinceConfirm> getProvinceConfirm() {
        return provinceConfirm;
    }

    public void setProvinceConfirm(List<ProvinceConfirm> provinceConfirm) {
        this.provinceConfirm = provinceConfirm;
    }

    public Map<String, Map<String, Long>> getProvinceAuthStatus() {
        return provinceAuthStatus;
    }

    public void setProvinceAuthStatus(Map<String, Map<String, Long>> provinceAuthStatus) {
        this.provinceAuthStatus = provinceAuthStatus;
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
