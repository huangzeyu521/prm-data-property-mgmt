package com.csg.prm.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 统一 API 响应包装类(对齐 data_pod / cn.csg.datapod 规范)。
 * <p>所有 REST 接口出参均以此封装,信封字段 {@code {code, message, data, timestamp}};
 * {@code code=200} 表示成功。
 *
 * @param <T> 响应数据类型
 */
// 注:泛型类不加 @Schema(name=...),否则 springdoc 会把 Result<T> 收敛成单一 schema、抹掉 data 实际类型
// (导致 PageResult 与各 DTO 不出现在文档)。仅在字段级标注,泛型按 ResultXxx 自动特化展开。
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码(200 成功) */
    @Schema(description = "状态码,200 表示成功;其余为业务码/错误码", example = "200")
    private Integer code;
    /** 响应消息 */
    @Schema(description = "响应消息", example = "操作成功")
    private String message;
    /** 响应数据 */
    @Schema(description = "响应数据(失败时为 null)")
    private T data;
    /** 服务器时间戳(毫秒) */
    @Schema(description = "服务器时间戳(毫秒)", example = "1718182400000")
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ======================== 成功 ========================

    public static <T> Result<T> success() {
        return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResponseCode.SUCCESS.getCode(), message, data);
    }

    // ======================== 失败 ========================

    public static <T> Result<T> fail(String message) {
        return new Result<>(ResponseCode.FAILURE.getCode(), message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /** 便捷:由 ResponseCode 枚举构造失败响应(PRM 迁移兼容)。 */
    public static <T> Result<T> fail(ResponseCode rc) {
        return new Result<>(rc.getCode(), rc.getMessage(), null);
    }

    // ======================== 错误(系统异常等) ========================

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    /** 便捷:由 ResponseCode 枚举构造错误响应。 */
    public static <T> Result<T> error(ResponseCode rc) {
        return new Result<>(rc.getCode(), rc.getMessage(), null);
    }

    /** 是否成功(code==200)。仅供 Java 端判断,不序列化进出参信封(保持 datapod 纯 {code,message,data,timestamp})。 */
    @JsonIgnore
    public boolean isSuccess() {
        return code != null && code == ResponseCode.SUCCESS.getCode();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
