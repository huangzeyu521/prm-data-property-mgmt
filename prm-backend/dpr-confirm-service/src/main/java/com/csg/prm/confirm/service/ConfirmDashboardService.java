package com.csg.prm.confirm.service;

import com.csg.prm.confirm.dto.ConfirmDashboardVO;

/**
 * 确权看板服务(F-04 综合分析):聚合本域确权申请与权益卡片数据。
 */
public interface ConfirmDashboardService {

    /** 确权分析:支持按组织(部门)/时间周期筛选;含流程瓶颈、趋势、风险预警。 */
    ConfirmDashboardVO dashboard(String deptName, String startTime, String endTime);
}
