package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.confirm.dto.CertRenderVO;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.entity.EquityCert;

/**
 * 确权权益证书服务(F-02-001-003-003)。
 */
public interface EquityCertService {
    /** 由权益卡片签发证书 */
    String issue(String cardId, String issueUnit, String templateId, String templateName);

    /** 制卡后自动按权益类型选生效模板签发标准化证书(无匹配模板用默认);返回 certId 或 null。 */
    String autoIssueForCard(EquityCard card);

    /** 渲染证书内容(证书+卡片+模板)供在线预览。 */
    CertRenderVO render(String certId);

    EquityCert getById(String certId);
    void revoke(String certId);
    PageResult<EquityCert> page(long current, long size, String cardId);
}
