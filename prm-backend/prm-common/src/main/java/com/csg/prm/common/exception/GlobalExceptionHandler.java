package com.csg.prm.common.exception;

import com.csg.prm.common.api.R;
import com.csg.prm.common.api.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理:屏蔽技术细节,返回用户友好提示(符合界面提示规范)。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValidation(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return R.fail(ResultCode.PARAM_ERROR.getCode(), msg.isEmpty() ? ResultCode.PARAM_ERROR.getMessage() : msg);
    }

    /** 请求体格式错误(如 JSON 解析失败)属于客户端错误,返回 400 而非 999 系统异常 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return R.fail(ResultCode.PARAM_ERROR.getCode(), "请求体格式错误,请检查参数");
    }

    /** 方法不支持属客户端 405:返回真实状态码并降为 WARN(同 404 口径,避免误报系统异常) */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return R.fail(ResultCode.PARAM_ERROR.getCode(), "请求方法不支持:" + e.getMethod());
    }

    /** 路径不存在属客户端 404:返回真实 404 状态码并降为 WARN,避免误报"系统异常"与 ERROR 堆栈噪音 */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNotFound(NoResourceFoundException e) {
        log.warn("请求路径不存在: {}", e.getResourcePath());
        return R.fail(ResultCode.NOT_FOUND.getCode(), "请求路径不存在");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOthers(Exception e) {
        log.error("系统异常", e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }
}
