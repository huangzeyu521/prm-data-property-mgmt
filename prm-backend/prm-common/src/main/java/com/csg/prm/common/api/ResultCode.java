package com.csg.prm.common.api;

/**
 * 统一返回码。
 */
public enum ResultCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录失效"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    BIZ_ERROR(500, "业务处理失败"),
    SYSTEM_ERROR(999, "系统异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
