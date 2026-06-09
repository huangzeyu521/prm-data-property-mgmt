package com.csg.prm.authorize.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.authorize.entity.AuthCertTemplate;

/**
 * 授权权益证书模板服务(可研 3.2.2.1.1.3.4.2)。已用模板禁物理删,仅停用 + 新版本。
 */
public interface AuthCertTemplateService {
    String create(AuthCertTemplate t);
    void update(AuthCertTemplate t);
    void delete(String templateId);
    void enable(String templateId);
    void disable(String templateId);

    /** 上传套版文件(PDF/Word/图片,格式校验 + Base64)。 */
    void uploadFile(String templateId, String fileName, byte[] data);

    /** 下载套版文件。 */
    byte[] download(String templateId);

    AuthCertTemplate getById(String templateId);

    PageResult<AuthCertTemplate> page(long current, long size, String templateName,
                                      String certType, String templateStatus);
}
