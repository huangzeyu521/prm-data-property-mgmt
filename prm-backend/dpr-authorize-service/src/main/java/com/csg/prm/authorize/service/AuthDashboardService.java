package com.csg.prm.authorize.service;

import com.csg.prm.authorize.dto.AuthDashboardVO;

/**
 * 授权看板服务(F-04 综合分析):聚合本域授权申请与授权证书数据。
 */
public interface AuthDashboardService {

    /** 授权分析:支持按使用场景/组织(部门)/时间周期筛选;含合规结果、趋势、风险预警。 */
    AuthDashboardVO dashboard(String scenario, String deptName, String startTime, String endTime);
}
