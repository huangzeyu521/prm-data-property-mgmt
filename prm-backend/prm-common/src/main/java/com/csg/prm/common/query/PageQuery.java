package com.csg.prm.common.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 分页查询基类。其他查询 DTO 继承此类。
 * <p>jakarta 约束对齐南网规范(配合 @Valid 生效);{@link #toPage()} 仍保留 clamp 作纵深防御。
 */
public class PageQuery {

    /** 当前页,从 1 开始 */
    @Min(value = 1, message = "页码最小为 1")
    private long current = 1;
    /** 每页条数 */
    @Min(value = 1, message = "每页大小最小为 1")
    @Max(value = 500, message = "每页大小最大为 500")
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
