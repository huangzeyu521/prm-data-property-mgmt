package com.csg.prm.common.api;

import com.csg.prm.common.exception.GlobalExceptionHandler;
import com.csg.prm.common.query.PageRequest;
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
 * 响应约定对齐 data_pod / cn.csg.datapod 规范的单元测试:
 * 信封 {@link Result} 为 {@code {code, message, data, timestamp}},成功码 200;
 * 方法级参数校验失败由全局处理器统一转 400(而非误判系统异常)。
 */
class ResponseConventionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void result_envelope_successCode200AndTimestamp() {
        Result<String> ok = Result.success("payload");
        assertEquals(200, ok.getCode(), "datapod 规范成功码为 200");
        assertEquals("操作成功", ok.getMessage());
        // 规范信封 timestamp 字段:构造即赋值
        assertTrue(ok.getTimestamp() > 0, "信封应携带 timestamp");
        assertEquals("payload", ok.getData());
        assertTrue(ok.isSuccess(), "code==200 应判成功");
    }

    @Test
    void fail_envelope_keepsCustomCodeAndMessage() {
        Result<Void> r = Result.fail(400, "组织标识不能为空");
        assertEquals(400, r.getCode());
        assertEquals("组织标识不能为空", r.getMessage());
        assertFalse(r.isSuccess());
    }

    @Test
    void constraintViolation_mapsToBadRequest400_notSystemError() {
        // PageRequest 的 jakarta 约束:current 最小为 1
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        PageRequest q = new PageRequest();
        q.setPageNum(0);
        Set<ConstraintViolation<PageRequest>> violations = validator.validate(q);
        assertFalse(violations.isEmpty(), "current=0 应触发 @Min 校验");

        Result<Void> r = handler.handleConstraintViolation(new ConstraintViolationException(violations));
        assertNotNull(r);
        assertEquals(ResponseCode.BAD_REQUEST.getCode(), r.getCode(), "方法级校验失败应为 400 而非系统异常");
        assertTrue(r.getMessage().contains("页码"), "应回传具体校验提示");
    }
}
