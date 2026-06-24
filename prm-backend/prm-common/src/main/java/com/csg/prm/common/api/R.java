package com.csg.prm.common.api;

import java.io.Serializable;

/**
 * 统一响应信封 {code, msg, data, timestamp}。code=0 表示成功。
 * <p>对齐南网后端规范信封 {@code {code, message, data, timestamp}}:在既有 {@code msg}
 * 之外附加序列化 {@code message}(同值别名)与 {@code timestamp},既贴合规范字段名又不破坏
 * 前端既有契约(前端读 {@code code/msg/data})。
 */
public class R<T> implements Serializable {

    private int code;
    private String msg;
    private T data;
    /** 服务器时间戳(毫秒),南网规范信封字段 */
    private long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> fail(ResultCode rc) {
        return new R<>(rc.getCode(), rc.getMessage(), null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    /** 南网规范信封字段名为 {@code message};此处作为 {@link #msg} 的同值序列化别名。 */
    public String getMessage() {
        return msg;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
