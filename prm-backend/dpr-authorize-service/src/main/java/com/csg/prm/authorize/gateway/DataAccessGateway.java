package com.csg.prm.authorize.gateway;

/**
 * 底层数据资源权限开通网关(授权 F-03 -> 数据资产管理平台/数据服务)。
 * 协议审核通过正式生效时,触发开通被授权方对底层数据资源的访问权限(需求§4.3.3)。
 * 生产环境由 Feign/消息调用数据服务平台 + @Primary 覆盖;本地/测试用 {@link LocalDataAccessGateway} 桩。
 */
public interface DataAccessGateway {

    /**
     * 开通数据访问权限。
     * @return 开通凭据/工单号(本地桩返回模拟号)
     */
    String grant(String applyId, String granteeOrg, String scope);

    /**
     * 回收数据访问权限(协议终止——附录D 第七章,动态跟踪要求)。
     * @return 回收工单号(本地桩返回模拟号)
     */
    String revoke(String applyId, String granteeOrg, String reason);
}
