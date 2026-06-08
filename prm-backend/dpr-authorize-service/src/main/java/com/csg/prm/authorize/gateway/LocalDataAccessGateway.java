package com.csg.prm.authorize.gateway;

import com.csg.prm.common.crypto.Sm3Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 数据权限开通网关本地桩:记录开通意图并返回确定性工单号。
 * 生产环境另以 Feign/消息实现 + @Primary 覆盖,真正开通底层数据资源权限。
 */
@Component
public class LocalDataAccessGateway implements DataAccessGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalDataAccessGateway.class);

    @Override
    public String grant(String applyId, String granteeOrg, String scope) {
        String ticket = "GRANT-" + Sm3Util.hashHex(applyId + "|" + granteeOrg).substring(0, 12).toUpperCase();
        log.info("[数据权限开通-本地桩] 协议生效 -> 开通 申请={} 被授权方={} 范围={} 工单={}",
                applyId, granteeOrg, scope, ticket);
        return ticket;
    }
}
