package com.csg.prm.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 统一分页结果。
 */
public class PageResult<T> implements Serializable {

    private long total;
    private long current;
    private long size;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, long current, long size, List<T> records) {
        this.total = total;
        this.current = current;
        this.size = size;
        this.records = records;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0, 1, 10, Collections.emptyList());
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
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

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
