package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.authorize.entity.AuthCert;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权权益证书服务。授权审批通过后自动生成,支持撤销、监测联动熔断暂停、到期续签。
 */
public interface AuthCertService {

    String generateFromApply(AuthApply apply);

    /** 渲染证书内容(证书+模板+合规校验)供在线预览。 */
    com.csg.prm.authorize.dto.AuthCertRenderVO render(String certId);

    AuthCert getById(String certId);

    void revoke(String certId);

    PageResult<AuthCert> page(PageRequest query);

    /**
     * 监测联动熔断:暂停某资产下所有生效证书,并自动建违规追责记录。
     * @return 被暂停的证书数量
     */
    int suspendByAsset(String assetId, String reason, String sourceAlertId, String violationType);

    /** 到期续签:延长生效证书有效期(整改后亦可对已暂停证书恢复并续期) */
    void renew(String certId, LocalDateTime newValidDate);

    /** 到期预警:列出 days 天内到期(或已过期)的生效证书 */
    List<AuthCert> findExpiring(int days);
}
