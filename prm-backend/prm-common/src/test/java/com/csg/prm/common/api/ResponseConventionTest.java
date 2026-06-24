package com.csg.prm.common.api;

import com.csg.prm.common.exception.GlobalExceptionHandler;
import com.csg.prm.common.query.PageQuery;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 响应约定对齐南网规范的单元测试:
 * 信封 {@link R} 含 timestamp/message 规范字段;方法级参数校验失败由全局处理器转 400(而非误判 999)。
 */
class ResponseConventionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void result_envelope_carriesTimestampAndMessageAlias() {
        R<String> ok = R.ok("payload");
        assertEquals(0, ok.getCode());
        assertEquals("成功", ok.getMsg());
        // 南网规范字段名 message:与 msg 同值别名
        assertEquals(ok.getMsg(), ok.getMessage());
        // 规范信封 timestamp 字段:构造即赋值
        assertTrue(ok.getTimestamp() > 0, "信封应携带 timestamp");
        assertEquals("payload", ok.getData());
    }

    @Test
    void fail_envelope_keepsCustomCodeAndMessage() {
        R<Void> r = R.fail(400, "组织标识不能为空");
        assertEquals(400, r.getCode());
        assertEquals("组织标识不能为空", r.getMsg());
        assertEquals("组织标识不能为空", r.getMessage());
    }

    @Test
    void constraintViolation_mapsToParamError400_notSystemError() {
        // PageQuery 的 jakarta 约束:current 最小为 1
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        PageQuery q = new PageQuery();
        q.setCurrent(0);
        Set<ConstraintViolation<PageQuery>> violations = validator.validate(q);
        assertFalse(violations.isEmpty(), "current=0 应触发 @Min 校验");

        R<Void> r = handler.handleConstraintViolation(new ConstraintViolationException(violations));
        assertNotNull(r);
        assertEquals(ResultCode.PARAM_ERROR.getCode(), r.getCode(), "方法级校验失败应为 400 而非 999");
        assertTrue(r.getMsg().contains("页码"), "应回传具体校验提示");
    }
}
