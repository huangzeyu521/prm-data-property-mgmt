package com.csg.prm.common.exception;

import com.csg.prm.common.api.ResponseCode;

/**
 * 自定义业务异常(对齐 data_pod / cn.csg.datapod 规范)。
 * <p>业务逻辑中主动抛出的可预见异常(数据不存在、规则冲突等),由
 * {@link GlobalExceptionHandler} 统一捕获并转换为标准 {@code Result} 响应。
 * <pre>
 *   throw new BusinessException(ResponseCode.DATA_NOT_FOUND);
 *   throw new BusinessException("仅草稿状态可删除");
 *   throw new BusinessException(404, "授权申请不存在");
 * </pre>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码 */
    private final Integer code;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.code = responseCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResponseCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResponseCode.BUSINESS_ERROR.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
