package com.csg.prm.confirm;

import com.csg.prm.confirm.controller.ConfirmMaterialController;
import com.csg.prm.common.auth.RequiresRole;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 职责分离守卫(确权侧):材料人工核验结论(check)属预审/审核动作,申报人自助走 check-run/push-review,不得自核。
 * 锁定 @RequiresRole 不含 "apply"。
 */
class ApplicantRbacGuardTest {

    @Test
    void material_check_blockApplicant() throws Exception {
        Method m = ConfirmMaterialController.class.getMethod("check", String.class, boolean.class, String.class);
        RequiresRole rr = m.getAnnotation(RequiresRole.class);
        assertNotNull(rr, "ConfirmMaterialController.check 必须标注 @RequiresRole");
        assertFalse(Arrays.asList(rr.value()).contains("apply"),
                "材料人工核验不得允许 apply 角色(职责分离)");
    }
}
