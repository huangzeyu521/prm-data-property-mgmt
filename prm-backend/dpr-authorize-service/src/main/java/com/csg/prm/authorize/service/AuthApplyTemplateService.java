package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthApplyTemplate;
import com.csg.prm.common.api.PageResult;

/**
 * 授权申请表单模板服务(可研 3.2.2.1.1.3.1.2)。
 */
public interface AuthApplyTemplateService {

    String create(AuthApplyTemplate t);

    /** 修改:版本自增。 */
    void update(AuthApplyTemplate t);

    void delete(String templateId);

    void enable(String templateId);

    void disable(String templateId);

    AuthApplyTemplate getById(String templateId);

    PageResult<AuthApplyTemplate> page(long current, long size, String templateName, String authType, String templateStatus);
}
