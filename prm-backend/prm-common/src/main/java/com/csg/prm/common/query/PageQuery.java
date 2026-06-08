package com.csg.prm.common.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 分页查询基类。其他查询 DTO 继承此类。
 */
public class PageQuery {

    /** 当前页,从 1 开始 */
    private long current = 1;
    /** 每页条数 */
    private long size = 10;

    public <T> IPage<T> toPage() {
        long s = this.size <= 0 ? 10 : Math.min(this.size, 500);
        long c = this.current <= 0 ? 1 : this.current;
        return new Page<>(c, s);
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
