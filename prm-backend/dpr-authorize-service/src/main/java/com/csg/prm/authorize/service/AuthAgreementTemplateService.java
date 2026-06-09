package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthAgreementTemplate;
import com.csg.prm.common.api.PageResult;

/**
 * 授权协议模板库服务(可研 3.2.2.1.1.3.3.1)。
 */
public interface AuthAgreementTemplateService {

    String create(AuthAgreementTemplate t);

    /** 修改:版本自增。 */
    void update(AuthAgreementTemplate t);

    void delete(String templateId);

    void enable(String templateId);

    void disable(String templateId);

    /** 上传套版文件(PDF/Word,格式校验 + Base64)。 */
    void uploadFile(String templateId, String fileName, byte[] data);

    /** 下载套版文件。 */
    byte[] download(String templateId);

    AuthAgreementTemplate getById(String templateId);

    PageResult<AuthAgreementTemplate> page(long current, long size, String templateName, String authType, String purpose, String status);
}
