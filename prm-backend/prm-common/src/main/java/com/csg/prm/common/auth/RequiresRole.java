package com.csg.prm.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RBAC:标注端点所需角色(满足其一即可;角色 all 始终放行)。
 * 仅当 prm.auth.enabled=true 时强制;否则不拦截(兼容既有无 token 测试)。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    String[] value();
}
