package com.csg.prm.authorize;

import com.csg.prm.authorize.entity.AuthCertTemplate;
import com.csg.prm.authorize.service.AuthCertTemplateService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 授权权益证书模板管理测试(可研 3.2.2.1.1.3.4.2):创建默认生效、更新自增版本、停用/启用、按证书类型分页。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthCertTemplateTest {

    @Autowired private AuthCertTemplateService service;

    @Test
    void create_defaults_active_v1_and_special_type() {
        AuthCertTemplate t = new AuthCertTemplate();
        t.setTemplateName("专项授权-测试证书模板");
        t.setRightType("使用权");
        String id = service.create(t);
        assertNotNull(id);

        AuthCertTemplate created = service.page(1, 50, "专项授权-测试证书模板", null, null)
                .getRecords().get(0);
        assertEquals("v1", created.getTemplateVersion());
        assertEquals(AuthCertTemplate.STATUS_ACTIVE, created.getTemplateStatus());
        assertEquals(AuthCertTemplate.TYPE_SPECIAL, created.getCertType(), "未指定证书类型默认专项");
    }

    @Test
    void update_bumps_version_and_disable_enable_toggles_status() {
        AuthCertTemplate t = new AuthCertTemplate();
        t.setTemplateName("批量授权-版本测试模板");
        t.setCertType(AuthCertTemplate.TYPE_BATCH);
        t.setRightType("经营权");
        String id = service.create(t);

        AuthCertTemplate upd = new AuthCertTemplate();
        upd.setTemplateId(id);
        upd.setTemplateName("批量授权-版本测试模板");
        upd.setTemplateContent("更新后的证书正文");
        service.update(upd);

        service.disable(id);
        AuthCertTemplate afterDisable = service.page(1, 50, "批量授权-版本测试模板", null, null)
                .getRecords().get(0);
        assertEquals("v2", afterDisable.getTemplateVersion(), "更新应自增版本");
        assertEquals(AuthCertTemplate.STATUS_DISABLED, afterDisable.getTemplateStatus());

        service.enable(id);
        AuthCertTemplate afterEnable = service.page(1, 50, "批量授权-版本测试模板", null, null)
                .getRecords().get(0);
        assertEquals(AuthCertTemplate.STATUS_ACTIVE, afterEnable.getTemplateStatus());
    }

    @Test
    void page_filters_by_cert_type() {
        // 测试环境仅建表不载种子,故自建数据后再过滤
        AuthCertTemplate s = new AuthCertTemplate();
        s.setTemplateName("过滤测试-专项证书模板");
        s.setCertType(AuthCertTemplate.TYPE_SPECIAL);
        s.setRightType("使用权");
        service.create(s);
        AuthCertTemplate b = new AuthCertTemplate();
        b.setTemplateName("过滤测试-批量证书模板");
        b.setCertType(AuthCertTemplate.TYPE_BATCH);
        b.setRightType("使用权");
        service.create(b);

        PageResult<AuthCertTemplate> batch = service.page(1, 50, null, AuthCertTemplate.TYPE_BATCH, null);
        assertTrue(batch.getTotal() >= 1, "应能查到批量授权证书模板");
        assertTrue(batch.getRecords().stream().allMatch(x -> AuthCertTemplate.TYPE_BATCH.equals(x.getCertType())),
                "按批量授权证书过滤应只返回批量类型");
    }

    @Test
    void update_missing_template_throws() {
        AuthCertTemplate t = new AuthCertTemplate();
        t.setTemplateId("NOT-EXIST");
        assertThrows(BusinessException.class, () -> service.update(t));
    }
}
