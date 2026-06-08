package com.csg.prm.common.api;

import java.io.Serializable;

/**
 * 统一响应信封 {code, data, msg}。code=0 表示成功。
 */
public class R<T> implements Serializable {

    private int code;
    private String msg;
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
