package com.csg.prm.common.api;

/**
 * 统一响应状态码枚举(对齐 data_pod / cn.csg.datapod 规范)。
 * <p>状态码设计:200 成功;400/401/403/404/500 HTTP 对齐;1000~ 通用业务、2000~ 用户模块。
 * <p>末段 PARAM_ERROR/BIZ_ERROR 为 PRM 既有码名的同值别名,保证迁移期调用点零改动。
 */
public enum ResponseCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    /** 操作失败(通用) */
    FAILURE(500, "操作失败"),
    /** 请求参数错误 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未认证 */
    UNAUTHORIZED(401, "未认证,请先登录"),
    /** 无权限 */
    FORBIDDEN(403, "无权限访问"),
    /** 资源不存在 */
    NOT_FOUND(404, "请求的资源不存在"),
    /** 系统内部错误 */
    SYSTEM_ERROR(500, "系统内部错误,请联系管理员"),

    // ======================== 业务异常(1000~) ========================
    /** 业务处理失败(通用) */
    BUSINESS_ERROR(1000, "业务处理失败"),
    /** 数据已存在 */
    DATA_ALREADY_EXISTS(1001, "数据已存在"),
    /** 数据不存在 */
    DATA_NOT_FOUND(1002, "数据不存在"),

    // ======================== 兼容 PRM 既有码名(同值别名) ========================
    /** = BAD_REQUEST,请求参数错误 */
    PARAM_ERROR(400, "请求参数错误"),
    /** = 业务处理失败(PRM 既有,沿用 500 数值) */
    BIZ_ERROR(500, "业务处理失败");

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
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
