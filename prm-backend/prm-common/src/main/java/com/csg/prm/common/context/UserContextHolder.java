package com.csg.prm.common.context;

/**
 * 基于 ThreadLocal 的用户上下文持有者。请求进入时由拦截器设置,结束时清理。
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        UserContext ctx = HOLDER.get();
        return ctx != null ? ctx : UserContext.system();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
