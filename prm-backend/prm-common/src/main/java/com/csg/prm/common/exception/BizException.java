package com.csg.prm.common.exception;

import com.csg.prm.common.api.ResultCode;

/**
 * 业务异常。统一由 {@link GlobalExceptionHandler} 转为标准响应。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public int getCode() {
        return code;
    }
}
