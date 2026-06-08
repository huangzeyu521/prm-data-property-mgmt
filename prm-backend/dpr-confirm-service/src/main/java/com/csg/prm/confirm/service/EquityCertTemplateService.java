package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.confirm.entity.EquityCertTemplate;

/**
 * 确权权益证书模板服务。已用模板禁物理删,仅停用 + 新版本。
 */
public interface EquityCertTemplateService {
    String create(EquityCertTemplate t);
    void update(EquityCertTemplate t);
    void enable(String templateId);
    void disable(String templateId);

    /** 上传模板套版文件(PDF/Word/图片,格式校验后 Base64 入库)。 */
    void uploadFile(String templateId, String fileName, byte[] data);

    /** 下载模板套版文件二进制。 */
    byte[] download(String templateId);

    EquityCertTemplate getById(String templateId);

    PageResult<EquityCertTemplate> page(long current, long size, String templateName, String templateStatus);
}
