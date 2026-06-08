package com.csg.prm.authorize.service;

import com.csg.prm.authorize.dto.AuthDashboardVO;

/**
 * 授权看板服务(F-04 综合分析):聚合本域授权申请与授权证书数据。
 */
public interface AuthDashboardService {

    AuthDashboardVO dashboard();
}
