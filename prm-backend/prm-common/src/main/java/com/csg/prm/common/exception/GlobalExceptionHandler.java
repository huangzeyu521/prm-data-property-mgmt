package com.csg.prm.common.exception;

import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.api.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器(对齐 data_pod / cn.csg.datapod 规范)。
 * <p>统一捕获 Controller 层异常,转换为标准 {@link Result} 响应。优先级:精确异常 → 通用异常。
 * 屏蔽技术细节,返回用户友好提示(符合界面提示规范)。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ======================== 参数校验(400) ========================

    /** @RequestBody + @Valid 校验失败 / 表单绑定失败 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), msg.isEmpty() ? ResponseCode.BAD_REQUEST.getMessage() : msg);
    }

    /** @Validated + @RequestParam/@PathVariable 上的 jakarta 约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", msg);
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), msg.isEmpty() ? ResponseCode.BAD_REQUEST.getMessage() : msg);
    }

    /** 缺少必要请求参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), "缺少必要参数: " + e.getParameterName());
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String type = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知";
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), "参数类型错误: " + e.getName() + " 应为 " + type + " 类型");
    }

    /** 请求体格式错误(如 JSON 解析失败) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResponseCode.BAD_REQUEST.getCode(), "请求体格式错误,请检查参数");
    }

    // ======================== 405 / 404 ========================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return Result.fail(HttpStatus.METHOD_NOT_ALLOWED.value(), "请求方法不支持:" + e.getMethod());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("请求路径不存在: {}", e.getResourcePath());
        return Result.fail(ResponseCode.NOT_FOUND.getCode(), "请求路径不存在");
    }

    // ======================== 业务异常 ========================

    /** 业务逻辑主动抛出的可预见异常:HTTP 200,体内 code 携带业务码。 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    // ======================== 系统异常(兜底) ========================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOthers(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResponseCode.SYSTEM_ERROR);
    }
}
